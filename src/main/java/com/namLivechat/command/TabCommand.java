package com.namLivechat.command;

import com.namLivechat.NamLivechat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TabCommand implements TabCompleter {

    private final NamLivechat plugin;

    public TabCommand(NamLivechat plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("namlivechat")) {
            if (args.length == 1) {
                return filterAndCollect(Collections.singletonList("reload"), args[0]);
            }
        } else if (command.getName().equalsIgnoreCase("livechat")) {
            if (args.length == 1) {
                return filterAndCollect(Arrays.asList("start", "stop"), args[0]);
            }

            if (args.length == 2) {
                if ("start".equalsIgnoreCase(args[0])) {
                    return filterAndCollect(Arrays.asList("youtube", "twitch", "tiktok"), args[1]);
                } else if ("stop".equalsIgnoreCase(args[0])) {
                    List<String> activePlatforms = new ArrayList<>();
                    if (plugin.getYoutubeService().isConnected(player)) activePlatforms.add("youtube");
                    if (plugin.getTwitchService().isRunning(player)) activePlatforms.add("twitch");
                    if (plugin.getTiktokService().isConnected(player)) activePlatforms.add("tiktok");
                    return filterAndCollect(activePlatforms, args[1]);
                }
            }

            if (args.length == 3 && "start".equalsIgnoreCase(args[0])) {
                return switch (args[1].toLowerCase()) {
                    case "youtube" -> Collections.singletonList("<url/id>");
                    case "twitch" -> Collections.singletonList("<channel/url>");
                    case "tiktok" -> Collections.singletonList("<@username/url>");
                    default -> Collections.emptyList();
                };
            }
        }

        return Collections.emptyList();
    }

    private List<String> filterAndCollect(List<String> source, String arg) {
        return source.stream()
                .filter(s -> s.startsWith(arg.toLowerCase()))
                .collect(Collectors.toList());
    }
}