package io.wangock.wmotd.managers;

import io.wangock.wmotd.Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class IconManager {
    private final Main plugin;
    private final Map<String, BufferedImage> templateToIcon = new HashMap<>();
    private volatile BufferedImage currentIcon;
    private BukkitTask rotationTask;
    private long iconTtlMs;
    private long lastLoad;

    public IconManager(Main plugin) {
        this.plugin = plugin;
        this.iconTtlMs = plugin.getConfigManager().getConfig().getLong("cache.icon-ttl-ms", 5000);
        ensureIconsFolder();
        preloadConfiguredIcons();
    }

    public void start() {
        FileConfiguration cfg = plugin.getConfigManager().getConfig();
        if (!cfg.getBoolean("icons.enabled", true)) return;
        long interval = cfg.getLong("icons.rotation-interval-ticks", 200L);
        if (rotationTask != null) rotationTask.cancel();
        rotationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::rotateIfNeeded, interval, interval);
    }

    public void stop() {
        if (rotationTask != null) {
            rotationTask.cancel();
            rotationTask = null;
        }
    }

    private void ensureIconsFolder() {
        File dir = new File(plugin.getDataFolder(), plugin.getConfigManager().getConfig().getString("icons.folder", "icons"));
        if (!dir.exists()) {
            // Provide a default icon if none
            dir.mkdirs();
            try {
                // No binary generation here; users should put icons in folder
            } catch (Exception ignored) {}
        }
    }

    private void preloadConfiguredIcons() {
        FileConfiguration cfg = plugin.getConfigManager().getConfig();
        String folder = cfg.getString("icons.folder", "icons");
        Map<String, Object> map = cfg.getConfigurationSection("icons.map") != null ?
                cfg.getConfigurationSection("icons.map").getValues(false) : new HashMap<>();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String template = e.getKey();
            String fileName = String.valueOf(e.getValue());
            File file = new File(plugin.getDataFolder(), folder + File.separator + fileName);
            if (file.exists()) {
                try {
                    BufferedImage img = ImageIO.read(file);
                    templateToIcon.put(template, img);
                } catch (IOException ignored) {}
            }
        }
        currentIcon = templateToIcon.getOrDefault("default", null);
    }

    private void rotateIfNeeded() {
        // If TTL passed and sync-with-motd is disabled, rotate through loaded icons
        FileConfiguration cfg = plugin.getConfigManager().getConfig();
        boolean sync = cfg.getBoolean("icons.sync-with-motd", true);
        long now = System.currentTimeMillis();
        if (sync) return;
        if (now - lastLoad <= iconTtlMs) return;
        // Simple rotation by map order
        for (BufferedImage image : templateToIcon.values()) {
            currentIcon = image;
            break;
        }
        lastLoad = now;
    }

    public void onTemplateUsed(String template) {
        if (!plugin.getConfigManager().getConfig().getBoolean("icons.sync-with-motd", true)) return;
        BufferedImage img = templateToIcon.get(template);
        if (img == null) img = templateToIcon.get("default");
        if (img != null) currentIcon = img;
    }

    public BufferedImage getCurrentIcon() {
        return currentIcon;
    }
}


