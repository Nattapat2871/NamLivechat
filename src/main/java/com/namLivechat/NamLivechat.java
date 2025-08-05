package com.namLivechat;

import com.namLivechat.command.AdminCommand;
import com.namLivechat.command.LiveChatCommand;
import com.namLivechat.command.TabCommand;
import com.namLivechat.platform.TikTok.TikTokService; // แก้ไข import ที่นี่
import com.namLivechat.platform.Twitch.TwitchService;
import com.namLivechat.platform.Youtube.YouTubeService;
import com.namLivechat.service.AlertService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NamLivechat extends JavaPlugin {

    private YouTubeService youtubeService;
    private TwitchService twitchService;
    private TikTokService tiktokService;
    private AlertService alertService;
    private boolean isFolia = false;
    private boolean isDisabling = false;

    private FileConfiguration youtubeConfig;
    private FileConfiguration twitchConfig;
    private FileConfiguration tiktokConfig;

    @Override
    public void onEnable() {
        isDisabling = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }

        configureLibraryLogging();
        loadConfigs();

        this.alertService = new AlertService(this, isFolia);
        this.youtubeService = new YouTubeService(this, alertService, isFolia);
        this.twitchService = new TwitchService(this, alertService, isFolia);
        this.tiktokService = new TikTokService(this, alertService, isFolia);

        TabCommand tabCompleter = new TabCommand(this);
        getCommand("livechat").setExecutor(new LiveChatCommand(this));
        getCommand("livechat").setTabCompleter(tabCompleter);
        getCommand("namlivechat").setExecutor(new AdminCommand(this));
        getCommand("namlivechat").setTabCompleter(tabCompleter);

        displayStartupMessage();
    }

    @Override
    public void onDisable() {
        isDisabling = true;
        if (twitchService != null) twitchService.stopAll();
        if (youtubeService != null) youtubeService.stopAll();
        if (tiktokService != null) tiktokService.stopAll();
        getLogger().info("NamLivechat has been disabled.");
    }

    private void configureLibraryLogging() {
        Logger.getLogger("com.namLivechat.libs.twitch4j.chat.TwitchChat").setLevel(Level.SEVERE);
        Logger.getLogger("com.namLivechat.libs.netflix.config.sources.URLConfigurationSource").setLevel(Level.OFF);
        Logger.getLogger("com.namLivechat.libs.netflix.config.DynamicPropertyFactory").setLevel(Level.OFF);
        Logger.getLogger("com.namLivechat.libs.xanthic.cache.core.CacheApiSettings").setLevel(Level.OFF);
        Logger.getLogger("io.github.jwdeveloper.tiktok").setLevel(Level.SEVERE);
    }

    private void displayStartupMessage() {
        String serverSoftware = Bukkit.getName();
        String serverVersion = Bukkit.getServer().getMinecraftVersion();
        String pluginVersion = getDescription().getVersion();

        Bukkit.getConsoleSender().sendMessage(ChatColor.BLUE + "--------------------------------------------------");
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&3 |\\ |  /\\ |\\  /| &3 NamLivechat &e" + pluginVersion));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&3 | \\| /--\\| \\/ | &e Running on &6" + serverSoftware + " &5 " + serverVersion));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&3 Created by: &bNattapat2871"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&8 GitHub: &fhttps://github.com/Nattapat2871"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.BLUE + "--------------------------------------------------");
    }

    public void loadConfigs() {
        saveDefaultConfig();
        super.reloadConfig();

        File youtubeConfigFile = new File(getDataFolder(), "youtube-config.yml");
        if (!youtubeConfigFile.exists()) saveResource("youtube-config.yml", false);
        youtubeConfig = YamlConfiguration.loadConfiguration(youtubeConfigFile);

        File twitchConfigFile = new File(getDataFolder(), "twitch-config.yml");
        if (!twitchConfigFile.exists()) saveResource("twitch-config.yml", false);
        twitchConfig = YamlConfiguration.loadConfiguration(twitchConfigFile);

        File tiktokConfigFile = new File(getDataFolder(), "tiktok-config.yml");
        if (!tiktokConfigFile.exists()) saveResource("tiktok-config.yml", false);
        tiktokConfig = YamlConfiguration.loadConfiguration(tiktokConfigFile);
    }

    public void reloadPlugin() {
        loadConfigs();
        if (youtubeService != null) youtubeService.initialize();
        if (twitchService != null) twitchService.initialize();
        getLogger().info("Configuration files have been reloaded.");
    }

    public boolean isDisabling() {
        return isDisabling;
    }

    // Getters
    public FileConfiguration getYoutubeConfig() { return youtubeConfig; }
    public FileConfiguration getTwitchConfig() { return twitchConfig; }
    public FileConfiguration getTiktokConfig() { return tiktokConfig; }
    public YouTubeService getYoutubeService() { return youtubeService; }
    public TwitchService getTwitchService() { return twitchService; }
    public TikTokService getTiktokService() { return tiktokService; }
    public AlertService getAlertService() { return alertService; }
    public boolean isFolia() { return isFolia; }
}