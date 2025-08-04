package com.namLivechat.platform.Twitch;

import com.namLivechat.NamLivechat;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask; // เพิ่ม import นี้
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class TwitchBossBar {

    private final NamLivechat plugin;
    private final Player player;
    private final boolean isFolia;
    private BossBar currentBossBar;

    // --- ส่วนที่แก้ไข: เปลี่ยนชนิดตัวแปรเป็น Object ---
    private Object currentTask;

    public TwitchBossBar(NamLivechat plugin, Player player, boolean isFolia) {
        this.plugin = plugin;
        this.player = player;
        this.isFolia = isFolia;
    }

    public void showAlert(String title, BarColor color, int durationSeconds) {
        stop();

        currentBossBar = Bukkit.createBossBar(title, color, BarStyle.SOLID);
        currentBossBar.setProgress(1.0);
        currentBossBar.addPlayer(player);

        final long totalTicks = durationSeconds * 20L;

        Runnable taskRunnable = new Runnable() {
            private long ticksElapsed = 0;

            @Override
            public void run() {
                if (player == null || !player.isOnline()) {
                    stop();
                    return;
                }
                ticksElapsed++;
                double progress = 1.0 - ((double) ticksElapsed / totalTicks);

                if (progress <= 0) {
                    stop();
                    return;
                }

                if (currentBossBar != null) {
                    currentBossBar.setProgress(Math.max(0, progress));
                }
            }
        };

        if (isFolia) {
            // --- ส่วนที่แก้ไข: ไม่ต้องมี BukkitTask ด้านหน้า ---
            currentTask = player.getScheduler().runAtFixedRate(plugin, (task) -> taskRunnable.run(), null, 1L, 1L);
        } else {
            // --- ส่วนที่แก้ไข: ไม่ต้องมี BukkitTask ด้านหน้า ---
            currentTask = Bukkit.getScheduler().runTaskTimer(plugin, taskRunnable, 0L, 1L);
        }
    }

    public void stop() {
        // --- ส่วนที่แก้ไข: ตรวจสอบชนิดของ Task ก่อนสั่งยกเลิก ---
        if (currentTask != null) {
            if (currentTask instanceof BukkitTask) {
                if (!((BukkitTask) currentTask).isCancelled()) {
                    ((BukkitTask) currentTask).cancel();
                }
            } else if (currentTask instanceof ScheduledTask) {
                if (!((ScheduledTask) currentTask).isCancelled()) {
                    ((ScheduledTask) currentTask).cancel();
                }
            }
        }

        if (currentBossBar != null) {
            currentBossBar.removeAll();
        }

        currentTask = null;
        currentBossBar = null;
    }
}