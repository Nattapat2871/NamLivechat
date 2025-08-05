package com.namLivechat.command;

import com.namLivechat.NamLivechat;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiveChatCommand implements CommandExecutor {

    private final NamLivechat plugin;
    private static final Pattern TWITCH_URL_PATTERN = Pattern.compile("twitch\\.tv/([a-zA-Z0-9_]+)");
    private static final Pattern TIKTOK_URL_PATTERN = Pattern.compile("tiktok\\.com/@([a-zA-Z0-9_.]+)");

    public LiveChatCommand(NamLivechat plugin) {
        this.plugin = plugin;
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
            player.sendMessage(ChatColor.AQUA + "--- " + desc.getName() + " v" + desc.getVersion() + " ---");
            player.sendMessage(ChatColor.WHITE + "Brings YouTube, Twitch & TikTok live chats into Minecraft!");
            player.sendMessage(ChatColor.GREEN + "Usage: /livechat <start|stop> <platform> [url/id/user]");
            player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.5f);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "start" -> handleStartCommand(player, args);
            case "stop" -> handleStopCommand(player, args);
            default -> {
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use 'start' or 'stop'.");
                player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            }
        }
        return true;
    }

    private void handleStartCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /livechat start <platform> <url/id/user>");
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            return;
        }
        String platform = args[1].toLowerCase();
        String input = args[2];

        if (input.startsWith("<") && input.endsWith(">")) {
            player.sendMessage(ChatColor.RED + "Please provide a real URL, ID, or Username, not the example text.");
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            return;
        }

        switch (platform) {
            case "youtube" -> {
                player.sendMessage(ChatColor.YELLOW + "Connecting to YouTube...");
                plugin.getYoutubeService().start(player, input);
            }
            case "twitch" -> {
                Matcher matcher = TWITCH_URL_PATTERN.matcher(input);
                String channelName = matcher.find() ? matcher.group(1) : input;
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eConnecting to Twitch channel &d" + channelName + "&e..."));
                plugin.getTwitchService().start(player, channelName);
            }
            case "tiktok" -> {
                Matcher matcher = TIKTOK_URL_PATTERN.matcher(input);
                String username = matcher.find() ? matcher.group(1) : input;
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eConnecting to TikTok user &b@" + username.replace("@", "") + "&e..."));
                plugin.getTiktokService().start(player, username);
            }
            default -> {
                player.sendMessage(ChatColor.RED + "Unknown platform. Use: youtube, twitch, tiktok");
                player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            }
        }
    }

    private void handleStopCommand(Player player, String[] args) {
        if (args.length < 2) {
            boolean stoppedAny = plugin.getYoutubeService().stop(player, true) ||
                    plugin.getTwitchService().stop(player, true) ||
                    plugin.getTiktokService().stop(player, true);

            if (stoppedAny) {
                player.sendMessage(ChatColor.GREEN + "Disconnected from all live chats.");
                player.playSound(player.getLocation(), "block.note_block.bass", 1.0f, 1.0f);
            } else {
                player.sendMessage(ChatColor.RED + "You are not connected to any live chat.");
                player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            }
            return;
        }

        String platform = args[1].toLowerCase();
        boolean stopped = false;
        switch (platform) {
            case "youtube" -> stopped = plugin.getYoutubeService().stop(player, false);
            case "twitch" -> stopped = plugin.getTwitchService().stop(player, false);
            case "tiktok" -> stopped = plugin.getTiktokService().stop(player, false);
            default -> {
                player.sendMessage(ChatColor.RED + "Unknown platform: " + platform);
                player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                return;
            }
        }

        if (stopped) {
            player.sendMessage(ChatColor.GREEN + "Disconnected from " + platform + " live chat.");
            player.playSound(player.getLocation(), "block.note_block.bass", 1.0f, 1.0f);
        } else {
            player.sendMessage(ChatColor.RED + "You are not connected to a " + platform + " live chat.");
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
        }
    }
}