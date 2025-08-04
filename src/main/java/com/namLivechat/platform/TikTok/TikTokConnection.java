package com.namLivechat.platform.TikTok;

import com.namLivechat.NamLivechat;
import com.namLivechat.service.AlertService;
import io.github.jwdeveloper.tiktok.TikTokLive;
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
    private final boolean isFolia;
    private LiveClient liveClient;

    public TikTokConnection(NamLivechat plugin, Player player, String username, AlertService alertService, boolean isFolia) {
        this.plugin = plugin;
        this.player = player;
        this.username = username;
        this.alertService = alertService;
        this.isFolia = isFolia;
    }

    public void start() {
        runAsyncTask(() -> {
            try {
                plugin.getLogger().info("Attempting to connect to TikTok user: @" + username);

                this.liveClient = TikTokLive.newClient(username)
                        .onComment((client, event) -> {
                            String author = event.getUser().getProfileName();
                            String message = event.getText();
                            String format = plugin.getTiktokConfig().getString("message-format", "&b[TikTok] &f%user%&7: &f%message%");
                            String formattedMessage = format.replace("%user%", author).replace("%message%", message);
                            runOnPlayerThread(() -> player.sendMessage(ChatColor.translateAlternateColorCodes('&', formattedMessage)));
                        })
                        .onGift((client, event) -> {
                            handleAlert("gift", event.getUser().getProfileName(), event.getGift().getName(), String.valueOf(event.getCombo()), String.valueOf(event.getGift().getDiamondCost()));
                        })
                        .onFollow((client, event) -> {
                            handleAlert("follow", event.getUser().getProfileName(), null, null, null);
                        })
                        .onConnected((client, event) -> {
                            runOnPlayerThread(() -> {
                                player.sendMessage(ChatColor.GREEN + "Successfully connected to @" + username + "'s TikTok chat.");
                                player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.0f);
                            });
                            plugin.getLogger().info("Successfully connected to TikTok user: @" + username);
                        })
                        .onDisconnected((client, event) -> {
                            runOnPlayerThread(() -> player.sendMessage(ChatColor.YELLOW + "Disconnected from @" + username + "'s TikTok chat."));
                            plugin.getLogger().info("Disconnected from TikTok user: @" + username);
                            plugin.getTiktokService().removeConnection(player.getUniqueId());
                        })
                        .onLiveEnded((client, event) -> {
                            runOnPlayerThread(() -> player.sendMessage(ChatColor.YELLOW + "The stream for @" + username + " has ended."));
                            plugin.getLogger().info("TikTok stream has ended for user: @" + username);
                            plugin.getTiktokService().removeConnection(player.getUniqueId());
                        })
                        .onError((client, event) -> {
                            runOnPlayerThread(() -> {
                                player.sendMessage(ChatColor.RED + "Could not connect to TikTok. The user might not be live or the username is incorrect.");
                                player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                            });
                            plugin.getLogger().severe("Failed to connect to TikTok user @" + username + ": " + event.getException().getMessage());
                            plugin.getTiktokService().removeConnection(player.getUniqueId());
                        })
                        .build();

                liveClient.connect();

            } catch (Exception e) {
                runOnPlayerThread(() -> {
                    player.sendMessage(ChatColor.RED + "An unexpected error occurred while trying to connect to TikTok.");
                    player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                });
                plugin.getLogger().severe("Unexpected error in TikTokConnection: " + e.getMessage());
                plugin.getTiktokService().removeConnection(player.getUniqueId());
            }
        });
    }

    public void stop() {
        // --- ส่วนที่แก้ไข: วิธีที่ถูกต้อง ---
        if (liveClient != null) {
            liveClient.disconnect();
        }
    }

    private void handleAlert(String type, String user, String giftName, String amount, String totalValue) {
        FileConfiguration config = plugin.getTiktokConfig();
        String path = "events." + type;

        if (!config.getBoolean(path + ".enabled", true)) return;

        String message = config.getString(path + ".message", "");
        message = message.replace("%user%", user)
                .replace("%gift_name%", giftName != null ? giftName : "")
                .replace("%amount%", amount != null ? amount : "")
                .replace("%total_value%", totalValue != null ? totalValue : "");

        final String finalMessage = message;
        runOnPlayerThread(() -> {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', finalMessage));
            String soundName = config.getString(path + ".sound.name", "");
            if (!soundName.isEmpty()) {
                float volume = (float) config.getDouble(path + ".sound.volume", 1.0);
                float pitch = (float) config.getDouble(path + ".sound.pitch", 1.5);
                player.playSound(player.getLocation(), soundName.toLowerCase(Locale.ROOT), volume, pitch);
            }
        });

        if (config.getBoolean(path + ".boss-bar.enabled", false)) {
            String bossBarMessage = config.getString(path + ".boss-bar.message", "");
            bossBarMessage = bossBarMessage.replace("%user%", user)
                    .replace("%gift_name%", giftName != null ? giftName : "")
                    .replace("%amount%", amount != null ? amount : "")
                    .replace("%total_value%", totalValue != null ? totalValue : "");

            try {
                BarColor color = BarColor.valueOf(config.getString(path + ".boss-bar.color", "WHITE").toUpperCase());
                int duration = config.getInt(path + ".boss-bar.duration", 10);
                alertService.queueBossBar(player, ChatColor.translateAlternateColorCodes('&', bossBarMessage), color, duration);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid Boss Bar color in tiktok-config.yml for " + type);
            }
        }
    }

    private void runOnPlayerThread(Runnable runnable) {
        if (player == null || !player.isOnline()) return;
        if (isFolia) {
            player.getScheduler().run(plugin, (task) -> runnable.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    private void runAsyncTask(Runnable runnable) {
        if (isFolia) {
            Bukkit.getAsyncScheduler().runNow(plugin, (task) -> runnable.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }
}