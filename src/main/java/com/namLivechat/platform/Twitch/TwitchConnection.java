package com.namLivechat.platform.Twitch;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.chat.events.channel.GiftSubscriptionsEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.StreamList;
import com.namLivechat.NamLivechat;
import com.namLivechat.service.AlertService;
import com.namLivechat.service.MessageHandler;
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
    private final AlertService alertService;
    private final MessageHandler messageHandler;
    private final boolean isFolia;

    private TwitchClient client;
    private TwitchStreamMonitor streamMonitor;
    private TwitchFollowerMonitor followerMonitor;
    private final String oauthToken;
    private boolean isStopping = false;

    public TwitchConnection(NamLivechat plugin, Player player, String channelName) {
        this.plugin = plugin;
        this.player = player;
        this.channelName = channelName;
        this.alertService = plugin.getAlertService();
        this.messageHandler = plugin.getMessageHandler();
        this.isFolia = plugin.isFolia();
        this.oauthToken = plugin.getTwitchConfig().getString("oauth-token");
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

                Stream streamInfo = getStream();
                if (streamInfo == null) {
                    runOnPlayerThread(player, () -> {
                        messageHandler.sendFormattedMessage(player, "twitch_not_live", "%channel%", channelName);
                        player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                    });
                    cleanupConnection();
                    return;
                }

                String broadcasterId = streamInfo.getUserId();

                client.getChat().joinChannel(channelName.toLowerCase());
                registerEventListeners();

                runOnPlayerThread(player, () -> {
                    String streamTitle = streamInfo.getTitle();
                    String streamerName = streamInfo.getUserName();
                    messageHandler.sendFormattedMessage(player, "connect_success_twitch",
                            "%title%", streamTitle,
                            "%channel%", streamerName
                    );
                    player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.0f);
                });

                this.streamMonitor = new TwitchStreamMonitor(plugin, client, oauthToken, channelName, (unused) -> stopDueToStreamEnd(), isFolia);
                this.streamMonitor.start();

                if (plugin.getTwitchConfig().getBoolean("events.new-follower.enabled", true)) {
                    this.followerMonitor = new TwitchFollowerMonitor(plugin, client, oauthToken, broadcasterId, this::handleNewFollower, isFolia);
                    this.followerMonitor.start();
                }

            } catch (Exception e) {
                String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (errorMessage.contains("login authentication failed") || errorMessage.contains("invalid irc credentials")) {
                    runOnPlayerThread(player, () -> {
                        messageHandler.sendMessage(player, "twitch_invalid_token");
                        player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                    });
                } else {
                    plugin.logDebug("Unexpected error in TwitchConnection for " + player.getName() + ": " + e.getMessage());
                }
                cleanupConnection();
            }
        });
    }

    private void stopDueToStreamEnd() {
        if (isStopping) return;
        isStopping = true;

        runOnPlayerThread(player, () -> messageHandler.sendFormattedMessage(player, "disconnect_stream_ended_twitch", "%channel%", channelName));
        cleanupConnection();
    }

    public void stopManual() {
        if (isStopping) return;
        isStopping = true;

        cleanupConnection();
    }

    private void cleanupConnection() {
        runOnPlayerThread(player, () -> alertService.stopBossBar(player));

        if (streamMonitor != null) streamMonitor.stop();
        if (followerMonitor != null) followerMonitor.stop();
        if (client != null) client.close();

        plugin.getTwitchService().removeConnection(player.getUniqueId());
    }

    private void handleNewFollower(String followerName) {
        FileConfiguration config = plugin.getTwitchConfig();
        String eventType = "new-follower";
        String path = "events." + eventType;

        String messageTemplate = config.getString(path + ".message");
        if (messageTemplate == null) return;

        String finalMessage = messageTemplate.replace("%user%", followerName);
        broadcastChatMessage(finalMessage, path + ".sound");

        if (config.getBoolean(path + ".boss-bar.enabled", false)) {
            String bossBarTemplate = config.getString(path + ".boss-bar.message");
            if (bossBarTemplate != null) {
                String bossBarMessage = bossBarTemplate.replace("%user%", followerName);
                try {
                    BarColor color = BarColor.valueOf(config.getString(path + ".boss-bar.color", "WHITE").toUpperCase());
                    int duration = config.getInt(path + ".boss-bar.duration", 10);
                    alertService.showBossBarAlert(player, ChatColor.translateAlternateColorCodes('&', bossBarMessage), color, duration);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid Boss Bar color in twitch-config.yml for " + eventType);
                }
            }
        }
    }

    private void registerEventListeners() {
        FileConfiguration config = plugin.getTwitchConfig();
        TwitchMessageFormatter formatter = new TwitchMessageFormatter(config);
        TwitchChatListener listener = new TwitchChatListener(plugin, player.getUniqueId(), formatter);

        client.getEventManager().onEvent(ChannelMessageEvent.class, listener::onChannelMessage);

        if (!config.getBoolean("events.enabled", true)) return;

        client.getEventManager().onEvent(SubscriptionEvent.class, event -> {
            if (isStopping || !event.getChannel().getName().equalsIgnoreCase(channelName)) return;

            String eventType = null;
            if (event.getGifted() && event.getGiftedBy() != null && config.getBoolean("events.gift-subscription.enabled", true)) {
                eventType = "gift-subscription";
            } else if (!event.getGifted()) {
                if (event.getMonths() > 1 && config.getBoolean("events.resubscription.enabled", true)) {
                    eventType = "resubscription";
                } else if (event.getMonths() <= 1 && config.getBoolean("events.new-subscription.enabled", true)) {
                    eventType = "new-subscription";
                }
            }
            if (eventType == null) return;

            String path = "events." + eventType;
            String messageTemplate = config.getString(path + ".message");
            if (messageTemplate == null) return;

            String finalMessage = replacePlaceholders(messageTemplate, event, null);
            broadcastChatMessage(finalMessage, path + ".sound");

            if (config.getBoolean(path + ".boss-bar.enabled", false)) {
                String bossBarTemplate = config.getString(path + ".boss-bar.message");
                if (bossBarTemplate != null) {
                    String bossBarMessage = replacePlaceholders(bossBarTemplate, event, null);

                    try {
                        BarColor color = BarColor.valueOf(config.getString(path + ".boss-bar.color", "WHITE").toUpperCase());
                        int duration = config.getInt(path + ".boss-bar.duration", 10);
                        alertService.showBossBarAlert(player, ChatColor.translateAlternateColorCodes('&', bossBarMessage), color, duration);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid Boss Bar color in twitch-config.yml for " + eventType);
                    }
                }
            }
        });

        if (config.getBoolean("events.community-subscription.enabled", true)) {
            client.getEventManager().onEvent(GiftSubscriptionsEvent.class, event -> {
                if (isStopping || !event.getChannel().getName().equalsIgnoreCase(channelName)) return;

                String eventType = "community-subscription";
                String path = "events." + eventType;
                String messageTemplate = config.getString(path + ".message");
                if (messageTemplate == null) return;

                String finalMessage = replacePlaceholders(messageTemplate, null, event);
                broadcastChatMessage(finalMessage, path + ".sound");

                if (config.getBoolean(path + ".boss-bar.enabled", false)) {
                    String bossBarTemplate = config.getString(path + ".boss-bar.message");
                    if (bossBarTemplate != null) {
                        String bossBarMessage = replacePlaceholders(bossBarTemplate, null, event);
                        try {
                            BarColor color = BarColor.valueOf(config.getString(path + ".boss-bar.color", "WHITE").toUpperCase());
                            int duration = config.getInt(path + ".boss-bar.duration", 10);
                            alertService.showBossBarAlert(player, ChatColor.translateAlternateColorCodes('&', bossBarMessage), color, duration);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid Boss Bar color in twitch-config.yml for " + eventType);
                        }
                    }
                }
            });
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
            plugin.getLogger().warning("An error occurred while trying to play sound '" + soundName + "'.");
        }
    }

    public Stream getStream() throws Exception {
        try {
            StreamList resultList = client.getHelix().getStreams(oauthToken, null, null, 1, null, null, null, Collections.singletonList(channelName)).execute();
            return resultList.getStreams().isEmpty() ? null : resultList.getStreams().get(0);
        } catch (Exception e) {
            throw e;
        }
    }

    private void runOnPlayerThread(Player player, Runnable runnable) {
        if (plugin.isDisabling() || player == null || !player.isOnline()) return;
        if (isFolia) {
            player.getScheduler().run(plugin, task -> runnable.run(), null);
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