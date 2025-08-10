package io.wangock.wmotd.hooks;

import io.wangock.wmotd.Main;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class PlaceholderHook {
    private final Main plugin;
    private final boolean papiEnabledByConfig;

    public PlaceholderHook(Main plugin) {
        this.plugin = plugin;
        this.papiEnabledByConfig = plugin.getConfigManager().getConfig().getBoolean("placeholders.placeholderapi.enabled", true);
    }

    public boolean isAvailable() {
        return papiEnabledByConfig && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public String apply(String text, UUID uuid) {
        if (!isAvailable()) return text;
        try {
            OfflinePlayer player = null;
            if (uuid != null) player = Bukkit.getOfflinePlayer(uuid);
            return PlaceholderAPI.setPlaceholders(player, text);
        } catch (Throwable t) {
            return text;
        }
    }
}


