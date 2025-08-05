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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AlertService {

    private final NamLivechat plugin;
    private final Map<UUID, Object> activeBossBarTasks = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> activeBossBars = new ConcurrentHashMap<>();
    private final boolean isFolia;

    public AlertService(NamLivechat plugin, boolean isFolia) {
        this.plugin = plugin;
        this.isFolia = isFolia;
    }

    // --- เมธอดถูกเปลี่ยนชื่อและแก้ไข Logic ---
    public void showBossBarAlert(Player player, String title, BarColor color, int duration) {
        // หยุดและล้าง Boss Bar เก่าทิ้งทันที
        stopBossBar(player);

        UUID playerUUID = player.getUniqueId();
        BossBar bossBar = Bukkit.createBossBar(title, color, BarStyle.SOLID);
        bossBar.setProgress(1.0);
        bossBar.addPlayer(player);
        activeBossBars.put(playerUUID, bossBar);

        final long totalTicks = duration * 20L;
        final long[] ticksElapsed = {0};

        Runnable taskRunnable = () -> {
            ticksElapsed[0]++;
            double progress = 1.0 - ((double) ticksElapsed[0] / totalTicks);

            if (progress <= 0) {
                stopBossBar(player);
            } else {
                BossBar currentBar = activeBossBars.get(playerUUID);
                if (currentBar != null) {
                    currentBar.setProgress(Math.max(0, progress));
                }
            }
        };

        Object task;
        if (isFolia) {
            task = player.getScheduler().runAtFixedRate(plugin, t -> {
                if (!activeBossBarTasks.containsKey(playerUUID)) {
                    t.cancel();
                } else {
                    taskRunnable.run();
                }
            }, null, 1L, 1L);
        } else {
            task = Bukkit.getScheduler().runTaskTimer(plugin, taskRunnable, 0L, 1L);
        }
        activeBossBarTasks.put(playerUUID, task);
    }

    public void stopBossBar(Player player) {
        UUID playerUUID = player.getUniqueId();
        Object task = activeBossBarTasks.remove(playerUUID);
        if (task != null) {
            if (task instanceof BukkitTask) ((BukkitTask) task).cancel();
            else if (task instanceof ScheduledTask) ((ScheduledTask) task).cancel();
        }

        BossBar bossBar = activeBossBars.remove(playerUUID);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }
}