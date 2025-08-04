package com.namLivechat.platform.Twitch;

import com.namLivechat.NamLivechat;
import com.namLivechat.service.AlertService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TwitchService {

    private final NamLivechat plugin;
    private final AlertService alertService;
    private final boolean isFolia;
    private final Map<UUID, TwitchConnection> connections = new ConcurrentHashMap<>();

    public TwitchService(NamLivechat plugin, AlertService alertService, boolean isFolia) {
        this.plugin = plugin;
        this.alertService = alertService;
        this.isFolia = isFolia;
        initialize();
    }

    public void initialize() {
        // Placeholder for future initialization logic
    }

    public void start(Player player, String channelName) {
        if (connections.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already connected to a Twitch chat.");
            return;
        }

        String oauthToken = plugin.getTwitchConfig().getString("oauth-token");
        if (oauthToken == null || oauthToken.isEmpty() || oauthToken.equals("YOUR_OAUTH_TOKEN_HERE")) {
            player.sendMessage(ChatColor.RED + "Twitch OAuth Token is not configured in twitch-config.yml.");
            return;
        }

        TwitchConnection connection = new TwitchConnection(plugin, player, channelName, oauthToken, isFolia, alertService);
        connections.put(player.getUniqueId(), connection);
        connection.start();
    }

    public boolean stop(Player player, boolean silent) {
        TwitchConnection connection = connections.get(player.getUniqueId());
        if (connection != null) {
            connection.stopManual();
            return true;
        }
        return false;
    }

    public void stopAll() {
        connections.values().forEach(TwitchConnection::stopManual);
        connections.clear();
    }

    public boolean isRunning(Player player) {
        return connections.containsKey(player.getUniqueId());
    }

    public void removeConnection(UUID uuid) {
        connections.remove(uuid);
    }
}