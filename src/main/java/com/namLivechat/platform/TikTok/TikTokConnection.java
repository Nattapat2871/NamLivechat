package com.namLivechat.platform.TikTok;

import com.namLivechat.NamLivechat;
import com.namLivechat.service.AlertService;
import io.github.jwdeveloper.tiktok.TikTokLive;
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
    private final boolean isFolia;
    private LiveClient liveClient;

    public TikTokConnection(NamLivechat plugin, Player player, String username, AlertService alertService, boolean isFolia) {
        this.plugin = plugin;
        this.player = player;
        this.username = username.replace("@", "");
        this.alertService = alertService;
        this.isFolia = isFolia;
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
                            String format = plugin.getTiktokConfig().getString("message-format", "&b[TikTok] &f%user%&7: &f%message%");
                            String rawMessage = format.replace("%user%", coloredAuthor).replace("%message%", message);

                            // --- ส่วนที่แก้ไข: แปลงสีทั้งหมดในครั้งเดียวตอนท้าย ---
                            String finalMessage = ChatColor.translateAlternateColorCodes('&', rawMessage);
                            runOnPlayerThread(() -> player.sendMessage(finalMessage));
                        })
                        .onGift((client, event) -> handleAlert("gift", event.getUser(), event.getGift().getName(), String.valueOf(event.getCombo()), String.valueOf(event.getGift().getDiamondCost())))
                        .onFollow((client, event) -> handleAlert("follow", event.getUser(), null, null, null))
                        .onConnected((client, event) -> {
                            String liveTitle = client.getRoomInfo().getTitle();
                            String channelName = client.getRoomInfo().getHost().getProfileName();
                            // --- ส่วนที่แก้ไข: เปลี่ยนรูปแบบข้อความ ---
                            String connectingMessage = String.format("&aSucessfully connected to &f%s &a@%s tiktok live chat.", liveTitle, channelName);

                            runOnPlayerThread(() -> {
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', connectingMessage));
                                player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.0f);
                            });
                        })
                        .onLiveEnded((client, event) -> {
                            runOnPlayerThread(() -> player.sendMessage(ChatColor.YELLOW + "The stream for @" + username + " has ended."));
                            stop();
                        })
                        .onError((client, event) -> {
                            String exceptionMsg = event.getException().getMessage().toLowerCase();
                            String userMessage;
                            if (exceptionMsg.contains("user not found")) {
                                userMessage = "Could not find a TikTok user named '" + username + "'.";
                            } else if (exceptionMsg.contains("user is offline") || exceptionMsg.contains("stream is offline")) {
                                userMessage = "The user @" + username + " is not currently live.";
                            } else {
                                userMessage = "Could not connect to TikTok. Please try again later.";
                            }
                            runOnPlayerThread(() -> {
                                player.sendMessage(ChatColor.RED + userMessage);
                                player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                            });
                            plugin.getTiktokService().removeConnection(player.getUniqueId());
                        })
                        .build();

                liveClient.connect();

            } catch (Exception e) {
                plugin.getLogger().severe("An unexpected error occurred in TikTokConnection for " + player.getName() + ": " + e.getMessage());
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

    private void handleAlert(String type, User user, String giftName, String amount, String totalValue) {
        FileConfiguration config = plugin.getTiktokConfig();
        String path = "events." + type;
        if (!config.getBoolean(path + ".enabled", true)) return;

        String userDisplayName = user.getProfileName();

        String message = config.getString(path + ".message", "");
        message = replacePlaceholders(message, userDisplayName, giftName, amount, totalValue);
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
            bossBarMessage = replacePlaceholders(bossBarMessage, userDisplayName, giftName, amount, totalValue);
            try {
                BarColor color = BarColor.valueOf(config.getString(path + ".boss-bar.color", "WHITE").toUpperCase());
                int duration = config.getInt(path + ".boss-bar.duration", 10);
                alertService.showBossBarAlert(player, ChatColor.translateAlternateColorCodes('&', bossBarMessage), color, duration);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid Boss Bar color in tiktok-config.yml for " + type);
            }
        }
    }

    private String getAuthorColor(User user) {
        FileConfiguration config = plugin.getTiktokConfig();
        if (user.isModerator()) return config.getString("role-colors.moderator", "&9");
        if (user.isSubscriber()) return config.getString("role-colors.subscriber", "&a");
        return config.getString("role-colors.default", "&7");
    }

    private String replacePlaceholders(String template, String user, String giftName, String amount, String totalValue) {
        return template
                .replace("%user%", user)
                .replace("%gift_name%", giftName != null ? giftName : "")
                .replace("%amount%", amount != null ? amount : "")
                .replace("%total_value%", totalValue != null ? totalValue : "");
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