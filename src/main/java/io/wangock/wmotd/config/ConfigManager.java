package io.wangock.wmotd.config;

import io.wangock.wmotd.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private final Main plugin;

    private FileConfiguration config;
    private FileConfiguration templates;
    private FileConfiguration messages;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        this.templates = load("templates.yml");
        this.messages = load("messages.yml");
    }

    public void saveState() {
        // For future state persistence (e.g., stats). Currently no-op
    }

    private FileConfiguration load(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() { return config; }
    public FileConfiguration getTemplates() { return templates; }
    public FileConfiguration getMessages() { return messages; }

    public String getLocale() {
        return config.getString("locale", "en_US");
    }

    public Map<String, Object> getSchedulerTimes() {
        Map<String, Object> map = new HashMap<>();
        ConfigurationSection sec = config.getConfigurationSection("scheduler.times");
        if (sec != null) map.putAll(sec.getValues(false));
        return map;
    }

    public Map<String, String> getDateTemplates() {
        Map<String, String> out = new HashMap<>();
        ConfigurationSection sec = config.getConfigurationSection("scheduler.dates");
        if (sec != null) {
            for (String k : sec.getKeys(false)) {
                out.put(k, sec.getString(k));
            }
        }
        return out;
    }

    public List<String> getTemplateLines(String templateName) {
        return templates.getStringList(templateName + ".lines");
    }

    public String getMessage(String key) {
        String locale = getLocale();
        String path = locale + "." + key;
        String s = messages.getString(path);
        if (s == null) {
            s = messages.getString("en_US." + key, key);
        }
        return s;
    }

    public String getPrefix() {
        return messages.getString("prefix", "&8[&bwMOTD&8]&r ");
    }

    public void reloadAll() throws IOException {
        loadAll();
    }
}


