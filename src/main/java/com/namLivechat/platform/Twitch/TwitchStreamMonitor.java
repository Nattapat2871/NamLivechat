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
    private final Consumer<Void> onStreamEnd;
    private final boolean isFolia;
    private Object task;

    public TwitchStreamMonitor(NamLivechat plugin, TwitchClient client, String oauthToken, String channelName, Consumer<Void> onStreamEnd, boolean isFolia) {
        this.plugin = plugin;
        this.client = client;
        this.oauthToken = oauthToken;
        this.channelName = channelName;
        this.onStreamEnd = onStreamEnd;
        this.isFolia = isFolia;
    }

    public void start() {
        Runnable checkTask = () -> {
            if (plugin.isDisabling()) {
                stop();
                return;
            }
            // --- ส่วนที่แก้ไข: เปลี่ยนมาเรียกใช้ isStreamLive() ที่นี่แทน ---
            if (!isStreamLive()) {
                onStreamEnd.accept(null);
                stop();
            }
        };

        if (isFolia) {
            this.task = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (t) -> checkTask.run(), 60, 60, java.util.concurrent.TimeUnit.SECONDS);
        } else {
            this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, checkTask, 1200L, 1200L); // 60 seconds
        }
    }

    // --- เพิ่มเมธอด isStreamLive() กลับเข้ามาสำหรับ Monitor ---
    private boolean isStreamLive() {
        try {
            StreamList resultList = client.getHelix().getStreams(oauthToken, null, null, 1, null, null, null, Collections.singletonList(channelName)).execute();
            return !resultList.getStreams().isEmpty();
        } catch (Exception e) {
            plugin.logDebug("Twitch stream monitor check failed: " + e.getMessage());
            return false; // ถ้าเช็คไม่สำเร็จ ให้ถือว่าสตรีมจบแล้ว
        }
    }

    public void stop() {
        if (task != null) {
            if (task instanceof io.papermc.paper.threadedregions.scheduler.ScheduledTask) {
                ((io.papermc.paper.threadedregions.scheduler.ScheduledTask) task).cancel();
            } else if (task instanceof BukkitTask) {
                ((BukkitTask) task).cancel();
            }
            task = null;
        }
    }
}