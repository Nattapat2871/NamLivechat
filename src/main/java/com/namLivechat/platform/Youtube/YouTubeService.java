package com.namLivechat.platform.Youtube;

import com.google.api.services.youtube.model.LiveChatMessage;
import com.google.api.services.youtube.model.LiveChatMessageListResponse;
import com.namLivechat.NamLivechat;
import com.namLivechat.service.AlertService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class YouTubeService {

    private final NamLivechat plugin;
    private final AlertService alertService;
    private final YouTubeApiHelper apiHelper;
    private final boolean isFolia;

    private final Map<UUID, Object> playerTasks = new ConcurrentHashMap<>();
    private final Map<UUID, String> nextPageTokens = new ConcurrentHashMap<>();
    private final Map<UUID, Long> startTimeStamps = new ConcurrentHashMap<>();
    private final Map<UUID, String> connectedChannels = new ConcurrentHashMap<>();

    public YouTubeService(NamLivechat plugin, AlertService alertService, boolean isFolia) {
        this.plugin = plugin;
        this.alertService = alertService;
        this.isFolia = isFolia;
        this.apiHelper = new YouTubeApiHelper(plugin);
        initialize();
    }

    public void initialize() {
        apiHelper.initialize();
        if (apiHelper.isAvailable()) {
            plugin.getLogger().info("YouTube Service has been successfully initialized.");
        } else {
            plugin.getLogger().warning("YouTube API Key is not set. YouTube feature will be disabled.");
        }
    }

    public void start(Player player, String input) {
        if (!apiHelper.isAvailable()) {
            player.sendMessage(ChatColor.RED + "YouTube feature is disabled. Please check the server console.");
            return;
        }
        if (playerTasks.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already connected to a YouTube chat.");
            return;
        }

        String videoId = apiHelper.getVideoIdFromUrl(input);
        if (videoId == null) videoId = input;

        playerTasks.put(player.getUniqueId(), new Object());

        final String finalVideoId = videoId;
        Runnable connectionTask = () -> {
            try {
                LiveStreamInfo streamInfo = apiHelper.getLiveChatId(finalVideoId);
                if (streamInfo == null) {
                    runOnPlayerThread(player, () -> player.sendMessage(ChatColor.RED + "Could not find an active live stream for this Video ID/URL."));
                    playerTasks.remove(player.getUniqueId());
                    return;
                }

                UUID playerUUID = player.getUniqueId();
                startTimeStamps.put(playerUUID, System.currentTimeMillis());
                connectedChannels.put(playerUUID, streamInfo.channelTitle());

                String connectingMessage = "&aConnecting to: &f%videoTitle% &a(Channel: &f%channelTitle%&a)".replace("%videoTitle%", streamInfo.videoTitle()).replace("%channelTitle%", streamInfo.channelTitle());
                runOnPlayerThread(player, () -> {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', connectingMessage));
                    player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.0f);
                });

                fetchAndDisplayMessages(player, streamInfo.liveChatId());

            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (message.contains("api key not valid") || message.contains("api_key_invalid")) {
                    runOnPlayerThread(player, () -> player.sendMessage(ChatColor.RED + "YouTube API Key is invalid. Please contact an admin."));
                    plugin.getLogger().severe("Failed to connect to YouTube: Invalid API Key. Please check youtube-config.yml.");
                } else if (message.contains("quotaexceeded")) {
                    runOnPlayerThread(player, () -> player.sendMessage(ChatColor.RED + "YouTube API Quota has been exceeded for today."));
                    plugin.getLogger().severe("Failed to connect to YouTube: API Quota Exceeded.");
                } else {
                    runOnPlayerThread(player, () -> player.sendMessage(ChatColor.RED + "An unexpected error occurred while connecting to YouTube."));
                    plugin.getLogger().severe("YouTube connection error for " + player.getName() + ": " + e.getMessage());
                }
                playerTasks.remove(player.getUniqueId());
            }
        };

        if (isFolia) {
            Bukkit.getAsyncScheduler().runNow(plugin, (task) -> connectionTask.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, connectionTask);
        }
    }

    public boolean stop(Player player, boolean silent) {
        UUID playerUUID = player.getUniqueId();
        if (!playerTasks.containsKey(playerUUID)) {
            return false;
        }

        alertService.stopBossBar(player);
        Object task = playerTasks.remove(playerUUID);
        if (task instanceof io.papermc.paper.threadedregions.scheduler.ScheduledTask) {
            ((io.papermc.paper.threadedregions.scheduler.ScheduledTask) task).cancel();
        } else if (task instanceof BukkitTask) {
            ((BukkitTask) task).cancel();
        }

        nextPageTokens.remove(playerUUID);
        startTimeStamps.remove(playerUUID);
        connectedChannels.remove(playerUUID);

        if (!silent) {
            // Let LiveChatCommand handle the message
        }
        return true;
    }

    public void stopAll() {
        playerTasks.keySet().forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) stop(player, true);
        });
    }

    private void fetchAndDisplayMessages(Player player, String liveChatId) {
        UUID playerUUID = player.getUniqueId();
        if (!player.isOnline() || !playerTasks.containsKey(playerUUID)) {
            return;
        }

        try {
            String pageToken = nextPageTokens.get(playerUUID);
            LiveChatMessageListResponse response = apiHelper.getLiveChatMessages(liveChatId, pageToken);
            if (response == null) return;

            nextPageTokens.put(playerUUID, response.getNextPageToken());
            long lastMessageTime = startTimeStamps.getOrDefault(playerUUID, 0L);
            long maxTimestamp = lastMessageTime;

            YouTubeMessageProcessor processor = new YouTubeMessageProcessor(plugin, alertService, player);
            for (LiveChatMessage item : response.getItems()) {
                long messageTimestamp = item.getSnippet().getPublishedAt().getValue();
                if (messageTimestamp > lastMessageTime) {
                    runOnPlayerThread(player, () -> processor.handleMessage(item));
                    if (messageTimestamp > maxTimestamp) maxTimestamp = messageTimestamp;
                }
            }
            startTimeStamps.put(playerUUID, maxTimestamp);

            long delayMillis = response.getPollingIntervalMillis();
            Runnable selfRepeatingTask = () -> fetchAndDisplayMessages(player, liveChatId);

            Object task;
            if (isFolia) {
                task = Bukkit.getAsyncScheduler().runDelayed(plugin, (t) -> selfRepeatingTask.run(), delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
            } else {
                task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, selfRepeatingTask, delayMillis / 50);
            }
            playerTasks.put(playerUUID, task);

        } catch (IOException e) {
            String channelTitle = connectedChannels.getOrDefault(player.getUniqueId(), "YouTube");
            String message = String.format("&aDisconnected from &cYouTube &f%s&a's live chat.", channelTitle);
            runOnPlayerThread(player, () -> player.sendMessage(ChatColor.translateAlternateColorCodes('&', message)));
            stop(player, true);
        }
    }

    private void runOnPlayerThread(Player player, Runnable runnable) {
        if (player == null || !player.isOnline()) return;
        if (isFolia) {
            player.getScheduler().run(plugin, (task) -> runnable.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public String getVideoIdFromUrl(String url) {
        return apiHelper.getVideoIdFromUrl(url);
    }

    public boolean isConnected(Player player) {
        return playerTasks.containsKey(player.getUniqueId());
    }

    public record LiveStreamInfo(String liveChatId, String channelTitle, String videoTitle) {}
}