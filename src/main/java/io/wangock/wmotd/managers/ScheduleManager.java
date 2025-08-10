package io.wangock.wmotd.managers;

import io.wangock.wmotd.Main;
import io.wangock.wmotd.hooks.WeatherHook;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ScheduleManager {
    private final Main plugin;
    private final WeatherHook weatherHook;
    private BukkitTask task;
    private final AtomicInteger cycleIndex = new AtomicInteger(0);

    private volatile int votesCount = 0; // Placeholder for vote integration

    public ScheduleManager(Main plugin, WeatherHook weatherHook) {
        this.plugin = plugin;
        this.weatherHook = weatherHook;
    }

    public void start() {
        FileConfiguration cfg = plugin.getConfigManager().getConfig();
        if (!cfg.getBoolean("scheduler.enabled", true)) return;
        long interval = cfg.getLong("rotation.interval-ticks", 200L);
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // advance cycle index on interval to animate even without pings
            cycleIndex.incrementAndGet();
        }, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public String resolveScheduledTemplate() {
        FileConfiguration cfg = plugin.getConfigManager().getConfig();
        if (!cfg.getBoolean("scheduler.enabled", true)) return null;

        // Date based (MM-dd)
        Map<String, String> dateMap = plugin.getConfigManager().getDateTemplates();
        String todayKey = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"));
        if (dateMap.containsKey(todayKey)) return dateMap.get(todayKey);

        // Time based
        ConfigurationSection times = cfg.getConfigurationSection("scheduler.times");
        if (times != null) {
            LocalTime now = LocalTime.now();
            for (String key : times.getKeys(false)) {
                String from = times.getString(key + ".from");
                String to = times.getString(key + ".to");
                String template = times.getString(key + ".template");
                if (from == null || to == null || template == null) continue;
                LocalTime f = LocalTime.parse(from);
                LocalTime t = LocalTime.parse(to);
                boolean between;
                if (f.isBefore(t) || f.equals(t)) {
                    between = !now.isBefore(f) && !now.isAfter(t);
                } else {
                    // Over midnight
                    between = !now.isBefore(f) || !now.isAfter(t);
                }
                if (between) return template;
            }
        }

        // Weather-based example (simple): day/night templates
        if (cfg.getBoolean("weather.enabled", false)) {
            if (weatherHook.isNight()) return "night_owl";
            else return "hello_day";
        }

        return null;
    }

    public String resolvePersonalizedTemplate(UUID uuid, String ip, String country) {
        FileConfiguration cfg = plugin.getConfigManager().getConfig();
        List<String> priorities = cfg.getStringList("personalization.priority");
        if (priorities.isEmpty()) priorities = Arrays.asList("uuid", "permission", "ip", "country");

        for (String p : priorities) {
            switch (p) {
                case "uuid":
                    if (uuid != null) {
                        String temp = cfg.getString("personalization.uuids." + uuid);
                        if (temp != null) return temp;
                    }
                    break;
                case "permission":
                    if (uuid != null && Bukkit.getPlayer(uuid) != null) {
                        ConfigurationSection groups = cfg.getConfigurationSection("personalization.groups");
                        if (groups != null) {
                            for (String key : groups.getKeys(false)) {
                                String perm = groups.getString(key + ".permission");
                                String template = groups.getString(key + ".template");
                                if (perm != null && template != null && Bukkit.getPlayer(uuid).hasPermission(perm)) {
                                    return template;
                                }
                            }
                        }
                    }
                    break;
                case "ip":
                    if (ip != null) {
                        String temp = cfg.getString("personalization.ips." + ip);
                        if (temp != null) return temp;
                    }
                    break;
                case "country":
                    if (country != null) {
                        String temp = cfg.getString("personalization.countries." + country);
                        if (temp != null) return temp;
                    }
                    break;
            }
        }
        return null;
    }

    public String getNextCycled(List<String> keys) {
        if (keys.isEmpty()) return "default";
        int idx = Math.floorMod(cycleIndex.getAndIncrement(), keys.size());
        return keys.get(idx);
    }

    public int getVotesCount() {
        return votesCount;
    }
}


