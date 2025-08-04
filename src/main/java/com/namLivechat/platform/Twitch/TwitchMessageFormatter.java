package com.namLivechat.platform.Twitch;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.enums.CommandPermission; // เพิ่ม import นี้
import org.bukkit.ChatColor; // เพิ่ม import นี้
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set; // เพิ่ม import นี้

public class TwitchMessageFormatter {
    private final FileConfiguration config;
    private final String messageFormat;

    public TwitchMessageFormatter(FileConfiguration config) {
        this.config = config;
        // ใช้ format จากไฟล์ twitch-config.yml
        this.messageFormat = config.getString("format", "&d[Twitch] %user%&7: &f%message%");
    }

    public String format(ChannelMessageEvent event) {
        // --- ส่วนที่แก้ไข: นำสีของยศมาใส่ให้ชื่อ User ---
        String userColor = getTwitchUserColor(event.getPermissions());
        String coloredUser = userColor + event.getUser().getName();

        String message = event.getMessage();
        String messageWithoutEmotes = removeTwitchEmotes(message, event.getMessageEvent().getTags().get("emotes"));

        String formattedMessage = this.messageFormat
                .replace("%user%", coloredUser) // ใช้ %user% ที่มีสีแล้ว
                .replace("%message%", messageWithoutEmotes);

        // --- ส่วนที่แก้ไข: แปลงโค้ดสีทั้งหมดก่อนส่งออกไป ---
        return ChatColor.translateAlternateColorCodes('&', formattedMessage);
    }

    // --- ส่วนที่แก้ไข: เพิ่มเมธอดนี้กลับเข้ามาเพื่อดึงสีตามยศ ---
    private String getTwitchUserColor(Set<CommandPermission> permissions) {
        if (permissions.contains(CommandPermission.BROADCASTER)) {
            return config.getString("role-colors.broadcaster", "&6");
        } else if (permissions.contains(CommandPermission.MODERATOR)) {
            return config.getString("role-colors.moderator", "&9");
        } else if (permissions.contains(CommandPermission.SUBSCRIBER)) {
            return config.getString("role-colors.subscriber", "&a");
        } else if (permissions.contains(CommandPermission.VIP)) {
            return config.getString("role-colors.vip", "&d");
        }
        return config.getString("role-colors.default", "&7");
    }

    private String removeTwitchEmotes(String message, String emotesTag) {
        if (emotesTag == null || emotesTag.isEmpty() || message.isEmpty()) {
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
                } catch (NumberFormatException e) {
                    // Ignore
                }
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