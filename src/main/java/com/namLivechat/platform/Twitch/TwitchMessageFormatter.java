package com.namLivechat.platform.Twitch;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.enums.CommandPermission;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.Set;

public class TwitchMessageFormatter {
    private final FileConfiguration config;
    private final String messageFormat;

    public TwitchMessageFormatter(FileConfiguration config) {
        this.config = config;
        this.messageFormat = config.getString("format", "&5[Twitch] %user% : &f%message%");
    }

    public String format(ChannelMessageEvent event) {
        String badges = formatBadges(event.getMessageEvent().getBadges());

        // --- ส่วนที่แก้ไข: เพิ่มการดึงสียศ ---
        String userColor = getAuthorColor(event.getPermissions());
        String coloredUsername = userColor + event.getUser().getName();
        // ------------------------------------

        String message = event.getMessage();
        String messageWithoutEmotes = removeTwitchEmotes(message, event.getMessageEvent().getTags().get("emotes"));

        String result = this.messageFormat
                .replace("%badges%", badges)
                .replace("%user%", coloredUsername) // ใช้ชื่อที่ใส่สีแล้ว
                .replace("%message%", messageWithoutEmotes);

        return ChatColor.translateAlternateColorCodes('&', result);
    }

    private String formatBadges(Map<String, String> badges) {
        if (badges == null || badges.isEmpty()) {
            return "";
        }
        StringBuilder formattedBadges = new StringBuilder();
        for (String badge : badges.keySet()) {
            String color = config.getString("chat-format.badge-colors." + badge, "&f");
            String symbol = config.getString("chat-format.badge-symbols." + badge, "[" + badge.substring(0, 1).toUpperCase() + "]");
            formattedBadges.append(color).append(symbol).append("&r ");
        }
        return formattedBadges.toString();
    }

    // --- เพิ่มเมธอดใหม่: สำหรับดึงสีตามยศ ---
    private String getAuthorColor(Set<CommandPermission> permissions) {
        if (permissions.contains(CommandPermission.BROADCASTER)) {
            return config.getString("role-colors.broadcaster", "&6");
        } else if (permissions.contains(CommandPermission.MODERATOR)) {
            return config.getString("role-colors.moderator", "&9");
        } else if (permissions.contains(CommandPermission.VIP)) {
            return config.getString("role-colors.vip", "&d");
        } else if (permissions.contains(CommandPermission.SUBSCRIBER)) {
            return config.getString("role-colors.subscriber", "&a");
        }
        return config.getString("role-colors.default", "&7");
    }
    // ------------------------------------------

    private String removeTwitchEmotes(String message, String emotesTag) {
        if (message == null || message.isEmpty() || emotesTag == null || emotesTag.isEmpty()) {
            return message;
        }

        boolean[] isEmoteChar = new boolean[message.length()];

        String[] emotes = emotesTag.split("/");
        for (String emote : emotes) {
            String[] parts = emote.split(":");
            if (parts.length < 2) continue;

            String[] ranges = parts[1].split(",");
            for (String range : ranges) {
                String[] pos = range.split("-");
                if (pos.length < 2) continue;
                try {
                    int start = Integer.parseInt(pos[0]);
                    int end = Integer.parseInt(pos[1]);
                    for (int i = start; i <= end && i < message.length(); i++) {
                        isEmoteChar[i] = true;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        StringBuilder newMessage = new StringBuilder();
        for (int i = 0; i < message.length(); i++) {
            if (!isEmoteChar[i]) {
                newMessage.append(message.charAt(i));
            }
        }

        return newMessage.toString().trim().replaceAll(" +", " ");
    }
}