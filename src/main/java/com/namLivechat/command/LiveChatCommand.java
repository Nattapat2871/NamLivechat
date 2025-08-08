package com.namLivechat.command;

import com.namLivechat.NamLivechat;
import com.namLivechat.service.MessageHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiveChatCommand implements CommandExecutor {

    private final NamLivechat plugin;
    private final MessageHandler messageHandler;
    private static final Pattern TWITCH_URL_PATTERN = Pattern.compile("twitch\\.tv/([a-zA-Z0-9_]+)");
    private static final Pattern TIKTOK_URL_PATTERN = Pattern.compile("tiktok\\.com/@([a-zA-Z0-9_.]+)");
    private static final List<String> PLATFORMS = Arrays.asList("youtube", "twitch", "tiktok");

    public LiveChatCommand(NamLivechat plugin) {
        this.plugin = plugin;
        this.messageHandler = plugin.getMessageHandler();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            messageHandler.sendMessage(sender, "player_only");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            messageHandler.sendFormattedMessage(player, "help_header", "%version%", plugin.getDescription().getVersion());
            messageHandler.sendMessage(player, "help_description");
            messageHandler.sendMessage(player, "help_usage");
            player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.5f);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "start" -> handleStartCommand(player, args);
            case "stop" -> handleStopCommand(player, args);
            default -> {
                messageHandler.sendMessage(player, "unknown_subcommand");
                player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            }
        }
        return true;
    }

    private void handleStartCommand(Player player, String[] args) {
        if (args.length < 2) {
            messageHandler.sendMessage(player, "start_usage");
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            return;
        }

        String firstArg = args[1].toLowerCase();

        // กรณีที่ 1: /livechat start <url> (มี 2 arguments และ argument ที่ 2 ไม่ใช่ชื่อ platform)
        if (args.length == 2 && !PLATFORMS.contains(firstArg)) {
            detectAndStart(player, args[1]); // args[1] คือ URL
            return;
        }

        // กรณีที่ 2: /livechat start <platform> <url> (มี 3 arguments และ argument ที่ 2 คือชื่อ platform)
        if (args.length >= 3 && PLATFORMS.contains(firstArg)) {
            startPlatform(player, firstArg, args[2]); // firstArg คือ platform, args[2] คือ URL
            return;
        }

        // กรณีอื่นๆ ที่ใส่คำสั่งผิด
        messageHandler.sendMessage(player, "start_usage");
        player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
    }

    private void detectAndStart(Player player, String url) {
        if (url.startsWith("<") && url.endsWith(">")) {
            messageHandler.sendMessage(player, "start_placeholder_error");
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            return;
        }

        if (url.contains("youtube.com") || url.contains("youtu.be")) {
            startPlatform(player, "youtube", url);
        } else if (url.contains("twitch.tv")) {
            startPlatform(player, "twitch", url);
        } else if (url.contains("tiktok.com")) {
            startPlatform(player, "tiktok", url);
        } else {
            messageHandler.sendMessage(player, "unknown_platform");
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
        }
    }

    private void startPlatform(Player player, String platform, String input) {
        if (input.startsWith("<") && input.endsWith(">")) {
            messageHandler.sendMessage(player, "start_placeholder_error");
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            return;
        }

        switch (platform) {
            case "youtube" -> {
                messageHandler.sendFormattedMessage(player, "connecting", "%platform%", "YouTube");
                plugin.getYoutubeService().start(player, input);
            }
            case "twitch" -> {
                Matcher matcher = TWITCH_URL_PATTERN.matcher(input);
                String channelName = matcher.find() ? matcher.group(1) : input;
                messageHandler.sendFormattedMessage(player, "connecting_channel", "%platform%", "Twitch", "%channel%", channelName);
                plugin.getTwitchService().start(player, channelName);
            }
            case "tiktok" -> {
                Matcher matcher = TIKTOK_URL_PATTERN.matcher(input);
                String username = matcher.find() ? matcher.group(1) : input;
                messageHandler.sendFormattedMessage(player, "connecting_user", "%platform%", "TikTok", "%user%", username.replace("@", ""));
                plugin.getTiktokService().start(player, username);
            }
            default -> {
                messageHandler.sendMessage(player, "unknown_platform");
                player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            }
        }
    }

    private void handleStopCommand(Player player, String[] args) {
        if (args.length < 2) {
            boolean stoppedAny = (plugin.getYoutubeService() != null && plugin.getYoutubeService().stop(player, true)) ||
                    (plugin.getTwitchService() != null && plugin.getTwitchService().stop(player, true)) ||
                    (plugin.getTiktokService() != null && plugin.getTiktokService().stop(player, true));

            if (stoppedAny) {
                messageHandler.sendMessage(player, "stop_all_success");
                player.playSound(player.getLocation(), "block.note_block.bass", 1.0f, 1.0f);
            } else {
                messageHandler.sendMessage(player, "not_connected");
                player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            }
            return;
        }

        String platform = args[1].toLowerCase();
        boolean stopped = false;
        switch (platform) {
            case "youtube" -> stopped = plugin.getYoutubeService() != null && plugin.getYoutubeService().stop(player, false);
            case "twitch" -> stopped = plugin.getTwitchService() != null && plugin.getTwitchService().stop(player, false);
            case "tiktok" -> stopped = plugin.getTiktokService() != null && plugin.getTiktokService().stop(player, false);
            default -> {
                messageHandler.sendMessage(player, "unknown_platform");
                player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                return;
            }
        }

        if (stopped) {
            messageHandler.sendFormattedMessage(player, "stop_success", "%platform%", platform);
            player.playSound(player.getLocation(), "block.note_block.bass", 1.0f, 1.0f);
        } else {
            messageHandler.sendFormattedMessage(player, "not_connected_platform", "%platform%", platform);
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
        }
    }
}