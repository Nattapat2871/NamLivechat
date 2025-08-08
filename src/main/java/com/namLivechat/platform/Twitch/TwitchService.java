package com.namLivechat.platform.Twitch;

import com.namLivechat.NamLivechat;
import com.namLivechat.service.AlertService;
import com.namLivechat.service.MessageHandler;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TwitchService {

    private final NamLivechat plugin;
    private final AlertService alertService;
    private final MessageHandler messageHandler;
    private final boolean isFolia;
    private final Map<UUID, TwitchConnection> connections = new ConcurrentHashMap<>();

    public TwitchService(NamLivechat plugin) {
        this.plugin = plugin;
        this.alertService = plugin.getAlertService();
        this.messageHandler = plugin.getMessageHandler();
        this.isFolia = plugin.isFolia();
    }

    public void initialize() {
        // This service does not require special initialization
    }

    public void start(Player player, String channelName) {
        if (!plugin.getTwitchConfig().getBoolean("enabled", false)) {
            messageHandler.sendMessage(player, "twitch_disabled");
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            return;
        }
        if (connections.containsKey(player.getUniqueId())) {
            messageHandler.sendFormattedMessage(player, "already_connected", "%platform%", "Twitch");
            return;
        }

        String oauthToken = plugin.getTwitchConfig().getString("oauth-token");
        if (oauthToken == null || oauthToken.isEmpty() || oauthToken.equalsIgnoreCase("YOUR_OAUTH_TOKEN_HERE")) {
            messageHandler.sendMessage(player, "twitch_no_token");
            return;
        }

        TwitchConnection connection = new TwitchConnection(plugin, player, channelName);
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