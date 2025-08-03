package com.namLivechat;

import com.namLivechat.command.AdminCommand;
import com.namLivechat.command.LiveChatCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class NamLivechat extends JavaPlugin implements Listener {

    private static NamLivechat instance;
    private LiveChatCommand liveChatCommand;

    @Override
    public void onEnable() {
        instance = this;
        displayStartupMessage();
        saveDefaultConfig();

        liveChatCommand = new LiveChatCommand(this);
        getCommand("livechat").setExecutor(liveChatCommand);
        getCommand("livechat").setTabCompleter(liveChatCommand);

        AdminCommand adminCommand = new AdminCommand(this);
        getCommand("namlivechat").setExecutor(adminCommand);
        getCommand("namlivechat").setTabCompleter(adminCommand);

        getServer().getPluginManager().registerEvents(this, this);

        reload();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (liveChatCommand != null) {
            // Stop all tasks for the quitting player across all platforms
            liveChatCommand.stopAllTasksForPlayer(event.getPlayer());
        }
    }

    public void reload() {
        if (liveChatCommand != null) {
            liveChatCommand.stopAllTasks();
        }
        reloadConfig();
        String apiKey = getConfig().getString("youtube-api-key");
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            getLogger().warning("YouTube API Key is not set in config.yml!");
            getLogger().warning("YouTube features will be disabled until you set a key and run /namlivechat reload");
            liveChatCommand.initializeServices(null);
        } else {
            liveChatCommand.initializeServices(apiKey);
        }
    }

    private void displayStartupMessage() {
        String serverSoftware = Bukkit.getName();
        String serverVersion = Bukkit.getServer().getMinecraftVersion();
        String pluginVersion = getDescription().getVersion();

        Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "--------------------------------------------------");
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&b _   _   &9 __   __ &b  __  &9 __ &f NamLiveChat &e" + pluginVersion));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&b| \\ | | &9 /  \\ /  |&b|  \\/  |&7 Running on " + serverSoftware + " " + serverVersion));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&b|  \\| | &9| () | () |&b| |\\/| |&7 Created by: &dNattapat2871"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&b|_|\\__|&9 \\__/\\__/ &b|_|  |_|&7 GitHub: &fhttps://github.com/Nattapat2871"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "--------------------------------------------------");
    }

    @Override
    public void onDisable() {
        if (liveChatCommand != null) {
            liveChatCommand.stopAllTasks();
        }
        getLogger().info("NamLivechat has been disabled!");
    }

    public static NamLivechat getInstance() {
        return instance;
    }
}