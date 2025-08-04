package com.namLivechat.platform.Twitch;

import com.namLivechat.NamLivechat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TwitchService {

    private final NamLivechat plugin;
    private final Map<UUID, TwitchConnection> playerConnections = new ConcurrentHashMap<>();
    private final boolean isFolia;

    public TwitchService(NamLivechat plugin) {
        this.plugin = plugin;
        this.isFolia = isFoliaServer();
    }

    private boolean isFoliaServer() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void start(Player player, String channelName) {
        FileConfiguration twitchConfig = plugin.getTwitchConfig();

        if (!twitchConfig.getBoolean("enabled", true)) {
            player.sendMessage(ChatColor.RED + "Twitch feature is disabled.");
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            return;
        }

        String oauthToken = twitchConfig.getString("oauth-token");
        if (oauthToken == null || oauthToken.isEmpty() || oauthToken.equalsIgnoreCase("YOUR_OAUTH_TOKEN_HERE")) {
            player.sendMessage(ChatColor.RED + "Twitch OAuth token is not set!");
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            return;
        }

        TwitchConnection connection = new TwitchConnection(plugin, player, channelName, oauthToken, isFolia);
        playerConnections.put(player.getUniqueId(), connection);
        connection.start();
    }

    public void stop(Player player, boolean silent) {
        if (player == null) return;
        TwitchConnection connection = playerConnections.remove(player.getUniqueId());

        if (connection != null) {
            connection.stop(); // **เรียก stop โดยตรง**
            if (!silent) {
                String leaveMessage = "&cDisconnected from Twitch chat.";
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', leaveMessage));
                player.playSound(player.getLocation(), "block.note_block.bass", 1.0f, 1.0f);
            }
        } else {
            if (!silent) {
                player.sendMessage(ChatColor.RED + "You are not connected to a Twitch live chat.");
                player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            }
        }
    }

    public void removeConnection(UUID playerUUID) {
        playerConnections.remove(playerUUID);
    }

    // --- ส่วนที่แก้ไข: ทำให้การ stopAll เป็นแบบ Synchronous ---
    public void stopAll() {
        if (playerConnections.isEmpty()) return;
        plugin.getLogger().info("Stopping all active Twitch connections...");
        // ไม่ต้องใช้ runAsyncTask ที่นี่แล้ว
        for (TwitchConnection connection : playerConnections.values()) {
            connection.stop();
        }
        playerConnections.clear();
        plugin.getLogger().info("All Twitch connections have been stopped.");
    }
    // --- จบส่วนที่แก้ไข ---


    public boolean isRunning(Player player) {
        if (player == null) return false;
        return playerConnections.containsKey(player.getUniqueId());
    }

    public NamLivechat getPlugin() {
        return plugin;
    }
}