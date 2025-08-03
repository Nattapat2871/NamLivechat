package com.namLivechat.command;

import com.namLivechat.NamLivechat;
import com.namLivechat.platform.Youtube.YouTubeService;
import com.google.api.services.youtube.model.LiveChatMessage;
import com.google.api.services.youtube.model.LiveChatMessageAuthorDetails;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.scheduler.BukkitTask;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LiveChatCommand implements CommandExecutor, TabCompleter {

    private final NamLivechat plugin;
    private YouTubeService youtubeService;
    private String messageFormat;
    private final Map<UUID, Map<String, Object>> playerTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, String>> nextPageTokens = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> startTimeStamps = new ConcurrentHashMap<>();

    // Boss Bar variables
    private record BossBarInfo(String title, BarColor color, BarStyle style) {}
    private final Map<UUID, Queue<BossBarInfo>> bossBarQueue = new ConcurrentHashMap<>();
    private final Map<UUID, Object> bossBarTasks = new ConcurrentHashMap<>();


    private boolean isFolia = false;
    private static final Object TASK_IN_PROGRESS = new Object();

    public LiveChatCommand(NamLivechat plugin) {
        this.plugin = plugin;
        this.isFolia = isFoliaServer();
    }

    public void initializeServices(String apiKey) {
        this.messageFormat = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("message-format", "&c[YouTube] &f%player%: &e%message%"));
        if (apiKey == null) {
            this.youtubeService = null;
            return;
        }
        try {
            this.youtubeService = new YouTubeService(apiKey);
            plugin.getLogger().info("YouTube Service has been successfully initialized.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize YouTube service! Check your API Key or internet connection.");
            this.youtubeService = null;
        }
    }

    private boolean isFoliaServer() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            PluginDescriptionFile desc = plugin.getDescription();
            sender.sendMessage(ChatColor.AQUA + "--- " + desc.getName() + " v" + desc.getVersion() + " ---");
            sender.sendMessage(ChatColor.WHITE + "Feature: " + ChatColor.YELLOW + "Display YouTube Live Chat in-game.");
            sender.sendMessage(ChatColor.WHITE + "Author: " + ChatColor.YELLOW + desc.getAuthors().get(0));
            sender.sendMessage(ChatColor.GREEN + "Usage: /" + label + " <start|stop> <platform> [id/url]");
            return true;
        }

        UUID playerUUID = player.getUniqueId();
        playerTasks.putIfAbsent(playerUUID, new ConcurrentHashMap<>());
        nextPageTokens.putIfAbsent(playerUUID, new ConcurrentHashMap<>());
        startTimeStamps.putIfAbsent(playerUUID, new ConcurrentHashMap<>());
        bossBarQueue.putIfAbsent(playerUUID, new LinkedList<>());

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start":
                handleStartCommand(player, args);
                break;
            case "stop":
                handleStopCommand(player, args);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use 'start' or 'stop'.");
                player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                break;
        }
        return true;
    }

    private void handleStartCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /livechat start <platform> <id/url>");
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            return;
        }

        String platform = args[1].toLowerCase();

        if (platform.equals("tiktok") || platform.equals("twitch")) {
            player.sendMessage(ChatColor.GOLD + "This platform is not yet available. Please follow development at: https://github.com/Nattapat2871");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /livechat start <platform> <id/url>");
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            return;
        }

        UUID playerUUID = player.getUniqueId();
        if (playerTasks.get(playerUUID).containsKey(platform)) {
            player.sendMessage(ChatColor.RED + "You are already connected to a " + platform + " live chat.");
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            return;
        }

        switch (platform) {
            case "youtube":
                if (youtubeService == null) {
                    player.sendMessage(ChatColor.RED + "YouTube feature is currently disabled. Please contact an admin.");
                    player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                    return;
                }
                startLiveChat(player, args[2], platform);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown platform. Available platforms: youtube, tiktok, twitch");
                player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                break;
        }
    }

    private void handleStopCommand(Player player, String[] args) {
        UUID playerUUID = player.getUniqueId();
        Map<String, Object> currentTasks = playerTasks.get(playerUUID);

        if (currentTasks == null || currentTasks.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You are not connected to any live chat.");
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            return;
        }

        if (args.length < 2) {
            stopAllTasksForPlayer(player);
            player.sendMessage(ChatColor.GREEN + "Disconnected from all live chats.");
            player.playSound(player.getLocation(), "block.note_block.bass", 1.0f, 1.0f);
            return;
        }

        String platform = args[1].toLowerCase();
        if (!currentTasks.containsKey(platform)) {
            player.sendMessage(ChatColor.RED + "You are not connected to a " + platform + " live chat.");
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            return;
        }

        stopTaskForPlayerPlatform(player, platform);
        player.sendMessage(ChatColor.GREEN + "Disconnected from " + platform + " live chat.");
        player.playSound(player.getLocation(), "block.note_block.bass", 1.0f, 1.0f);
    }

    private void startLiveChat(Player player, String input, String platform) {
        String videoId = youtubeService.getVideoIdFromUrl(input);
        if (videoId == null) {
            videoId = input;
        }
        player.sendMessage(ChatColor.YELLOW + "Connecting to YouTube...");

        playerTasks.get(player.getUniqueId()).put(platform, TASK_IN_PROGRESS);

        final String finalVideoId = videoId;
        Runnable connectionTask = () -> {
            try {
                YouTubeService.LiveStreamInfo streamInfo = youtubeService.getLiveChatId(finalVideoId);
                if (streamInfo == null) {
                    runOnPlayerThread(player, () -> {
                        player.sendMessage(ChatColor.RED + "Could not find an active live stream for this Video ID/URL.");
                        player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                    });
                    stopTaskForPlayerPlatform(player, platform);
                    return;
                }
                UUID playerUUID = player.getUniqueId();
                startTimeStamps.get(playerUUID).put(platform, System.currentTimeMillis());

                String liveChatId = streamInfo.getLiveChatId();
                String channelTitle = streamInfo.getChannelTitle();
                String videoTitle = streamInfo.getVideoTitle();
                String connectingMessage = ChatColor.AQUA + "[NamLivechat] " + ChatColor.GREEN + "Connecting to: " + ChatColor.WHITE + videoTitle + ChatColor.GREEN + " (Channel: " + ChatColor.WHITE + channelTitle + ChatColor.GREEN + ")";

                runOnPlayerThread(player, () -> {
                    player.sendMessage(connectingMessage);
                    player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.0f);
                });
                fetchAndDisplayMessages(playerUUID, liveChatId, platform);
            } catch (Exception e) {
                // **NEW:** Enhanced error handling for connection phase
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("quotaExceeded")) {
                    runOnPlayerThread(player, () -> {
                        player.sendMessage(ChatColor.RED + "Could not connect at this time. The API quota may be full. Please wait or contact an admin.");
                        player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                    });
                } else {
                    runOnPlayerThread(player, () -> {
                        player.sendMessage(ChatColor.RED + "An error occurred while connecting to YouTube.");
                        player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                    });
                }
                e.printStackTrace();
                stopTaskForPlayerPlatform(player, platform);
            }
        };
        if (isFolia) {
            Bukkit.getAsyncScheduler().runNow(plugin, (task) -> connectionTask.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, connectionTask);
        }
    }

    private void fetchAndDisplayMessages(UUID playerUUID, String liveChatId, String platform) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline() || !playerTasks.getOrDefault(playerUUID, Collections.emptyMap()).containsKey(platform)) {
            return;
        }

        try {
            String pageToken = nextPageTokens.get(playerUUID).get(platform);
            var response = youtubeService.getLiveChatMessages(liveChatId, pageToken);
            if (response == null) return;

            nextPageTokens.get(playerUUID).put(platform, response.getNextPageToken());
            long lastMessageTime = startTimeStamps.get(playerUUID).getOrDefault(platform, 0L);
            long maxTimestamp = lastMessageTime;

            for (LiveChatMessage item : response.getItems()) {
                long messageTimestamp = item.getSnippet().getPublishedAt().getValue();
                if (messageTimestamp > lastMessageTime) {
                    String messageType = item.getSnippet().getType();
                    LiveChatMessageAuthorDetails authorDetails = item.getAuthorDetails();
                    String authorName = authorDetails.getDisplayName();
                    switch (messageType) {
                        case "textMessageEvent":
                            handleTextMessage(player, authorDetails, item.getSnippet().getDisplayMessage());
                            break;
                        case "superChatEvent":
                            if (plugin.getConfig().getBoolean("youtube-alerts.show-super-chat", true)) {
                                handleSuperChatMessage(player, authorName, item.getSnippet().getSuperChatDetails());
                            }
                            break;
                        case "newSponsorEvent":
                            if (plugin.getConfig().getBoolean("youtube-alerts.show-new-members", true)) {
                                handleNewMemberMessage(player, authorName);
                            }
                            break;
                    }
                    if(messageTimestamp > maxTimestamp) maxTimestamp = messageTimestamp;
                }
            }
            startTimeStamps.get(playerUUID).put(platform, maxTimestamp);

            long delayMillis = response.getPollingIntervalMillis();
            Runnable selfRepeatingTask = () -> fetchAndDisplayMessages(playerUUID, liveChatId, platform);

            if (isFolia) {
                Object task = Bukkit.getAsyncScheduler().runDelayed(plugin, (t) -> selfRepeatingTask.run(), delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
                playerTasks.get(playerUUID).put(platform, task);
            } else {
                BukkitTask task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, selfRepeatingTask, delayMillis / 50);
                playerTasks.get(playerUUID).put(platform, task);
            }
        } catch (IOException e) {
            final String finalReason;
            final String googleError = e.getMessage();

            if (googleError != null) {
                plugin.getLogger().severe("An error occurred while fetching chat messages: " + googleError);
                if (googleError.contains("liveChatEnded")) {
                    finalReason = "The stream has ended.";
                } else if (googleError.contains("quotaExceeded")) {
                    finalReason = "YouTube API Quota has been exceeded.";
                } else {
                    finalReason = "An API error occurred.";
                    runOnPlayerThread(player, () -> player.sendMessage(ChatColor.RED + "Details: " + googleError));
                }
            } else {
                finalReason = "Connection lost.";
            }

            e.printStackTrace();
            runOnPlayerThread(player, () -> player.sendMessage(ChatColor.YELLOW + "[NamLivechat] Disconnected from " + platform + ". (" + finalReason + ")"));
            stopTaskForPlayerPlatform(player, platform);
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

    private void handleTextMessage(Player player, LiveChatMessageAuthorDetails authorDetails, String message) {
        String cleanMessage = message.replaceAll(":[a-zA-Z0-9_\\-]+:", "");
        String authorColor = getAuthorColor(authorDetails);
        String coloredAuthor = ChatColor.translateAlternateColorCodes('&', authorColor + authorDetails.getDisplayName());
        String formattedMessage = messageFormat
                .replace("%player%", coloredAuthor)
                .replace("%message%", cleanMessage);
        runOnPlayerThread(player, () -> player.sendMessage(formattedMessage));
    }

    private void handleSuperChatMessage(Player player, String authorName, com.google.api.services.youtube.model.LiveChatSuperChatDetails details) {
        String format = plugin.getConfig().getString("super-chat-format", "&6[Super Chat] &e%player% &fhas donated &a%amount%&f: &d%message%");
        String message = format
                .replace("%player%", authorName)
                .replace("%amount%", details.getAmountDisplayString())
                .replace("%message%", details.getUserComment() != null ? details.getUserComment() : "");

        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);

        runOnPlayerThread(player, () -> {
            player.sendMessage(coloredMessage);
            playSoundFromConfig(player, "sounds.super-chat");

            if (plugin.getConfig().getBoolean("bossbar-alerts.enabled", false)) {
                BarColor color = BarColor.valueOf(plugin.getConfig().getString("bossbar-alerts.super-chat.color", "YELLOW").toUpperCase());
                BarStyle style = BarStyle.valueOf(plugin.getConfig().getString("bossbar-alerts.super-chat.style", "SOLID").toUpperCase());
                queueBossBar(player, new BossBarInfo(coloredMessage, color, style));
            }
        });
    }

    private void handleNewMemberMessage(Player player, String authorName) {
        String format = plugin.getConfig().getString("new-member-format", "&b[New Member] &d%player% &ahas just subscribed!");
        String message = format.replace("%player%", authorName);
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);

        runOnPlayerThread(player, () -> {
            player.sendMessage(coloredMessage);
            playSoundFromConfig(player, "sounds.new-member");

            if (plugin.getConfig().getBoolean("bossbar-alerts.enabled", false)) {
                BarColor color = BarColor.valueOf(plugin.getConfig().getString("bossbar-alerts.new-member.color", "PINK").toUpperCase());
                BarStyle style = BarStyle.valueOf(plugin.getConfig().getString("bossbar-alerts.new-member.style", "SOLID").toUpperCase());
                queueBossBar(player, new BossBarInfo(coloredMessage, color, style));
            }
        });
    }

    private void queueBossBar(Player player, BossBarInfo info) {
        UUID playerUUID = player.getUniqueId();
        bossBarQueue.get(playerUUID).add(info);
        if (!bossBarTasks.containsKey(playerUUID)) {
            processBossBarQueue(player);
        }
    }

    private void processBossBarQueue(Player player) {
        if (player == null) return;
        UUID playerUUID = player.getUniqueId();

        if (bossBarTasks.containsKey(playerUUID)) return;
        Queue<BossBarInfo> queue = bossBarQueue.get(playerUUID);
        if (queue == null || queue.isEmpty()) return;

        BossBarInfo info = queue.poll();
        int durationTicks = plugin.getConfig().getInt("bossbar-alerts.duration-seconds", 10) * 20;

        BossBar bossBar = Bukkit.createBossBar(info.title(), info.color(), info.style());
        bossBar.setProgress(1.0);
        bossBar.addPlayer(player);

        Runnable taskRunnable = new Runnable() {
            private int ticksRemaining = durationTicks;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    bossBar.removeAll();
                    bossBarTasks.remove(playerUUID);
                    return;
                }

                ticksRemaining--;
                double progress = Math.max(0, (double) ticksRemaining / durationTicks);
                bossBar.setProgress(progress);

                if (ticksRemaining <= 0) {
                    bossBar.removeAll();
                    bossBarTasks.remove(playerUUID);
                    processBossBarQueue(player);
                }
            }
        };

        if (isFolia) {
            Object task = player.getScheduler().runAtFixedRate(plugin, t -> {
                taskRunnable.run();
                if(!bossBarTasks.containsKey(playerUUID)) {
                    t.cancel();
                }
            }, null, 1L, 1L);
            bossBarTasks.put(playerUUID, task);
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, taskRunnable, 0L, 1L);
            bossBarTasks.put(playerUUID, task);
        }
    }

    private void playSoundFromConfig(Player player, String configPath) {
        ConfigurationSection soundSection = plugin.getConfig().getConfigurationSection(configPath);
        if (soundSection == null) return;
        String soundName = soundSection.getString("name", "");
        if (soundName.isEmpty()) {
            plugin.getLogger().warning("Sound name is empty in config.yml at path: " + configPath);
            return;
        }
        float volume = (float) soundSection.getDouble("volume", 1.0);
        float pitch = (float) soundSection.getDouble("pitch", 1.0);
        runOnPlayerThread(player, () -> player.playSound(player.getLocation(), soundName.toLowerCase(), volume, pitch));
    }

    private String getAuthorColor(LiveChatMessageAuthorDetails authorDetails) {
        if (authorDetails.getIsChatOwner() != null && authorDetails.getIsChatOwner()) {
            return plugin.getConfig().getString("role-colors.owner", "&6");
        } else if (authorDetails.getIsChatModerator() != null && authorDetails.getIsChatModerator()) {
            return plugin.getConfig().getString("role-colors.moderator", "&9");
        } else if (authorDetails.getIsChatSponsor() != null && authorDetails.getIsChatSponsor()) {
            return plugin.getConfig().getString("role-colors.member", "&a");
        }
        return plugin.getConfig().getString("role-colors.default", "&f");
    }

    public void stopTaskForPlayerPlatform(Player player, String platform) {
        if (player == null) return;
        UUID playerUUID = player.getUniqueId();
        Map<String, Object> tasks = playerTasks.get(playerUUID);
        if (tasks == null) return;

        Object task = tasks.remove(platform);
        if (task == null || task == TASK_IN_PROGRESS) return;

        try {
            if (task instanceof io.papermc.paper.threadedregions.scheduler.ScheduledTask) {
                ((io.papermc.paper.threadedregions.scheduler.ScheduledTask) task).cancel();
            } else if (task instanceof BukkitTask) {
                ((BukkitTask) task).cancel();
            }
        } catch (Exception e) {}

        if (nextPageTokens.containsKey(playerUUID)) {
            nextPageTokens.get(playerUUID).remove(platform);
        }
        if (startTimeStamps.containsKey(playerUUID)) {
            startTimeStamps.get(playerUUID).remove(platform);
        }
    }

    public void stopAllTasksForPlayer(Player player) {
        if (player == null) return;
        UUID playerUUID = player.getUniqueId();
        Map<String, Object> tasks = playerTasks.get(playerUUID);
        if (tasks != null) {
            new ArrayList<>(tasks.keySet()).forEach(platform -> stopTaskForPlayerPlatform(player, platform));
        }

        bossBarQueue.getOrDefault(playerUUID, new LinkedList<>()).clear();
        Object bossBarTask = bossBarTasks.remove(playerUUID);
        if (bossBarTask != null) {
            try {
                if (bossBarTask instanceof io.papermc.paper.threadedregions.scheduler.ScheduledTask) {
                    ((io.papermc.paper.threadedregions.scheduler.ScheduledTask) bossBarTask).cancel();
                } else if (bossBarTask instanceof BukkitTask) {
                    ((BukkitTask) bossBarTask).cancel();
                }
            } catch (Exception e) {}
        }
        Bukkit.getBossBars().forEachRemaining(bossBar -> bossBar.removePlayer(player));
    }

    public void stopAllTasks() {
        new ArrayList<>(playerTasks.keySet()).forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if(p != null) stopAllTasksForPlayer(p);
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("start", "stop").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            if ("start".equalsIgnoreCase(args[0]) || "stop".equalsIgnoreCase(args[0])) {
                List<String> platforms = Arrays.asList("youtube", "tiktok", "twitch");
                return platforms.stream()
                        .filter(p -> p.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        if (args.length == 3 && "start".equalsIgnoreCase(args[0]) && "youtube".equalsIgnoreCase(args[1])) {
            return Collections.singletonList("<url>");
        }
        return Collections.emptyList();
    }
}