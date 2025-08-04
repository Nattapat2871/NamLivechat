package com.namLivechat.service;

import com.namLivechat.NamLivechat;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AlertService {

    private final NamLivechat plugin;
    private final Map<UUID, Queue<BossBarInfo>> bossBarQueue = new ConcurrentHashMap<>();
    private final Map<UUID, Object> activeBossBarTasks = new ConcurrentHashMap<>();
    private final boolean isFolia;

    private record BossBarInfo(String title, BarColor color, int duration) {}

    public AlertService(NamLivechat plugin, boolean isFolia) {
        this.plugin = plugin;
        this.isFolia = isFolia;
    }

    public void queueBossBar(Player player, String title, BarColor color, int duration) {
        UUID playerUUID = player.getUniqueId();
        bossBarQueue.putIfAbsent(playerUUID, new LinkedList<>());
        bossBarQueue.get(playerUUID).add(new BossBarInfo(title, color, duration));

        if (!activeBossBarTasks.containsKey(playerUUID)) {
            processNextBossBar(player);
        }
    }

    private void processNextBossBar(Player player) {
        if (player == null || !player.isOnline()) {
            if (player != null) {
                activeBossBarTasks.remove(player.getUniqueId());
            }
            return;
        }

        UUID playerUUID = player.getUniqueId();
        Queue<BossBarInfo> queue = bossBarQueue.get(playerUUID);

        if (queue == null || queue.isEmpty()) {
            activeBossBarTasks.remove(playerUUID);
            return;
        }

        activeBossBarTasks.put(playerUUID, new Object()); // Mark as processing

        BossBarInfo info = queue.poll();
        BossBar bossBar = Bukkit.createBossBar(info.title(), info.color(), BarStyle.SOLID);
        bossBar.setProgress(1.0);
        bossBar.addPlayer(player);

        final long totalTicks = info.duration() * 20L;
        final long[] ticksElapsed = {0};

        Runnable taskRunnable = () -> {
            ticksElapsed[0]++;
            double progress = 1.0 - ((double) ticksElapsed[0] / totalTicks);

            if (progress <= 0) {
                bossBar.removeAll();
                activeBossBarTasks.remove(playerUUID);
                processNextBossBar(player); // Process next in queue after removing the placeholder
            } else {
                bossBar.setProgress(Math.max(0, progress));
            }
        };

        Object task;
        if (isFolia) {
            task = player.getScheduler().runAtFixedRate(plugin, t -> {
                if (!activeBossBarTasks.containsKey(playerUUID)) {
                    t.cancel();
                    bossBar.removeAll();
                } else {
                    taskRunnable.run();
                }
            }, null, 1L, 1L);
        } else {
            task = Bukkit.getScheduler().runTaskTimer(plugin, taskRunnable, 0L, 1L);
        }
        // This logic is slightly flawed as it re-puts the task. The check should be if a *new* task should run.
        // The placeholder logic handles this now.
    }


    public void stopBossBar(Player player) {
        UUID playerUUID = player.getUniqueId();
        Object task = activeBossBarTasks.remove(playerUUID);
        if (task != null) {
            if (task instanceof BukkitTask) {
                if (!((BukkitTask) task).isCancelled()) ((BukkitTask) task).cancel();
            } else if (task instanceof ScheduledTask) {
                if (!((ScheduledTask) task).isCancelled()) ((ScheduledTask) task).cancel();
            }
        }
        if (bossBarQueue.containsKey(playerUUID)) {
            bossBarQueue.get(playerUUID).clear();
        }

        Iterator<KeyedBossBar> iterator = Bukkit.getBossBars();
        while (iterator.hasNext()) {
            BossBar bar = iterator.next();
            if (bar.getPlayers().contains(player)) {
                bar.removePlayer(player);
            }
        }
    }
}