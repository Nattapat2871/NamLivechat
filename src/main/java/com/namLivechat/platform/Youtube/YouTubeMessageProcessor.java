package com.namLivechat.platform.Youtube;

import com.google.api.services.youtube.model.LiveChatMessage;
import com.google.api.services.youtube.model.LiveChatMessageAuthorDetails;
import com.google.api.services.youtube.model.LiveChatSuperChatDetails;
import com.namLivechat.NamLivechat;
import com.namLivechat.service.AlertService;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Locale;

public class YouTubeMessageProcessor {

    private final NamLivechat plugin;
    private final AlertService alertService;
    private final Player player;

    public YouTubeMessageProcessor(NamLivechat plugin, AlertService alertService, Player player) {
        this.plugin = plugin;
        this.alertService = alertService;
        this.player = player;
    }

    public void handleMessage(LiveChatMessage item) {
        FileConfiguration config = plugin.getYoutubeConfig();
        String type = item.getSnippet().getType();
        LiveChatMessageAuthorDetails author = item.getAuthorDetails();
        String authorName = author.getDisplayName();

        switch (type) {
            case "textMessageEvent":
                String authorColor = getAuthorColor(author, config);
                String coloredAuthor = ChatColor.translateAlternateColorCodes('&', authorColor + authorName);
                String format = config.getString("message-format", "&c[YouTube] &f%player%&7: &e%message%");

                String originalMessage = item.getSnippet().getDisplayMessage();
                String cleanMessage = originalMessage.replaceAll(":[a-zA-Z0-9_\\-]+:", "");

                String rawMessage = format
                        .replace("%player%", coloredAuthor)
                        .replace("%message%", cleanMessage);

                player.sendMessage(ChatColor.translateAlternateColorCodes('&', rawMessage));
                break;

            case "superChatEvent":
                if (config.getBoolean("youtube-alerts.show-super-chat", true)) {
                    LiveChatSuperChatDetails details = item.getSnippet().getSuperChatDetails();
                    handleAlert("super-chat", authorName, details.getAmountDisplayString(), details.getUserComment());
                }
                break;

            case "newSponsorEvent":
                if (config.getBoolean("youtube-alerts.show-new-members", true)) {
                    handleAlert("new-member", authorName, null, null);
                }
                break;
        }
    }

    private void handleAlert(String type, String authorName, String amount, String message) {
        FileConfiguration config = plugin.getYoutubeConfig();
        String path = type;

        String chatMessage = config.getString(path + ".message", "");
        chatMessage = replacePlaceholders(chatMessage, authorName, amount, message);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatMessage));

        ConfigurationSection soundSection = config.getConfigurationSection(path + ".sound");
        if (soundSection != null) {
            String soundName = soundSection.getString("name", "");
            if (!soundName.isEmpty()) {
                float volume = (float) soundSection.getDouble("volume", 1.0);
                float pitch = (float) soundSection.getDouble("pitch", 1.0);
                player.playSound(player.getLocation(), soundName.toLowerCase(Locale.ROOT), volume, pitch);
            }
        }

        if (config.getBoolean(path + ".boss-bar.enabled", false)) {
            String bossBarMessage = config.getString(path + ".boss-bar.message", "");
            bossBarMessage = replacePlaceholders(bossBarMessage, authorName, amount, message);

            try {
                BarColor color = BarColor.valueOf(config.getString(path + ".boss-bar.color", "WHITE").toUpperCase());
                int duration = config.getInt(path + ".boss-bar.duration", 10);
                alertService.showBossBarAlert(player, ChatColor.translateAlternateColorCodes('&', bossBarMessage), color, duration);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid Boss Bar color in youtube-config.yml for " + type);
            }
        }
    }

    private String replacePlaceholders(String template, String authorName, String amount, String message) {
        return template
                .replace("%player%", authorName)
                .replace("%amount%", amount != null ? amount : "")
                .replace("%message%", message != null ? message : "");
    }

    private String getAuthorColor(LiveChatMessageAuthorDetails author, FileConfiguration config) {
        if (author.getIsChatOwner() != null && author.getIsChatOwner()) return config.getString("role-colors.owner", "&6");
        if (author.getIsChatModerator() != null && author.getIsChatModerator()) return config.getString("role-colors.moderator", "&9");
        if (author.getIsChatSponsor() != null && author.getIsChatSponsor()) return config.getString("role-colors.member", "&a");
        return config.getString("role-colors.default", "&7");
    }
}