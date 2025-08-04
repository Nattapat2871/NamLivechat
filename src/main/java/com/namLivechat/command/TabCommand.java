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
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("namlivechat")) {
            if (args.length == 1) {
                if ("reload".startsWith(args[0].toLowerCase())) {
                    return Collections.singletonList("reload");
                }
            }
        } else if (command.getName().equalsIgnoreCase("livechat")) {
            if (args.length == 1) {
                return filterAndCollect(Arrays.asList("start", "stop"), args[0]);
            }

            if (args.length == 2) {
                if ("start".equalsIgnoreCase(args[0])) {
                    return filterAndCollect(Arrays.asList("youtube", "twitch"), args[1]);
                } else if ("stop".equalsIgnoreCase(args[0])) {
                    List<String> activePlatforms = new ArrayList<>();
                    if (plugin.getYoutubeService() != null && plugin.getYoutubeService().isConnected(player)) {
                        activePlatforms.add("youtube");
                    }
                    if (plugin.getTwitchService() != null && plugin.getTwitchService().isRunning(player)) {
                        activePlatforms.add("twitch");
                    }
                    return filterAndCollect(activePlatforms, args[1]);
                }
            }

            if (args.length == 3 && "start".equalsIgnoreCase(args[0])) {
                if ("youtube".equalsIgnoreCase(args[1])) {
                    return Collections.singletonList("<url/id>");
                }
                if ("twitch".equalsIgnoreCase(args[1])) {
                    return Collections.singletonList("<channel/url>");
                }
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