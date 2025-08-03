package com.namLivechat.command;

import com.namLivechat.NamLivechat;
import com.namLivechat.platform.Youtube.YouTubeService;
import com.google.api.services.youtube.model.LiveChatMessage;
import com.google.api.services.youtube.model.LiveChatMessageAuthorDetails;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.scheduler.BukkitTask;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LiveChatCommand implements CommandExecutor, TabCompleter {

    private final NamLivechat plugin;
    private YouTubeService youtubeService;
    private String messageFormat;
    private final Map<UUID, Object> playerTasks = new ConcurrentHashMap<>();
    private final Map<UUID, String> nextPageTokens = new ConcurrentHashMap<>();
    private final Map<UUID, Long> startTimeStamps = new ConcurrentHashMap<>();
    private boolean isFolia = false;

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
        if (args.length == 0) {
            PluginDescriptionFile desc = plugin.getDescription();
            sender.sendMessage(ChatColor.AQUA + "--- " + desc.getName() + " v" + desc.getVersion() + " ---");
            sender.sendMessage(ChatColor.WHITE + "Feature: " + ChatColor.YELLOW + "Display YouTube Live Chat in-game.");
            sender.sendMessage(ChatColor.WHITE + "Author: " + ChatColor.YELLOW + desc.getAuthors().get(0));
            sender.sendMessage(ChatColor.GREEN + "Usage: /" + label + " <start|stop> <platform> <id/url>");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }
        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();

        if ("start".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /livechat start <platform> <id/url>");
                return true;
            }

            String platform = args[1].toLowerCase();

            if (playerTasks.containsKey(playerUUID)) {
                player.sendMessage(ChatColor.RED + "You are already connected to a live chat.");
                return true;
            }

            switch (platform) {
                case "youtube":
                    if (youtubeService == null) {
                        sender.sendMessage(ChatColor.RED + "YouTube feature is currently disabled. Please contact an admin.");
                        return true;
                    }
                    if (args.length < 3) {
                        player.sendMessage(ChatColor.RED + "Usage: /livechat start youtube <url_or_id>");
                        return true;
                    }
                    startLiveChat(player, args[2]); // <-- เมธอดนี้เคยหายไป
                    break;
                case "tiktok":
                case "twitch":
                    player.sendMessage(ChatColor.GOLD + "This platform is under development. Please wait for future updates.");
                    player.sendMessage(ChatColor.GOLD + "Follow development at: https://github.com/Nattapat2871");
                    break;
                default:
                    player.sendMessage(ChatColor.RED + "Unknown platform. Available platforms: youtube, tiktok, twitch");
                    break;
            }

        } else if ("stop".equalsIgnoreCase(args[0])) {
            if (!playerTasks.containsKey(playerUUID)) {
                player.sendMessage(ChatColor.RED + "You are not connected to any live chat.");
                return true;
            }
            stopTaskForPlayer(player); // <-- เมธอดนี้เคยหายไป
            player.sendMessage(ChatColor.GREEN + "Disconnected from YouTube live chat.");
            player.playSound(player.getLocation(), "block.note_block.bass", 1.0f, 1.0f);
        } else {
            player.sendMessage(ChatColor.RED + "Unknown subcommand. Use '" + args[0] + "'.");
        }
        return true;
    }

    // ## เพิ่มเติม: เพิ่มเมธอดที่ขาดหายไปกลับเข้ามาทั้งหมด ##

    private void startLiveChat(Player player, String input) {
        String videoId = youtubeService.getVideoIdFromUrl(input);
        if (videoId == null) {
            videoId = input;
        }
        player.sendMessage(ChatColor.YELLOW + "Connecting to YouTube...");
        final String finalVideoId = videoId;
        Runnable connectionTask = () -> {
            try {
                YouTubeService.LiveStreamInfo streamInfo = youtubeService.getLiveChatId(finalVideoId);
                if (streamInfo == null) {
                    runOnPlayerThread(player, () -> {
                        player.sendMessage(ChatColor.RED + "Could not find an active live stream for this Video ID/URL.");
                        player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                    });
                    return;
                }
                UUID playerUUID = player.getUniqueId();
                startTimeStamps.put(playerUUID, System.currentTimeMillis());
                String liveChatId = streamInfo.getLiveChatId();
                String channelTitle = streamInfo.getChannelTitle();
                String videoTitle = streamInfo.getVideoTitle();
                String connectingMessage = ChatColor.AQUA + "[NamLivechat] " + ChatColor.GREEN + "Connecting to: " + ChatColor.WHITE + videoTitle + ChatColor.GREEN + " (Channel: " + ChatColor.WHITE + channelTitle + ChatColor.GREEN + ")";
                runOnPlayerThread(player, () -> {
                    player.sendMessage(connectingMessage);
                    player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.0f);
                });
                fetchAndDisplayMessages(playerUUID, liveChatId);
            } catch (Exception e) {
                runOnPlayerThread(player, () -> {
                    player.sendMessage(ChatColor.RED + "An error occurred while connecting to YouTube.");
                    player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                });
                e.printStackTrace();
            }
        };
        if (isFolia) {
            Bukkit.getAsyncScheduler().runNow(plugin, (task) -> connectionTask.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, connectionTask);
        }
    }

    private void fetchAndDisplayMessages(UUID playerUUID, String liveChatId) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) {
            stopTaskForPlayer(player);
            return;
        }
        try {
            String pageToken = nextPageTokens.get(playerUUID);
            var response = youtubeService.getLiveChatMessages(liveChatId, pageToken);
            if (response == null) return;
            nextPageTokens.put(playerUUID, response.getNextPageToken());
            long lastMessageTime = startTimeStamps.getOrDefault(playerUUID, 0L);
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
            startTimeStamps.put(playerUUID, maxTimestamp);
            long delayMillis = response.getPollingIntervalMillis();
            Runnable selfRepeatingTask = () -> fetchAndDisplayMessages(playerUUID, liveChatId);
            if (isFolia) {
                Object task = Bukkit.getAsyncScheduler().runDelayed(plugin, (t) -> selfRepeatingTask.run(), delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
                playerTasks.put(playerUUID, task);
            } else {
                BukkitTask task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, selfRepeatingTask, delayMillis / 50);
                playerTasks.put(playerUUID, task);
            }
        } catch (IOException e) {
            String reason = e.getMessage().contains("liveChatEnded") ? "The stream has ended." : "Connection lost.";
            runOnPlayerThread(player, () -> player.sendMessage(ChatColor.YELLOW + "[NamLivechat] Disconnected. (" + reason + ")"));
            stopTaskForPlayer(player);
        }
    }

    private void runOnPlayerThread(Player player, Runnable runnable) {
        if (player == null) return;
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
        runOnPlayerThread(player, () -> {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            playSoundFromConfig(player, "sounds.super-chat");
        });
    }

    private void handleNewMemberMessage(Player player, String authorName) {
        String format = plugin.getConfig().getString("new-member-format", "&b[New Member] &d%player% &ahas just subscribed!");
        String message = format.replace("%player%", authorName);
        runOnPlayerThread(player, () -> {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            playSoundFromConfig(player, "sounds.new-member");
        });
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

    public void stopTaskForPlayer(Player player) {
        if (player == null) return;
        UUID playerUUID = player.getUniqueId();
        Object task = playerTasks.remove(playerUUID);
        if (task == null) return;
        try {
            if (task instanceof io.papermc.paper.threadedregions.scheduler.ScheduledTask) {
                ((io.papermc.paper.threadedregions.scheduler.ScheduledTask) task).cancel();
            } else if (task instanceof BukkitTask) {
                ((BukkitTask) task).cancel();
            }
        } catch (Exception e) {}
        nextPageTokens.remove(playerUUID);
        startTimeStamps.remove(playerUUID);
    }

    public void stopAllTasks() {
        new java.util.ArrayList<>(playerTasks.keySet()).forEach(uuid -> stopTaskForPlayer(Bukkit.getPlayer(uuid)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("start", "stop");
        }
        if (args.length == 2 && "start".equalsIgnoreCase(args[0])) {
            List<String> platforms = Arrays.asList("youtube", "tiktok", "twitch");
            return platforms.stream()
                    .filter(p -> p.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && "start".equalsIgnoreCase(args[0]) && "youtube".equalsIgnoreCase(args[1])) {
            return Collections.singletonList("<url>");
        }
        return Collections.emptyList();
    }
}