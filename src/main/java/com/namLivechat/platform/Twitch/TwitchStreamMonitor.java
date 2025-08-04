package com.namLivechat.platform.Twitch;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.helix.domain.StreamList;
import com.namLivechat.NamLivechat;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.function.Consumer;

public class TwitchStreamMonitor {

    private final NamLivechat plugin;
    private final TwitchClient client;
    private final String oauthToken;
    private final String channelName;
    private final Consumer<Void> onStreamEnd; // Callback ที่จะถูกเรียกเมื่อไลฟ์จบ
    private Object task;
    private final boolean isFolia;

    public TwitchStreamMonitor(NamLivechat plugin, TwitchClient client, String oauthToken, String channelName, Consumer<Void> onStreamEnd, boolean isFolia) {
        this.plugin = plugin;
        this.client = client;
        this.oauthToken = oauthToken;
        this.channelName = channelName;
        this.onStreamEnd = onStreamEnd;
        this.isFolia = isFolia;
    }

    public void start() {
        Runnable taskLogic = () -> {
            if (!isStreamLive()) {
                plugin.getLogger().info("Stream for channel " + channelName + " has ended. Stopping connection.");
                onStreamEnd.accept(null); // เรียก callback เพื่อแจ้งว่าไลฟ์จบแล้ว
                stop(); // หยุดการทำงานของตัวเอง
            }
        };

        if (isFolia) {
            this.task = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, t -> taskLogic.run(), 2, 2, java.util.concurrent.TimeUnit.MINUTES);
        } else {
            this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, taskLogic, 20L * 120, 20L * 120);
        }
    }

    public void stop() {
        if (task == null) return;
        try {
            if (task instanceof io.papermc.paper.threadedregions.scheduler.ScheduledTask) {
                ((io.papermc.paper.threadedregions.scheduler.ScheduledTask) task).cancel();
            } else if (task instanceof BukkitTask) {
                ((BukkitTask) task).cancel();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to cancel a Twitch monitor task.");
        }
        task = null;
    }

    private boolean isStreamLive() {
        try {
            StreamList resultList = client.getHelix().getStreams(oauthToken, null, null, 1, null, null, null, Collections.singletonList(channelName)).execute();
            return !resultList.getStreams().isEmpty();
        } catch (Exception e) {
            plugin.getLogger().warning("Could not check stream status for " + channelName + ": " + e.getMessage());
            return false; // ถ้าเช็คไม่ได้ ให้ถือว่าออฟไลน์
        }
    }
}