package com.namLivechat.command;

import com.namLivechat.NamLivechat;
import com.namLivechat.service.MessageHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AdminCommand implements CommandExecutor {

    private final NamLivechat plugin;
    private final MessageHandler messageHandler;

    public AdminCommand(NamLivechat plugin) {
        this.plugin = plugin;
        this.messageHandler = plugin.getMessageHandler();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("namlivechat.admin")) {
            messageHandler.sendMessage(sender, "no_permission");
            return true;
        }

        if (args.length == 0) {
            messageHandler.sendMessage(sender, "admin_usage");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "reload":
                plugin.reloadPlugin();
                messageHandler.sendMessage(sender, "admin_reload");
                break;

            case "update":
                plugin.getUpdateChecker().downloadUpdate(sender);
                break;

            default:
                messageHandler.sendMessage(sender, "admin_usage");
                break;
        }
        return true;
    }
}