package com.namLivechat.platform.TikTok;

import com.namLivechat.NamLivechat;
import com.namLivechat.service.AlertService;
import com.namLivechat.service.MessageHandler;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TikTokService {

    private final NamLivechat plugin;
    private final AlertService alertService;
    private final MessageHandler messageHandler;
    private final boolean isFolia;
    private final Map<UUID, TikTokConnection> connections = new ConcurrentHashMap<>();

    public TikTokService(NamLivechat plugin) {
        this.plugin = plugin;
        this.alertService = plugin.getAlertService();
        this.messageHandler = plugin.getMessageHandler();
        this.isFolia = plugin.isFolia();
    }

    public void initialize() {
        // This service does not require special initialization
    }

    public void start(Player player, String username) {
        if (!plugin.getTiktokConfig().getBoolean("enabled", false)) {
            messageHandler.sendMessage(player, "tiktok_disabled");
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            return;
        }
        if (connections.containsKey(player.getUniqueId())) {
            messageHandler.sendFormattedMessage(player, "already_connected", "%platform%", "TikTok");
            return;
        }

        if (username.startsWith("@")) {
            username = username.substring(1);
        }

        TikTokConnection connection = new TikTokConnection(plugin, player, username);
        connections.put(player.getUniqueId(), connection);
        connection.start();
    }

    public boolean stop(Player player, boolean silent) {
        TikTokConnection connection = connections.remove(player.getUniqueId());
        if (connection != null) {
            connection.stop();
            return true;
        }
        return false;
    }

    public void stopAll() {
        connections.values().forEach(TikTokConnection::stop);
        connections.clear();
    }

    public boolean isConnected(Player player) {
        return connections.containsKey(player.getUniqueId());
    }

    public void removeConnection(UUID playerUUID) {
        connections.remove(playerUUID);
    }
}