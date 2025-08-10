package io.wangock.wmotd.managers;

// Gson not used currently; kept as dependency for JSON hooks
import io.wangock.wmotd.Main;
import io.wangock.wmotd.hooks.PlaceholderHook;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class MOTDManager {
    private final Main plugin;
    private final MiniMessageFormatter formatter;
    private final PlaceholderHook placeholderHook;
    private final ScheduleManager scheduleManager;
    private final IconManager iconManager;
    private final PlayerTracker playerTracker;

    private volatile String forcedTemplate; // message-of-the-day mode

    private long cacheTtlMs;
    private long lastBuildTime;
    private Component cachedMotd; // cached Component for quick reuse
    private String cachedTemplateName;

    private long motdGeneratedCount = 0L;
    private long cacheHits = 0L;
    private long cacheMisses = 0L;

    public MOTDManager(Main plugin,
                       MiniMessageFormatter formatter,
                       PlaceholderHook placeholderHook,
                       ScheduleManager scheduleManager,
                       IconManager iconManager,
                       PlayerTracker playerTracker) {
        this.plugin = plugin;
        this.formatter = formatter;
        this.placeholderHook = placeholderHook;
        this.scheduleManager = scheduleManager;
        this.iconManager = iconManager;
        this.playerTracker = playerTracker;
        this.cacheTtlMs = plugin.getConfigManager().getConfig().getLong("cache.motd-ttl-ms", 2000);
    }

    public void setForcedTemplate(String name) {
        this.forcedTemplate = name;
        invalidateCache();
    }

    public void invalidateCache() {
        this.lastBuildTime = 0L;
    }

    public Component buildMotd(UUID viewerUuid, String viewerName, String address, String countryCode) {
        long now = System.currentTimeMillis();
        if (now - lastBuildTime <= cacheTtlMs && cachedMotd != null) {
            cacheHits++;
            return cachedMotd;
        }
        cacheMisses++;

        String templateName = chooseTemplate(viewerUuid, viewerName, address, countryCode);
        List<String> lines = plugin.getConfigManager().getTemplateLines(templateName);
        if (lines == null || lines.isEmpty()) {
            lines = plugin.getConfigManager().getTemplateLines("default");
            templateName = "default";
        }

        String joined = String.join("\n", lines);
        String replaced = applyPlaceholders(joined, viewerUuid, viewerName);
        replaced = replaced.replace("{bar}", generateProgressBar())
                           .replace("{percent}", String.valueOf(getServerFillPercent()));

        Component component = formatter.parse(replaced);
        this.cachedMotd = component;
        this.cachedTemplateName = templateName;
        this.lastBuildTime = now;
        this.motdGeneratedCount++;

        // Sync icon to template if enabled
        iconManager.onTemplateUsed(templateName);

        return component;
    }

    private String generateProgressBar() {
        int max = Bukkit.getMaxPlayers();
        int online = Bukkit.getOnlinePlayers().size();
        int percent = max > 0 ? (int) Math.round(100.0 * online / max) : 0;
        int filled = (int) Math.round(percent / 10.0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(i < filled ? '█' : '░');
        }
        return sb.toString();
    }

    private int getServerFillPercent() {
        int max = Bukkit.getMaxPlayers();
        int online = Bukkit.getOnlinePlayers().size();
        if (max <= 0) return 0;
        return (int) Math.round(100.0 * online / max);
    }

    private String applyPlaceholders(String text, UUID viewerUuid, String viewerName) {
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        Duration uptime = Duration.between(plugin.getServerStartInstant(), Instant.now());
        String uptimeStr = formatDuration(uptime);
        String votesStr = resolveVotes(viewerUuid);

        String result = text
                .replace("{online}", String.valueOf(online))
                .replace("{max}", String.valueOf(max))
                .replace("{player}", viewerName != null ? viewerName : "Гость")
                .replace("{uptime}", uptimeStr)
                .replace("{votes}", votesStr);

        // PlaceholderAPI integration
        result = placeholderHook.apply(result, viewerUuid);
        return result;
    }

    private String resolveVotes(UUID viewerUuid) {
        // Try common PAPI placeholders if available
        String fallback = String.valueOf(scheduleManager.getVotesCount());
        try {
            String[] candidates = new String[] {
                    "%votingplugin_votes%",
                    "%votingplugin_points%",
                    "%nuVotifier_votes%"
            };
            for (String c : candidates) {
                String val = placeholderHook.apply(c, viewerUuid);
                if (val != null && val.matches("\\d+")) return val;
            }
        } catch (Throwable ignored) {}
        return fallback;
    }

    private String formatDuration(Duration d) {
        long seconds = d.getSeconds();
        long days = seconds / 86400; seconds %= 86400;
        long hours = seconds / 3600; seconds %= 3600;
        long minutes = seconds / 60; seconds %= 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString();
    }

    private String chooseTemplate(UUID uuid, String playerName, String address, String country) {
        FileConfiguration cfg = plugin.getConfigManager().getConfig();

        // Reactive conditions
        if (Bukkit.hasWhitelist()) {
            String t = cfg.getString("reactive.whitelist-template");
            if (t != null && !t.isEmpty()) return t;
        }
        if (Bukkit.getOnlinePlayers().size() >= Bukkit.getMaxPlayers()) {
            String t = cfg.getString("reactive.full-server-template");
            if (t != null && !t.isEmpty()) return t;
        }
        if (plugin.getServer().isStopping()) {
            String t = cfg.getString("reactive.restarting-template");
            if (t != null && !t.isEmpty()) return t;
        }

        // Forced template (message of the day mode)
        if (forcedTemplate != null && !forcedTemplate.isEmpty()) {
            return forcedTemplate;
        }

        // New player greeting (if known during ping)
        if (uuid != null && playerTracker.isNew(uuid)) {
            String newTpl = cfg.getString("personalization.new-player-template", "welcome_new");
            if (newTpl != null && !newTpl.isEmpty()) return newTpl;
        }

        // Schedule
        String scheduled = scheduleManager.resolveScheduledTemplate();
        if (scheduled != null) return scheduled;

        // Personalization
        String personalized = scheduleManager.resolvePersonalizedTemplate(uuid, address, country);
        if (personalized != null) return personalized;

        // Secret chance
        if (cfg.getBoolean("rotation.secret-motd.enabled", true)) {
            double chance = cfg.getDouble("rotation.secret-motd.chance", 0.01);
            if (ThreadLocalRandom.current().nextDouble() < chance) {
                List<String> lines = plugin.getConfigManager().getTemplateLines("secret");
                if (lines != null && !lines.isEmpty()) return "secret";
            }
        }

        // Rotation
        String mode = cfg.getString("rotation.mode", "cycle");
        List<String> keys = new ArrayList<>(plugin.getConfigManager().getTemplates().getKeys(false));
        if (keys.isEmpty()) return "default";
        keys.remove("secret");
        switch (mode) {
            case "random":
                return keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
            case "schedule":
                // already handled above
                return "default";
            default:
                return scheduleManager.getNextCycled(keys);
        }
    }

    public String getCachedTemplateName() { return cachedTemplateName; }
    public long getMotdGeneratedCount() { return motdGeneratedCount; }
    public long getCacheHits() { return cacheHits; }
    public long getCacheMisses() { return cacheMisses; }
}


