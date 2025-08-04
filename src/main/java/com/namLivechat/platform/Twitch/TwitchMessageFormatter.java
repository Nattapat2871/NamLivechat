package com.namLivechat.platform.Twitch;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.enums.CommandPermission;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class TwitchMessageFormatter {

    private final FileConfiguration twitchConfig;
    private final String format;

    public TwitchMessageFormatter(FileConfiguration twitchConfig) {
        this.twitchConfig = twitchConfig;
        this.format = twitchConfig.getString("format", "&5[Twitch] %user%&f: &7%message%");
    }

    public String format(ChannelMessageEvent event) {
        String message = removeTwitchEmotes(event);
        if (message.trim().isEmpty()) {
            return null; // ถ้าข้อความว่างเปล่าหลังลบอิโมจิ ก็ไม่ต้องส่งอะไร
        }

        String user = event.getUser().getName();
        String userColor = getTwitchUserColor(event.getPermissions());
        String coloredUser = ChatColor.translateAlternateColorCodes('&', userColor + user);

        String formattedMessage = this.format
                .replace("%user%", coloredUser)
                .replace("%message%", message);

        return ChatColor.translateAlternateColorCodes('&', formattedMessage);
    }

    // **เขียนเมธอดนี้ใหม่ทั้งหมดโดยใช้วิธีอ่าน Tags**
    private String removeTwitchEmotes(ChannelMessageEvent event) {
        String message = event.getMessage();
        String emotesTag = event.getMessageEvent().getTags().get("emotes");

        if (emotesTag == null || emotesTag.isEmpty()) {
            return message;
        }

        // สร้าง List ของตำแหน่ง [start, end]
        List<int[]> positions = new ArrayList<>();
        // emotesTag format: emoteId:start-end,start-end/emoteId2:start-end
        String[] emoteSections = emotesTag.split("/");
        for (String section : emoteSections) {
            String[] parts = section.split(":");
            if (parts.length < 2) continue;
            String[] ranges = parts[1].split(",");
            for (String range : ranges) {
                String[] indices = range.split("-");
                if (indices.length < 2) continue;
                try {
                    int start = Integer.parseInt(indices[0]);
                    int end = Integer.parseInt(indices[1]);
                    positions.add(new int[]{start, end});
                } catch (NumberFormatException ignored) {
                    // Ignore malformed emote tag parts
                }
            }
        }

        if (positions.isEmpty()) {
            return message;
        }

        // เรียงลำดับตำแหน่งจากท้ายมาหน้า เพื่อไม่ให้ index เพี้ยน
        positions.sort(Comparator.comparingInt(p -> p[0]).reversed());

        StringBuilder sb = new StringBuilder(message);
        for (int[] pos : positions) {
            sb.delete(pos[0], pos[1] + 1);
        }

        return sb.toString().trim();
    }


    private String getTwitchUserColor(Set<CommandPermission> permissions) {
        if (permissions.contains(CommandPermission.BROADCASTER)) {
            return twitchConfig.getString("role-colors.broadcaster", "&6");
        } else if (permissions.contains(CommandPermission.MODERATOR)) {
            return twitchConfig.getString("role-colors.moderator", "&9");
        } else if (permissions.contains(CommandPermission.SUBSCRIBER)) {
            return twitchConfig.getString("role-colors.subscriber", "&a");
        } else if (permissions.contains(CommandPermission.VIP)) {
            return twitchConfig.getString("role-colors.vip", "&d");
        }
        return twitchConfig.getString("role-colors.default", "&7");
    }
}