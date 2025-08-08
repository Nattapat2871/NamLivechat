package com.namLivechat.platform.Twitch;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.namLivechat.NamLivechat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TwitchChatListener {

    private final NamLivechat plugin;
    private final UUID playerUUID;
    private final TwitchMessageFormatter formatter;

    public TwitchChatListener(NamLivechat plugin, UUID playerUUID, TwitchMessageFormatter formatter) {
        this.plugin = plugin;
        this.playerUUID = playerUUID;
        this.formatter = formatter;
    }

    public void onChannelMessage(ChannelMessageEvent event) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) {
            return;
        }

        String formattedMessage = formatter.format(event);
        String finalMessage = ChatColor.translateAlternateColorCodes('&', formattedMessage);

        if (plugin.isFolia()) {
            player.getScheduler().run(plugin, (task) -> player.sendMessage(finalMessage), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(finalMessage));
        }
    }
}