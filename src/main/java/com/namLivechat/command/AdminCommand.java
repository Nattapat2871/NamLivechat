package com.namLivechat.command;

import com.namLivechat.NamLivechat;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.Collections;
import java.util.List;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final NamLivechat plugin;

    public AdminCommand(NamLivechat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && "reload".equalsIgnoreCase(args[0])) {
            plugin.reload();
            sender.sendMessage(ChatColor.GREEN + "NamLivechat configuration has been reloaded.");
            return true;
        }

        PluginDescriptionFile desc = plugin.getDescription();
        sender.sendMessage(ChatColor.AQUA + "--- " + desc.getName() + " Admin ---");
        sender.sendMessage(ChatColor.WHITE + "Version: " + ChatColor.YELLOW + desc.getVersion());
        sender.sendMessage(ChatColor.WHITE + "Author: " + ChatColor.YELLOW + desc.getAuthors().get(0));
        sender.sendMessage(ChatColor.WHITE + "GitHub: " + ChatColor.YELLOW + "https://github.com/Nattapat2871");
        sender.sendMessage(ChatColor.GREEN + "Usage: /" + label + " reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("reload");
        }
        return Collections.emptyList();
    }
}