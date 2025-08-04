package com.namLivechat;

import com.namLivechat.command.AdminCommand;
import com.namLivechat.command.LiveChatCommand;
import com.namLivechat.platform.Twitch.TwitchService; // **เพิ่ม import ที่นี่**
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NamLivechat extends JavaPlugin implements Listener {

    private static NamLivechat instance;
    private LiveChatCommand liveChatCommand;
    private TwitchService twitchService; // **1. ประกาศตัวแปร**

    private File twitchConfigFile;
    private FileConfiguration twitchConfig;


    @Override
    public void onEnable() {
        instance = this;
        displayStartupMessage();

        configureLibraryLogging();
        saveDefaultConfig();
        createTwitchConfig();

        // **2. สร้าง Instance ของ TwitchService**
        this.twitchService = new TwitchService(this);

        // Initialize commands
        liveChatCommand = new LiveChatCommand(this);
        getCommand("livechat").setExecutor(liveChatCommand);
        getCommand("livechat").setTabCompleter(liveChatCommand);

        AdminCommand adminCommand = new AdminCommand(this);
        getCommand("namlivechat").setExecutor(adminCommand);
        getCommand("namlivechat").setTabCompleter(adminCommand);

        getServer().getPluginManager().registerEvents(this, this);

        String apiKey = getConfig().getString("youtube-api-key");
        liveChatCommand.initializeServices(apiKey);

        getLogger().info("NamLivechat has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (liveChatCommand != null) {
            liveChatCommand.stopAllTasks();
        }
        getLogger().info("NamLivechat has been disabled!");
    }

    public void reload() {
        if (liveChatCommand != null) {
            liveChatCommand.stopAllTasks();
        }

        reloadConfig();
        reloadTwitchConfig();

        String apiKey = getConfig().getString("youtube-api-key");
        liveChatCommand.initializeServices(apiKey);
        getLogger().info("NamLivechat configuration has been reloaded.");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (liveChatCommand != null) {
            liveChatCommand.stopAllTasksForPlayer(event.getPlayer(), true);
        }
    }

    private void configureLibraryLogging() {
        Logger.getLogger("com.namLivechat.libs.google").setLevel(Level.SEVERE);
        Logger.getLogger("com.namLivechat.libs.netflix").setLevel(Level.SEVERE);
        Logger.getLogger("com.namLivechat.libs.xanthic").setLevel(Level.WARNING);
        Logger.getLogger("com.namLivechat.libs.twitch4j").setLevel(Level.WARNING);
        getLogger().info("Third-party library logging levels have been adjusted to reduce console spam.");
    }

    private void displayStartupMessage() {
        String serverSoftware = Bukkit.getName();
        String serverVersion = Bukkit.getServer().getMinecraftVersion();
        String pluginVersion = getDescription().getVersion();

        Bukkit.getConsoleSender().sendMessage(ChatColor.BLUE + "--------------------------------------------------");
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&3 NamLivechat &e" + pluginVersion));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&e Running on &6" + serverSoftware + "&e" + serverVersion));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&3 Created by: &bNattapat2871"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&8 GitHub: &fhttps://github.com/Nattapat2871"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.BLUE + "--------------------------------------------------");
    }

    public FileConfiguration getTwitchConfig() {
        return this.twitchConfig;
    }

    private void createTwitchConfig() {
        twitchConfigFile = new File(getDataFolder(), "twitch-config.yml");
        if (!twitchConfigFile.exists()) {
            twitchConfigFile.getParentFile().mkdirs();
            saveResource("twitch-config.yml", false);
        }

        twitchConfig = new YamlConfiguration();
        try {
            twitchConfig.load(twitchConfigFile);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void reloadTwitchConfig() {
        if (twitchConfigFile == null) {
            twitchConfigFile = new File(getDataFolder(), "twitch-config.yml");
        }
        twitchConfig = YamlConfiguration.loadConfiguration(twitchConfigFile);
    }

    // **3. สร้าง Getter เพื่อให้คลาสอื่นเรียกใช้ได้**
    public TwitchService getTwitchService() {
        return twitchService;
    }

    public static NamLivechat getInstance() {
        return instance;
    }
}