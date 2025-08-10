package io.wangock.wmotd.managers;

import io.wangock.wmotd.Main;
import org.bukkit.OfflinePlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerTracker {
    private final Main plugin;
    private final Set<UUID> knownPlayers = new HashSet<>();

    public PlayerTracker(Main plugin) {
        this.plugin = plugin;
        // Preload known players from server to greet only new ones
        for (OfflinePlayer p : plugin.getServer().getOfflinePlayers()) {
            if (p.getUniqueId() != null) knownPlayers.add(p.getUniqueId());
        }
    }

    public boolean isNew(UUID uuid) {
        return uuid != null && !knownPlayers.contains(uuid);
    }

    public void markKnown(UUID uuid) {
        if (uuid != null) knownPlayers.add(uuid);
    }
}


