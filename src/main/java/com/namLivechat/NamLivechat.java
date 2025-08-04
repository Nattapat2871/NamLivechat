package com.namLivechat;

import com.namLivechat.command.AdminCommand;
import com.namLivechat.command.LiveChatCommand;
import com.namLivechat.command.TabCommand;
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
    private AlertService alertService;
    private boolean isFolia = false;

    private FileConfiguration youtubeConfig;
    private FileConfiguration twitchConfig;

    @Override
    public void onEnable() {
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

        TabCommand tabCompleter = new TabCommand(this);
        getCommand("livechat").setExecutor(new LiveChatCommand(this));
        getCommand("livechat").setTabCompleter(tabCompleter);
        getCommand("namlivechat").setExecutor(new AdminCommand(this));
        getCommand("namlivechat").setTabCompleter(tabCompleter);

        displayStartupMessage();
    }

    @Override
    public void onDisable() {
        if(twitchService != null) twitchService.stopAll();
        if(youtubeService != null) youtubeService.stopAll();
        getLogger().info("NamLivechat has been disabled.");
    }

    private void configureLibraryLogging() {
        // --- ส่วนที่แก้ไข: ระบุ Logger ให้เฉพาะเจาะจงมากขึ้น ---
        // ปิด Log ทั้งหมดของ twitch4j, netflix, และ xanthic ที่ไม่จำเป็น
        Logger.getLogger("com.namLivechat.libs.twitch4j.chat.TwitchChat").setLevel(Level.SEVERE);
        Logger.getLogger("com.namLivechat.libs.netflix.config.sources.URLConfigurationSource").setLevel(Level.OFF);
        Logger.getLogger("com.namLivechat.libs.netflix.config.DynamicPropertyFactory").setLevel(Level.OFF);
        Logger.getLogger("com.namLivechat.libs.xanthic.cache.core.CacheApiSettings").setLevel(Level.OFF);
        // --------------------------------------------------------
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
        if (!youtubeConfigFile.exists()) {
            saveResource("youtube-config.yml", false);
        }
        youtubeConfig = YamlConfiguration.loadConfiguration(youtubeConfigFile);

        File twitchConfigFile = new File(getDataFolder(), "twitch-config.yml");
        if (!twitchConfigFile.exists()) {
            saveResource("twitch-config.yml", false);
        }
        twitchConfig = YamlConfiguration.loadConfiguration(twitchConfigFile);
    }

    public void reloadPlugin() {
        loadConfigs();
        if (youtubeService != null) youtubeService.initialize();
        if (twitchService != null) twitchService.initialize();
        getLogger().info("Configuration files have been reloaded.");
    }

    // Getters
    public FileConfiguration getYoutubeConfig() { return youtubeConfig; }
    public FileConfiguration getTwitchConfig() { return twitchConfig; }
    public YouTubeService getYoutubeService() { return youtubeService; }
    public TwitchService getTwitchService() { return twitchService; }
    public AlertService getAlertService() { return alertService; }
    public boolean isFolia() { return isFolia; }
}