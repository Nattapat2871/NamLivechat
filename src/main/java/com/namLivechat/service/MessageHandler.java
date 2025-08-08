package com.namLivechat.service;

import com.namLivechat.NamLivechat;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MessageHandler {

    private final NamLivechat plugin;
    private FileConfiguration langConfig;

    public MessageHandler(NamLivechat plugin) {
        this.plugin = plugin;
        reloadMessages();
    }

    public void reloadMessages() {
        String lang = plugin.getConfig().getString("language", "en");
        File langFile = new File(plugin.getDataFolder(), "messages/" + lang + ".yml");

        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file '" + lang + ".yml' not found. Defaulting to en.yml.");
            plugin.saveResource("messages/en.yml", false);
            lang = "en";
            langFile = new File(plugin.getDataFolder(), "messages/en.yml");
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);

        try (InputStream defaultStream = plugin.getResource("messages/" + lang + ".yml")) {
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                langConfig.setDefaults(defaultConfig);
                langConfig.options().copyDefaults(true);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Could not load default messages for " + lang);
            e.printStackTrace();
        }
    }

    private String getMessage(String path) {
        String message = langConfig.getString(path);
        return message != null ? ChatColor.translateAlternateColorCodes('&', message) : ChatColor.RED + "Missing message: " + path;
    }

    public void sendMessage(CommandSender sender, String path) {
        if (langConfig.isList(path)) {
            List<String> messages = langConfig.getStringList(path);
            for (String msg : messages) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            }
        } else {
            sender.sendMessage(getMessage(path));
        }
    }

    public String getFormattedMessage(String path, String... replacements) {
        String message = getMessage(path);
        if (replacements.length % 2 != 0) {
            return ChatColor.RED + "Invalid replacements for message: " + path;
        }
        for (int i = 0; i < replacements.length; i += 2) {
            if (replacements[i] != null && replacements[i+1] != null) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return message;
    }

    public void sendFormattedMessage(CommandSender sender, String path, String... replacements) {
        String formattedMessage = getFormattedMessage(path, replacements);
        sender.sendMessage(formattedMessage);
    }
}