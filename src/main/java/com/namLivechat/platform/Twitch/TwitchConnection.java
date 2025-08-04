package com.namLivechat.platform.Twitch;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.chat.events.channel.GiftSubscriptionsEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.github.twitch4j.helix.domain.StreamList;
import com.namLivechat.NamLivechat;
import com.namLivechat.service.AlertService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Locale;

public class TwitchConnection {

    private final NamLivechat plugin;
    private final Player player;
    private final String channelName;
    private final String oauthToken;
    private TwitchClient client;
    private TwitchStreamMonitor monitor;
    private final boolean isFolia;
    private final AlertService alertService;
    private boolean isStopping = false;

    public TwitchConnection(NamLivechat plugin, Player player, String channelName, String oauthToken, boolean isFolia, AlertService alertService) {
        this.plugin = plugin;
        this.player = player;
        this.channelName = channelName;
        this.oauthToken = oauthToken;
        this.isFolia = isFolia;
        this.alertService = alertService;
    }

    public void start() {
        runAsyncTask(() -> {
            try {
                this.client = TwitchClientBuilder.builder()
                        .withEnableHelix(true)
                        .withEnableChat(true)
                        .withChatAccount(new OAuth2Credential("twitch", oauthToken))
                        .withChatCommandsViaHelix(false)
                        .withDefaultAuthToken(new OAuth2Credential("twitch", oauthToken))
                        .build();

                plugin.getLogger().info("Checking stream status for '" + channelName + "'...");
                if (!isStreamLive()) {
                    runOnPlayerThread(player, () -> {
                        player.sendMessage(ChatColor.RED + "Channel '" + channelName + "' is not currently live.");
                        player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                    });
                    cleanupConnection();
                    return;
                }

                plugin.getLogger().info("Connecting to Twitch IRC for channel '" + channelName + "'...");
                client.getChat().joinChannel(channelName.toLowerCase());
                registerEventListeners();

                runOnPlayerThread(player, () -> {
                    String joinMessage = "&aSuccessfully connected to " + channelName + "'s chat.";
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', joinMessage));
                    player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.0f);
                });

                this.monitor = new TwitchStreamMonitor(plugin, client, oauthToken, channelName, (unused) -> {
                    stopDueToStreamEnd();
                }, isFolia);
                this.monitor.start();

            } catch (Exception e) {
                String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (errorMessage.contains("login authentication failed") || errorMessage.contains("invalid irc credentials")) {
                    runOnPlayerThread(player, () -> {
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[NamLivechat] Twitch connection failed!");
                        player.sendMessage(ChatColor.GRAY + "Reason: The OAuth Token is invalid or has expired.");
                        player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                    });
                    plugin.getLogger().severe("Failed to start Twitch connection: Invalid OAuth Token.");
                } else {
                    runOnPlayerThread(player, () -> {
                        player.sendMessage(ChatColor.RED + "Could not connect to Twitch. The channel might be offline or the API is temporarily down.");
                        player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                    });
                    plugin.getLogger().severe("Unexpected error in TwitchConnection for " + player.getName() + ": " + e.getMessage());
                }
                cleanupConnection();
            }
        });
    }

    private void stopDueToStreamEnd() {
        if (isStopping) return;
        isStopping = true;

        runOnPlayerThread(player, () -> {
            String message = String.format("&aDisconnected from &5Twitch &f%s&a's live chat.", channelName);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        });

        cleanupConnection();
    }

    public void stopManual() {
        if (isStopping) return;
        isStopping = true;

        cleanupConnection();
    }

    private void cleanupConnection() {
        runOnPlayerThread(player, () -> alertService.stopBossBar(player));

        if (monitor != null) {
            monitor.stop();
            monitor = null;
        }

        runAsyncTask(() -> {
            if (client != null) {
                try {
                    plugin.getLogger().info("Disconnecting from Twitch chat for " + player.getName());
                    if (client.getChat().isChannelJoined(channelName)) client.getChat().leaveChannel(channelName);
                    client.close();
                } catch (Exception e) { /* Ignore */ }
                client = null;
            }
        });
        plugin.getTwitchService().removeConnection(player.getUniqueId());
    }

    private void registerEventListeners() {
        FileConfiguration config = plugin.getTwitchConfig();
        TwitchMessageFormatter formatter = new TwitchMessageFormatter(config);
        TwitchChatListener listener = new TwitchChatListener(plugin, player.getUniqueId(), formatter);

        client.getEventManager().onEvent(ChannelMessageEvent.class, listener::onChannelMessage);

        if (!config.getBoolean("events.enabled", true)) return;

        client.getEventManager().onEvent(SubscriptionEvent.class, event -> {
            if (!event.getChannel().getName().equalsIgnoreCase(channelName)) return;
            if (isStopping) return;

            if (event.getGifted() && event.getGiftedBy() != null && config.getBoolean("events.gift-subscription.enabled", true)) {
                handleEvent("gift-subscription", event, null);
            } else if (!event.getGifted()) {
                if (event.getMonths() > 1 && config.getBoolean("events.resubscription.enabled", true)) {
                    handleEvent("resubscription", event, null);
                } else if (event.getMonths() <= 1 && config.getBoolean("events.new-subscription.enabled", true)) {
                    handleEvent("new-subscription", event, null);
                }
            }
        });

        if (config.getBoolean("events.community-subscription.enabled", true)) {
            client.getEventManager().onEvent(GiftSubscriptionsEvent.class, event -> {
                if (!event.getChannel().getName().equalsIgnoreCase(channelName)) return;
                if (isStopping) return;
                handleEvent("community-subscription", null, event);
            });
        }
    }

    private void handleEvent(String eventType, SubscriptionEvent subEvent, GiftSubscriptionsEvent giftEvent) {
        FileConfiguration config = plugin.getTwitchConfig();
        String path = "events." + eventType;
        String messageTemplate = config.getString(path + ".message");
        if (messageTemplate == null) return;

        String finalMessage = replacePlaceholders(messageTemplate, subEvent, giftEvent);
        broadcastChatMessage(finalMessage, path + ".sound");

        if (config.getBoolean(path + ".boss-bar.enabled", false)) {
            String bossBarTemplate = config.getString(path + ".boss-bar.message");
            if (bossBarTemplate != null) {
                String bossBarMessage = replacePlaceholders(bossBarTemplate, subEvent, giftEvent);
                try {
                    BarColor color = BarColor.valueOf(config.getString(path + ".boss-bar.color", "WHITE").toUpperCase());
                    int duration = config.getInt(path + ".boss-bar.duration", 10);
                    alertService.queueBossBar(player, ChatColor.translateAlternateColorCodes('&', bossBarMessage), color, duration);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid Boss Bar color in twitch-config.yml for " + eventType);
                }
            }
        }
    }

    private String replacePlaceholders(String template, SubscriptionEvent subEvent, GiftSubscriptionsEvent giftEvent) {
        if (subEvent != null) {
            return template
                    .replace("%user%", subEvent.getUser().getName())
                    .replace("%months%", String.valueOf(subEvent.getMonths()))
                    .replace("%streak%", subEvent.getSubStreak() != null ? String.valueOf(subEvent.getSubStreak()) : "1")
                    .replace("%tier%", subEvent.getSubscriptionPlan())
                    .replace("%gifter%", subEvent.getGiftedBy() != null ? subEvent.getGiftedBy().getName() : "")
                    .replace("%recipient%", subEvent.getUser().getName());
        } else if (giftEvent != null) {
            return template
                    .replace("%gifter%", giftEvent.getUser().getName())
                    .replace("%count%", String.valueOf(giftEvent.getCount()))
                    .replace("%tier%", giftEvent.getSubscriptionPlan());
        }
        return template;
    }

    private void broadcastChatMessage(String message, String soundPath) {
        runOnPlayerThread(player, () -> {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            playSoundFromConfig(soundPath);
        });
    }

    private void playSoundFromConfig(String configPath) {
        ConfigurationSection soundSection = plugin.getTwitchConfig().getConfigurationSection(configPath);
        if (soundSection == null) return;

        String soundName = soundSection.getString("name", "");
        if (soundName.isEmpty()) return;

        try {
            player.playSound(player.getLocation(), soundName.toLowerCase(Locale.ROOT), (float) soundSection.getDouble("volume", 1.0), (float) soundSection.getDouble("pitch", 1.0));
        } catch (Exception e) {
            plugin.getLogger().warning("An error occurred while trying to play sound '" + soundName + "'. Please ensure it's a valid sound key.");
        }
    }

    private boolean isStreamLive() throws Exception {
        try {
            StreamList resultList = client.getHelix().getStreams(oauthToken, null, null, 1, null, null, null, Collections.singletonList(channelName)).execute();
            return !resultList.getStreams().isEmpty();
        } catch (Exception e) {
            throw e;
        }
    }

    private void runOnPlayerThread(Player player, Runnable runnable) {
        if (player == null || !player.isOnline()) return;
        if (isFolia) {
            player.getScheduler().run(plugin, task -> runnable.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    private void runAsyncTask(Runnable runnable) {
        if (isFolia) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> runnable.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }
}