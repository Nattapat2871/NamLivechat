package com.namLivechat.platform.Youtube;

import com.google.api.services.youtube.model.LiveChatMessage;
import com.google.api.services.youtube.model.LiveChatMessageListResponse;
import com.namLivechat.NamLivechat;
import com.namLivechat.service.AlertService;
import com.namLivechat.service.MessageHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class YouTubeService {

    private final NamLivechat plugin;
    private final AlertService alertService;
    private final MessageHandler messageHandler;
    private final YouTubeApiHelper apiHelper;
    private final boolean isFolia;

    private final Map<UUID, Object> playerTasks = new ConcurrentHashMap<>();
    private final Map<UUID, String> nextPageTokens = new ConcurrentHashMap<>();
    private final Map<UUID, Long> startTimeStamps = new ConcurrentHashMap<>();
    private final Map<UUID, String> connectedChannels = new ConcurrentHashMap<>();

    public YouTubeService(NamLivechat plugin) {
        this.plugin = plugin;
        this.alertService = plugin.getAlertService();
        this.messageHandler = plugin.getMessageHandler();
        this.isFolia = plugin.isFolia();
        this.apiHelper = new YouTubeApiHelper(plugin);
        initialize();
    }

    public void initialize() {
        apiHelper.initialize();
    }

    public void start(Player player, String input) {
        if (!plugin.getYoutubeConfig().getBoolean("enabled", false)) {
            messageHandler.sendMessage(player, "youtube_disabled");
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            return;
        }

        if (!apiHelper.isAvailable()) {
            messageHandler.sendMessage(player, "youtube_no_api_key");
            return;
        }
        if (isConnected(player)) {
            messageHandler.sendFormattedMessage(player, "already_connected", "%platform%", "YouTube");
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
                    runOnPlayerThread(player, () -> messageHandler.sendMessage(player, "youtube_not_live"));
                    stop(player, true);
                    return;
                }

                UUID playerUUID = player.getUniqueId();
                startTimeStamps.put(playerUUID, System.currentTimeMillis());
                connectedChannels.put(playerUUID, streamInfo.channelTitle());

                runOnPlayerThread(player, () -> {
                    messageHandler.sendFormattedMessage(player, "connect_success_youtube",
                            "%title%", streamInfo.videoTitle(),
                            "%channel%", streamInfo.channelTitle()
                    );
                    player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.0f);
                });

                fetchAndDisplayMessages(player, streamInfo.liveChatId());

            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (message.contains("api key not valid") || message.contains("api_key_invalid")) {
                    runOnPlayerThread(player, () -> messageHandler.sendMessage(player, "youtube_invalid_key"));
                    plugin.getLogger().severe("Failed to connect to YouTube: Invalid API Key. Please check youtube-config.yml.");
                } else if (message.contains("quotaexceeded")) {
                    runOnPlayerThread(player, () -> messageHandler.sendMessage(player, "youtube_quota_exceeded"));
                    plugin.getLogger().severe("Failed to connect to YouTube: API Quota Exceeded.");
                } else {
                    plugin.logDebug("YouTube connection error for " + player.getName() + ": " + e.getMessage());
                }
                stop(player, true);
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
        if (!isConnected(player)) {
            return;
        }

        try {
            String pageToken = nextPageTokens.get(playerUUID);
            LiveChatMessageListResponse response = apiHelper.getLiveChatMessages(liveChatId, pageToken);

            if (response == null) {
                plugin.logDebug("API response for getLiveChatMessages was null. Stopping poll for " + player.getName());
                stop(player, true);
                return;
            }

            nextPageTokens.put(playerUUID, response.getNextPageToken());
            long lastMessageTime = startTimeStamps.getOrDefault(playerUUID, 0L);
            long maxTimestamp = lastMessageTime;

            YouTubeMessageProcessor processor = new YouTubeMessageProcessor(plugin, player);
            if (response.getItems() != null) {
                for (LiveChatMessage item : response.getItems()) {
                    long messageTimestamp = item.getSnippet().getPublishedAt().getValue();
                    if (messageTimestamp > lastMessageTime) {
                        runOnPlayerThread(player, () -> processor.handleMessage(item));
                        if (messageTimestamp > maxTimestamp) maxTimestamp = messageTimestamp;
                    }
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

        } catch (Exception e) {
            plugin.logDebug("An exception occurred during YouTube chat polling for " + player.getName() + ": " + e.getMessage());

            String channelTitle = connectedChannels.getOrDefault(player.getUniqueId(), "YouTube");
            runOnPlayerThread(player, () -> messageHandler.sendFormattedMessage(player, "disconnect_stream_ended_youtube", "%channel%", channelTitle));
            stop(player, true);
        }
    }

    private void runOnPlayerThread(Player player, Runnable runnable) {
        if (plugin.isDisabling() || player == null || !player.isOnline()) return;
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