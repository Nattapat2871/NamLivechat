package com.namLivechat.command;

import com.namLivechat.NamLivechat;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AdminCommand implements CommandExecutor {

    private final NamLivechat plugin;

    public AdminCommand(NamLivechat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("namlivechat.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            sender.sendMessage(ChatColor.GREEN + "NamLivechat configuration reloaded.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "Usage: /namlivechat reload");
        return true;
    }
}