package io.wangock.wmotd.listeners;

import io.wangock.wmotd.Main;
import io.wangock.wmotd.hooks.GeoHook;
import io.wangock.wmotd.managers.IconManager;
import io.wangock.wmotd.managers.MOTDManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import java.awt.image.BufferedImage;
import java.net.InetAddress;
import java.util.UUID;

public class PingListener implements Listener {
    private final MOTDManager motdManager;
    private final IconManager iconManager;
    private final GeoHook geoHook;

    public PingListener(Main plugin, MOTDManager motdManager, IconManager iconManager, GeoHook geoHook) {
        this.motdManager = motdManager;
        this.iconManager = iconManager;
        this.geoHook = geoHook;
    }

    @EventHandler
    public void onSpigotPing(ServerListPingEvent event) {
        String name = null;
        UUID uuid = null;
        InetAddress addr = event.getAddress();
        String ip = addr != null ? addr.getHostAddress() : null;
        String country = geoHook.getCountryCached(ip);

        Component motd = motdManager.buildMotd(uuid, name, ip, country);
        String legacy = LegacyComponentSerializer.legacySection().serialize(motd);
        event.setMotd(legacy);

        BufferedImage icon = iconManager.getCurrentIcon();
        if (icon != null) {
            try {
                event.setServerIcon(Bukkit.loadServerIcon(icon));
            } catch (Exception ignored) {}
        }
    }
}


