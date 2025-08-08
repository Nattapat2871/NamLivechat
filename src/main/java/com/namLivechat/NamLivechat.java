package com.namLivechat;

import com.namLivechat.command.AdminCommand;
import com.namLivechat.command.LiveChatCommand;
import com.namLivechat.command.TabCommand;
import com.namLivechat.platform.TikTok.TikTokService;
import com.namLivechat.platform.Twitch.TwitchService;
import com.namLivechat.platform.Youtube.YouTubeService;
import com.namLivechat.service.AlertService;
import com.namLivechat.service.MessageHandler;
import com.namLivechat.service.UpdateChecker;
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
    private UpdateChecker updateChecker;
    private MessageHandler messageHandler;

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

        loadConfigs();
        configureLibraryLogging();

        this.messageHandler = new MessageHandler(this);
        this.alertService = new AlertService(this);
        this.youtubeService = new YouTubeService(this);
        this.twitchService = new TwitchService(this);
        this.tiktokService = new TikTokService(this);

        this.updateChecker = new UpdateChecker(this);
        getServer().getPluginManager().registerEvents(updateChecker, this);
        updateChecker.check();

        getCommand("livechat").setExecutor(new LiveChatCommand(this));
        getCommand("namlivechat").setExecutor(new AdminCommand(this));
        TabCommand tabCompleter = new TabCommand(this);
        getCommand("livechat").setTabCompleter(tabCompleter);
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
        boolean debug = getConfig().getBoolean("debug-mode", false);
        Level level = debug ? Level.INFO : Level.SEVERE;

        Logger.getLogger("com.namLivechat.libs.twitch4j").setLevel(level);
        Logger.getLogger("com.namLivechat.libs.netflix").setLevel(level);
        Logger.getLogger("com.namLivechat.libs.xanthic").setLevel(level);
        Logger.getLogger("io.github.jwdeveloper.tiktok").setLevel(level);
    }

    private void displayStartupMessage() {
        String serverSoftware = Bukkit.getName();
        String serverVersion = Bukkit.getServer().getMinecraftVersion();
        String pluginVersion = getDescription().getVersion();

        String ytKey = getYoutubeConfig().getString("youtube-api-key");
        boolean isYtReady = !(ytKey == null || ytKey.isEmpty() || ytKey.equals("YOUR_API_KEY_HERE"));
        String ytStatus = isYtReady ? "&aConfigured" : "&cNot Configured";

        String twitchToken = getTwitchConfig().getString("oauth-token");
        boolean isTwitchReady = !(twitchToken == null || twitchToken.isEmpty() || twitchToken.equalsIgnoreCase("YOUR_OAUTH_TOKEN_HERE"));
        String twitchStatus = isTwitchReady ? "&aConfigured" : "&cNot Configured";

        String tiktokStatus = "&eNot Required";

        String statusLine = String.format("&7Status: &eYouTube: %s &7| &dTwitch: %s &7| &bTikTok: %s", ytStatus, twitchStatus, tiktokStatus);

        Bukkit.getConsoleSender().sendMessage(ChatColor.BLUE + "--------------------------------------------------");
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&3 |\\ |  /\\ |\\  /| &3 NamLivechat &e" + pluginVersion));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&3 | \\| /--\\| \\/ | &e Running on &6" + serverSoftware + " &5 " + serverVersion));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&3 Created by: &bNattapat2871"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&8 GitHub: &fhttps://github.com/Nattapat2871/Namlivechat"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', statusLine));
        Bukkit.getConsoleSender().sendMessage(ChatColor.BLUE + "--------------------------------------------------");
    }

    public void loadConfigs() {
        saveDefaultConfig();
        super.reloadConfig();

        saveResource("messages/en.yml", true);
        saveResource("messages/th.yml", true);

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

    public void logDebug(String message) {
        if (getConfig().getBoolean("debug-mode", false)) {
            getLogger().info("[Debug] " + message);
        }
    }

    public void reloadPlugin() {
        loadConfigs();
        if (messageHandler != null) messageHandler.reloadMessages();
        configureLibraryLogging();
        if (youtubeService != null) youtubeService.initialize();
        if (twitchService != null) twitchService.initialize();
        getLogger().info("Configuration files have been reloaded.");
    }

    public boolean isDisabling() { return isDisabling; }
    public File getPluginFile() { return getFile(); }

    // Getters
    public MessageHandler getMessageHandler() { return messageHandler; }
    public FileConfiguration getYoutubeConfig() { return youtubeConfig; }
    public FileConfiguration getTwitchConfig() { return twitchConfig; }
    public FileConfiguration getTiktokConfig() { return tiktokConfig; }
    public YouTubeService getYoutubeService() { return youtubeService; }
    public TwitchService getTwitchService() { return twitchService; }
    public TikTokService getTiktokService() { return tiktokService; }
    public AlertService getAlertService() { return alertService; }
    public UpdateChecker getUpdateChecker() { return updateChecker; }
    public boolean isFolia() { return isFolia; }
}