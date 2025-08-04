package com.namLivechat.platform.TikTok;

import com.namLivechat.NamLivechat;
import com.namLivechat.service.AlertService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TikTokService {

    private final NamLivechat plugin;
    private final AlertService alertService;
    private final boolean isFolia;
    private final Map<UUID, TikTokConnection> connections = new ConcurrentHashMap<>();

    public TikTokService(NamLivechat plugin, AlertService alertService, boolean isFolia) {
        this.plugin = plugin;
        this.alertService = alertService;
        this.isFolia = isFolia;
    }

    public void start(Player player, String username) {
        if (connections.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already connected to a TikTok chat.");
            return;
        }

        // ลบ @ ออกถ้าผู้ใช้ใส่มา
        if (username.startsWith("@")) {
            username = username.substring(1);
        }

        TikTokConnection connection = new TikTokConnection(plugin, player, username, alertService, isFolia);
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