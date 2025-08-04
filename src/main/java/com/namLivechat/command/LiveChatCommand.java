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
    private static final Pattern TWITCH_URL_PATTERN = Pattern.compile("^(?:https?:\\/\\/)?(?:www\\.)?twitch\\.tv\\/([a-zA-Z0-9_]+)");

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
            // --- ส่วนที่แก้ไข: เปลี่ยนเป็นข้อความภาษาอังกฤษ ---
            player.sendMessage(ChatColor.AQUA + "--- " + desc.getName() + " v" + desc.getVersion() + " ---");
            player.sendMessage(ChatColor.WHITE + "Brings YouTube & Twitch live chats into Minecraft!");
            player.sendMessage(ChatColor.GRAY + "Features: Real-time chat, Boss Bar alerts, fully configurable.");
            player.sendMessage(ChatColor.GREEN + "Usage: /livechat <start|stop> <platform> [url/id]");
            // ----------------------------------------------------

            player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.5f);
            return true;
        }

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
            player.sendMessage(ChatColor.RED + "Usage: /livechat start <platform> <url/id>");
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            return;
        }

        String platform = args[1].toLowerCase();

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /livechat start " + platform + " <url/id>");
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            return;
        }

        String input = args[2];

        if (input.equalsIgnoreCase("<url/id>") || input.equalsIgnoreCase("<channel/url>")) {
            player.sendMessage(ChatColor.RED + "Please provide a real URL or ID, not the example text.");
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            return;
        }

        switch (platform) {
            case "youtube":
                if (plugin.getYoutubeService() != null) {
                    String videoId = plugin.getYoutubeService().getVideoIdFromUrl(input);
                    String displayName = (videoId != null) ? videoId : input;
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eConnecting to YouTube live chat for &c" + displayName + "&e..."));
                    plugin.getYoutubeService().start(player, input);
                }
                break;
            case "twitch":
                if (plugin.getTwitchService() != null) {
                    Matcher matcher = TWITCH_URL_PATTERN.matcher(input);
                    String channelName = matcher.find() ? matcher.group(1) : input;
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eConnecting to Twitch channel &d" + channelName + "&e..."));
                    plugin.getTwitchService().start(player, channelName);
                }
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown platform. Available platforms: youtube, twitch");
                player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                break;
        }
    }

    private void handleStopCommand(Player player, String[] args) {
        if (args.length < 2) {
            boolean stoppedYoutube = plugin.getYoutubeService() != null && plugin.getYoutubeService().stop(player, true);
            boolean stoppedTwitch = plugin.getTwitchService() != null && plugin.getTwitchService().stop(player, true);

            if (stoppedYoutube || stoppedTwitch) {
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
            case "youtube":
                if (plugin.getYoutubeService() != null) {
                    stopped = plugin.getYoutubeService().stop(player, false);
                }
                break;
            case "twitch":
                if (plugin.getTwitchService() != null) {
                    stopped = plugin.getTwitchService().stop(player, false);
                }
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown platform: " + platform);
                player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                return;
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