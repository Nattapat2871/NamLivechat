package com.namLivechat.platform.Twitch;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.namLivechat.NamLivechat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;

public class TwitchChatListener {

    private final NamLivechat plugin;
    private final UUID playerUUID;
    private final TwitchMessageFormatter formatter;
    private final Consumer<String> messageConsumer; // Callback ที่จะถูกเรียกเมื่อมีข้อความ

    public TwitchChatListener(NamLivechat plugin, UUID playerUUID, TwitchMessageFormatter formatter) {
        this.plugin = plugin;
        this.playerUUID = playerUUID;
        this.formatter = formatter;
        // กำหนดให้ Callback คือการส่งข้อความหาผู้เล่น
        this.messageConsumer = this::sendMessageToPlayer;
    }

    // เมธอดนี้จะถูกเรียกโดย EventManager ของ TwitchClient
    public void onChannelMessage(ChannelMessageEvent event) {
        String formattedMessage = formatter.format(event);
        if (formattedMessage != null) {
            messageConsumer.accept(formattedMessage);
        }
    }

    private void sendMessageToPlayer(String message) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            // เราต้องกลับมาที่ Thread ของ Player เพื่อส่งข้อความ
            boolean isFolia = isFoliaServer();
            if (isFolia) {
                player.getScheduler().run(plugin, task -> player.sendMessage(message), null);
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(message));
            }
        }
    }

    private boolean isFoliaServer() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}