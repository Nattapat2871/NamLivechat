package com.namLivechat.platform.Twitch;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.helix.domain.Follow;
import com.github.twitch4j.helix.domain.FollowList;
import com.namLivechat.NamLivechat;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TwitchFollowerMonitor {

    private final NamLivechat plugin;
    private final TwitchClient client;
    private final String oauthToken;
    private final String broadcasterId;
    private final Consumer<String> onNewFollower;
    private final boolean isFolia;
    private Object task;
    private Set<String> knownFollowerIds;

    public TwitchFollowerMonitor(NamLivechat plugin, TwitchClient client, String oauthToken, String broadcasterId, Consumer<String> onNewFollower, boolean isFolia) {
        this.plugin = plugin;
        this.client = client;
        this.oauthToken = oauthToken;
        this.broadcasterId = broadcasterId;
        this.onNewFollower = onNewFollower;
        this.isFolia = isFolia;
        this.knownFollowerIds = new HashSet<>();
    }

    public void start() {
        Runnable checkTask = () -> {
            if (plugin.isDisabling()) {
                stop();
                return;
            }
            try {
                FollowList result = client.getHelix().getFollowers(oauthToken, null, broadcasterId, null, 100).execute();
                if (result == null || result.getFollows() == null) return;

                // --- ส่วนที่แก้ไข: เรียกใช้เมธอด getFromId() ที่ถูกต้อง ---
                List<String> currentFollowerIds = result.getFollows().stream()
                        .map(Follow::getFromId)
                        .collect(Collectors.toList());

                if (knownFollowerIds.isEmpty()) {
                    knownFollowerIds.addAll(currentFollowerIds);
                    return;
                }

                for (Follow follower : result.getFollows()) {
                    // --- ส่วนที่แก้ไข: เรียกใช้เมธอด getFromId() และ getFromName() ที่ถูกต้อง ---
                    if (!knownFollowerIds.contains(follower.getFromId())) {
                        onNewFollower.accept(follower.getFromName());
                        knownFollowerIds.add(follower.getFromId());
                    }
                }

            } catch (Exception e) {
                plugin.logDebug("Twitch follower monitor check failed: " + e.getMessage());
            }
        };

        if (isFolia) {
            this.task = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (t) -> checkTask.run(), 10, 30, java.util.concurrent.TimeUnit.SECONDS);
        } else {
            this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, checkTask, 200L, 600L);
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