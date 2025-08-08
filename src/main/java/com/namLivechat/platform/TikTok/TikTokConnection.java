package com.namLivechat.platform.TikTok;

import com.namLivechat.NamLivechat;
import com.namLivechat.service.AlertService;
import com.namLivechat.service.MessageHandler;
import io.github.jwdeveloper.tiktok.TikTokLive;
import io.github.jwdeveloper.tiktok.data.models.gifts.Gift;
import io.github.jwdeveloper.tiktok.data.models.users.User;
import io.github.jwdeveloper.tiktok.live.LiveClient;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Locale;

public class TikTokConnection {

    private final NamLivechat plugin;
    private final Player player;
    private final String username;
    private final AlertService alertService;
    private final MessageHandler messageHandler;
    private final boolean isFolia;
    private LiveClient liveClient;

    public TikTokConnection(NamLivechat plugin, Player player, String username) {
        this.plugin = plugin;
        this.player = player;
        this.username = username;
        this.alertService = plugin.getAlertService();
        this.messageHandler = plugin.getMessageHandler();
        this.isFolia = plugin.isFolia();
    }

    public void start() {
        runAsyncTask(() -> {
            try {
                this.liveClient = TikTokLive.newClient(username)
                        .onComment((client, event) -> {
                            String authorName = event.getUser().getProfileName();
                            String authorColor = getAuthorColor(event.getUser());
                            String coloredAuthor = authorColor + authorName;

                            String message = event.getText();
                            String format = plugin.getTiktokConfig().getString("message-format");
                            String rawMessage = format.replace("%user%", coloredAuthor).replace("%message%", message);

                            String finalMessage = ChatColor.translateAlternateColorCodes('&', rawMessage);
                            runOnPlayerThread(() -> player.sendMessage(finalMessage));
                        })
                        // --- ส่วนที่แก้ไข: ย้าย Logic การจัดการ Alert มาไว้ที่นี่โดยตรง ---
                        .onGift((client, event) -> {
                            FileConfiguration config = plugin.getTiktokConfig();
                            String path = "events.gift";
                            if (!config.getBoolean(path + ".enabled", true)) return;

                            User user = event.getUser();
                            Gift gift = event.getGift();

                            String message = config.getString(path + ".message", "")
                                    .replace("%user%", user.getProfileName())
                                    .replace("%gift_name%", gift.getName())
                                    .replace("%amount%", String.valueOf(event.getCombo()))
                                    .replace("%total_value%", String.valueOf(gift.getDiamondCost()));

                            broadcastAlert(message, path);

                            if (config.getBoolean(path + ".boss-bar.enabled", false)) {
                                String bossBarMessage = config.getString(path + ".boss-bar.message", "")
                                        .replace("%user%", user.getProfileName())
                                        .replace("%gift_name%", gift.getName())
                                        .replace("%amount%", String.valueOf(event.getCombo()))
                                        .replace("%total_value%", String.valueOf(gift.getDiamondCost()));

                                showBossBar(bossBarMessage, path);
                            }
                        })
                        .onFollow((client, event) -> {
                            FileConfiguration config = plugin.getTiktokConfig();
                            String path = "events.follow";
                            if (!config.getBoolean(path + ".enabled", true)) return;

                            User user = event.getUser();
                            String message = config.getString(path + ".message", "").replace("%user%", user.getProfileName());
                            broadcastAlert(message, path);

                            if (config.getBoolean(path + ".boss-bar.enabled", false)) {
                                String bossBarMessage = config.getString(path + ".boss-bar.message", "").replace("%user%", user.getProfileName());
                                showBossBar(bossBarMessage, path);
                            }
                        })
                        // ------------------------------------------
                        .onConnected((client, event) -> {
                            String liveTitle = client.getRoomInfo().getTitle();
                            String channelName = client.getRoomInfo().getHost().getProfileName();
                            runOnPlayerThread(() -> {
                                messageHandler.sendFormattedMessage(player, "connect_success_tiktok",
                                        "%title%", liveTitle,
                                        "%user%", channelName
                                );
                                player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.0f);
                            });
                        })
                        .onLiveEnded((client, event) -> {
                            runOnPlayerThread(() -> messageHandler.sendFormattedMessage(player, "disconnect_stream_ended_tiktok", "%user%", username));
                            stop();
                        })
                        .onError((client, event) -> {
                            String exceptionMsg = event.getException().getMessage().toLowerCase();
                            if (exceptionMsg.contains("user not found")) {
                                runOnPlayerThread(() -> messageHandler.sendFormattedMessage(player, "tiktok_user_not_found", "%user%", username));
                            } else if (exceptionMsg.contains("user is offline") || exceptionMsg.contains("stream is offline")) {
                                runOnPlayerThread(() -> messageHandler.sendFormattedMessage(player, "tiktok_user_offline", "%user%", username));
                            } else {
                                runOnPlayerThread(() -> messageHandler.sendMessage(player, "tiktok_connect_fail"));
                            }
                            plugin.getTiktokService().removeConnection(player.getUniqueId());
                        })
                        .build();

                liveClient.connect();

            } catch (Exception e) {
                plugin.logDebug("An unexpected error occurred in TikTokConnection for " + player.getName() + ": " + e.getMessage());
                plugin.getTiktokService().removeConnection(player.getUniqueId());
            }
        });
    }

    public void stop() {
        if (liveClient != null) {
            liveClient.disconnect();
        }
        alertService.stopBossBar(player);
        plugin.getTiktokService().removeConnection(player.getUniqueId());
    }

    private void broadcastAlert(String message, String configPath) {
        FileConfiguration config = plugin.getTiktokConfig();
        runOnPlayerThread(() -> {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            String soundName = config.getString(configPath + ".sound.name", "");
            if (!soundName.isEmpty()) {
                float volume = (float) config.getDouble(configPath + ".sound.volume", 1.0);
                float pitch = (float) config.getDouble(configPath + ".sound.pitch", 1.5);
                player.playSound(player.getLocation(), soundName.toLowerCase(Locale.ROOT), volume, pitch);
            }
        });
    }

    private void showBossBar(String message, String configPath) {
        FileConfiguration config = plugin.getTiktokConfig();
        try {
            BarColor color = BarColor.valueOf(config.getString(configPath + ".boss-bar.color", "WHITE").toUpperCase());
            int duration = config.getInt(configPath + ".boss-bar.duration", 10);
            alertService.showBossBarAlert(player, ChatColor.translateAlternateColorCodes('&', message), color, duration);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid Boss Bar color in tiktok-config.yml");
        }
    }

    private String getAuthorColor(User user) {
        FileConfiguration config = plugin.getTiktokConfig();
        if (user.isModerator()) return config.getString("role-colors.moderator", "&9");
        if (user.isSubscriber()) return config.getString("role-colors.subscriber", "&a");
        return config.getString("role-colors.default", "&7");
    }

    private void runOnPlayerThread(Runnable runnable) {
        if (plugin.isDisabling() || player == null || !player.isOnline()) return;
        if (isFolia) {
            player.getScheduler().run(plugin, (task) -> runnable.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    private void runAsyncTask(Runnable runnable) {
        if (plugin.isDisabling()) return;
        if (isFolia) {
            Bukkit.getAsyncScheduler().runNow(plugin, (task) -> runnable.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }
}