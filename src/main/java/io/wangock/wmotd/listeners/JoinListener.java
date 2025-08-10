package io.wangock.wmotd.listeners;

import io.wangock.wmotd.managers.PlayerTracker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {
    private final PlayerTracker playerTracker;

    public JoinListener(PlayerTracker playerTracker) {
        this.playerTracker = playerTracker;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playerTracker.markKnown(event.getPlayer().getUniqueId());
    }
}


