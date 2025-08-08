package com.namLivechat.service;

import com.namLivechat.NamLivechat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class UpdateChecker implements Listener {

    private final NamLivechat plugin;
    private final MessageHandler messageHandler;
    private String latestVersion;
    private boolean updateAvailable = false;
    private final File pluginFile;

    private final Set<UUID> notifiedAdmins = ConcurrentHashMap.newKeySet();

    public UpdateChecker(NamLivechat plugin) {
        this.plugin = plugin;
        this.messageHandler = plugin.getMessageHandler();
        this.pluginFile = plugin.getPluginFile();
    }

    public void check() {
        if (!plugin.getConfig().getBoolean("update-alert", true)) {
            return;
        }
        runAsyncTask(() -> {
            try {
                URL url = new URL("https://api.github.com/repos/Nattapat2871/NamLivechat/releases/latest");
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                String json = result.toString();
                if (json.contains("\"tag_name\":\"")) {
                    this.latestVersion = json.split("\"tag_name\":\"")[1].split("\"")[0];
                    this.updateAvailable = !plugin.getDescription().getVersion().equalsIgnoreCase(this.latestVersion);
                    if (this.updateAvailable) {
                        String message = String.format("&a[NamLivechat] &eA new version is available: &6%s", this.latestVersion);
                        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                    }
                }
            } catch (Exception e) {
                plugin.logDebug("Failed to check for updates: " + e.getMessage());
            }
        });
    }

    public void downloadUpdate(CommandSender sender) {
        if (pluginFile == null) {
            messageHandler.sendMessage(sender, "update_fail");
            return;
        }
        if (!updateAvailable) {
            messageHandler.sendMessage(sender, "update_latest");
            return;
        }
        if (!plugin.getConfig().getBoolean("auto-update", true)) {
            messageHandler.sendMessage(sender, "update_disabled");
            return;
        }

        messageHandler.sendFormattedMessage(sender, "update_downloading", "%version%", latestVersion);
        plugin.getLogger().info("Update download initiated by " + sender.getName() + " for version " + latestVersion + "...");

        runAsyncTask(() -> {
            try {
                String artifactId = plugin.getDescription().getName();
                String fileNameWithVersion = String.format("%s-%s.jar", artifactId, latestVersion);
                String downloadUrlString = String.format("https://github.com/Nattapat2871/NamLivechat/releases/download/%s/%s", latestVersion, fileNameWithVersion);
                URL downloadUrl = new URL(downloadUrlString);

                File updateFolder = plugin.getServer().getUpdateFolderFile();
                if (!updateFolder.exists()) {
                    updateFolder.mkdirs();
                }

                File downloadedFile = new File(updateFolder, this.pluginFile.getName());

                try (InputStream in = downloadUrl.openStream()) {
                    Files.copy(in, downloadedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                plugin.getLogger().info("Update " + latestVersion + " has been downloaded successfully.");
                plugin.getLogger().info("The new version will be installed automatically on the next server restart.");

                messageHandler.sendMessage(sender, "update_success");
                messageHandler.sendMessage(sender, "update_restart");

            } catch (Exception e) {
                messageHandler.sendMessage(sender, "update_fail");
                plugin.getLogger().severe("Update download failed: " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (player.hasPermission("namlivechat.admin") && updateAvailable && !notifiedAdmins.contains(playerUUID)) {

            notifiedAdmins.add(playerUUID);

            Runnable notificationTask = () -> {
                if (player.isOnline()) {
                    messageHandler.sendFormattedMessage(player, "update_available", "%version%", latestVersion);
                    player.playSound(player.getLocation(), "entity.player.levelup",1.0f,1.0f);
                }
            };

            if (plugin.isFolia()) {
                player.getScheduler().runDelayed(plugin, task -> notificationTask.run(), null, 100L);
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, notificationTask, 100L);
            }

            Runnable removeTask = () -> notifiedAdmins.remove(playerUUID);
            if (plugin.isFolia()) {
                Bukkit.getAsyncScheduler().runDelayed(plugin, task -> removeTask.run(), 10, TimeUnit.SECONDS);
            } else {
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, removeTask, 200L);
            }
        }
    }

    private void runOnPlayerThread(Player player, Runnable runnable) {
        if (plugin.isDisabling() || player == null || !player.isOnline()) return;
        if (plugin.isFolia()) {
            player.getScheduler().run(plugin, (task) -> runnable.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    private void runAsyncTask(Runnable runnable) {
        if (plugin.isDisabling()) return;
        if (plugin.isFolia()) {
            Bukkit.getAsyncScheduler().runNow(plugin, (task) -> runnable.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }
}