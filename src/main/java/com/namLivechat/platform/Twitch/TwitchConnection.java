package com.namLivechat.platform.Twitch;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.github.twitch4j.chat.events.channel.GiftSubscriptionsEvent;
import com.github.twitch4j.helix.domain.StreamList;
import com.namLivechat.NamLivechat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
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

    public TwitchConnection(NamLivechat plugin, Player player, String channelName, String oauthToken, boolean isFolia) {
        this.plugin = plugin;
        this.player = player;
        this.channelName = channelName;
        this.oauthToken = oauthToken;
        this.isFolia = isFolia;
    }

    public void start() {
        runAsyncTask(() -> {
            try {
                OAuth2Credential credential = new OAuth2Credential("twitch", oauthToken);
                this.client = TwitchClientBuilder.builder()
                        .withEnableHelix(true)
                        .withEnableChat(true)
                        .withChatAccount(credential)
                        .build();

                if (!isStreamLive()) {
                    runPlayerTask(() -> {
                        player.sendMessage(ChatColor.RED + "Channel '" + channelName + "' is not currently live.");
                        player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                    });
                    plugin.getTwitchService().removeConnection(player.getUniqueId());
                    stop();
                    return;
                }

                client.getChat().joinChannel(channelName.toLowerCase());

                registerEventListeners();

                runPlayerTask(() -> {
                    String joinMessage = "&aSuccessfully connected to " + channelName + "'s chat.";
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', joinMessage));
                    player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.0f);
                });

                this.monitor = new TwitchStreamMonitor(plugin, client, oauthToken, channelName, (unused) -> {
                    runPlayerTask(() -> {
                        player.sendMessage(ChatColor.YELLOW + "[NamLivechat] The stream for " + channelName + " has ended.");
                        plugin.getTwitchService().stop(player, true);
                    });
                }, isFolia);
                this.monitor.start();

            } catch (Exception e) {
                runPlayerTask(() -> {
                    player.sendMessage(ChatColor.RED + "Failed to connect to Twitch.");
                    if (e.getMessage() != null && e.getMessage().contains("Login authentication failed")) {
                        player.sendMessage(ChatColor.RED + "Reason: Login authentication failed. Your OAuth token might be expired or incorrect.");
                    }
                    player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                });
                plugin.getLogger().severe("[NamLivechat] Failed to start Twitch connection for " + player.getName() + ": " + e.getMessage());
                plugin.getTwitchService().removeConnection(player.getUniqueId());
            }
        });
    }

    private void registerEventListeners() {
        FileConfiguration config = plugin.getTwitchConfig();
        if (!config.getBoolean("events.enabled", true)) {
            return;
        }

        TwitchMessageFormatter formatter = new TwitchMessageFormatter(config);
        TwitchChatListener listener = new TwitchChatListener(plugin, player.getUniqueId(), formatter);

        client.getEventManager().onEvent(ChannelMessageEvent.class, listener::onChannelMessage);

        // Subscription (new and resub)
        if (config.getBoolean("events.new-subscription.enabled", true) || config.getBoolean("events.resubscription.enabled", true)) {
            client.getEventManager().onEvent(SubscriptionEvent.class, event -> {
                if (!event.getChannel().getName().equalsIgnoreCase(channelName)) return;
                // ไม่มี getStreakMonths() ใน event นี้ (เวอร์ชันใหม่)
                // จะเช็กแค่ months มากกว่า 1 ว่าเป็น resub
                boolean isResub = event.getMonths() > 1;
                if (isResub && config.getBoolean("events.resubscription.enabled", true)) {
                    String message = config.getString("events.resubscription.message", "&d[Twitch] &f%user% &ehas resubscribed for %months% months!")
                            .replace("%user%", event.getUser().getName())
                            .replace("%months%", String.valueOf(event.getMonths()))
                            .replace("%tier%", event.getSubscriptionPlan());

                    broadcastEventMessage(message, "events.resubscription.sound");
                } else if (!isResub && config.getBoolean("events.new-subscription.enabled", true)) {
                    String message = config.getString("events.new-subscription.message", "&d[Twitch] &f%user% &ehas just subscribed at Tier %tier%!")
                            .replace("%user%", event.getUser().getName())
                            .replace("%tier%", event.getSubscriptionPlan());

                    broadcastEventMessage(message, "events.new-subscription.sound");
                }
            });
        }

        // Gift Subscription (single)
        if (config.getBoolean("events.gift-subscription.enabled", true)) {
            client.getEventManager().onEvent(GiftSubscriptionsEvent.class, event -> {
                if (!event.getChannel().getName().equalsIgnoreCase(channelName)) return;
                String message = config.getString("events.gift-subscription.message", "&d[Twitch] &f%gifter% &ehas gifted a Tier %tier% sub to &f%recipient%&e!")
                        .replace("%gifter%", event.getGifter().getName())
                        .replace("%recipient%", event.getRecipient().getName())
                        .replace("%months%", String.valueOf(event.getMonths()))
                        .replace("%tier%", event.getSubscriptionPlan());

                broadcastEventMessage(message, "events.gift-subscription.sound");
            });
        }

        // Community Gift Subscription (multiple at once)
        if (config.getBoolean("events.community-subscription.enabled", true)) {
            client.getEventManager().onEvent(GiftSubscriptionsEvent.class, event -> {
                if (!event.getChannel().getName().equalsIgnoreCase(channelName)) return;
                // ไม่มี getGifter(), getRecipient(), getMonths() ใน event นี้ (เวอร์ชันใหม่)
                String message = config.getString("events.community-subscription.message", "&d[Twitch] &f%gifter% &eis gifting %count% Tier %tier% subs to the community!")
                        .replace("%gifter%", event.getUser().getName())
                        .replace("%count%", String.valueOf(event.getCount()))
                        .replace("%tier%", event.getSubscriptionPlan());

                broadcastEventMessage(message, "events.community-subscription.sound");
            });
        }
    }

    private void broadcastEventMessage(String message, String soundPath) {
        runPlayerTask(() -> {
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
            Sound sound = Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
            float volume = (float) soundSection.getDouble("volume", 1.0);
            float pitch = (float) soundSection.getDouble("pitch", 1.0);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound name in twitch-config.yml at path '" + configPath + "': " + soundName);
        }
    }

    public void stop() {
        if (monitor != null) {
            monitor.stop();
            monitor = null;
        }
        if (client != null) {
            try {
                if (client.getChat().isChannelJoined(channelName)) {
                    client.getChat().leaveChannel(channelName);
                }
                client.close();
            } catch (Exception e) {
                plugin.getLogger().warning("An error occurred while closing a Twitch connection: " + e.getMessage());
            }
            client = null;
        }
    }

    private boolean isStreamLive() {
        try {
            StreamList resultList = client.getHelix().getStreams(oauthToken, null, null, 1, null, null, null, Collections.singletonList(channelName)).execute();
            return !resultList.getStreams().isEmpty();
        } catch (Exception e) {
            plugin.getLogger().warning("Could not check stream status for " + channelName + ": " + e.getMessage());
            return false;
        }
    }

    private void runPlayerTask(Runnable runnable) {
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
