package io.wangock.wmotd.commands;

import io.wangock.wmotd.Main;
import io.wangock.wmotd.managers.IconManager;
import io.wangock.wmotd.managers.MOTDManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WMOTDCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final MOTDManager motdManager;
    private final IconManager iconManager; // used on reload to restart icon rotation

    public WMOTDCommand(Main plugin, MOTDManager motdManager, IconManager iconManager) {
        this.plugin = plugin;
        this.motdManager = motdManager;
        this.iconManager = iconManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Using messages prefix via color() method below when needed
        if (args.length == 0) {
            sender.sendMessage("/wmotd <reload|preview|set|stats>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("wmotd.admin.reload")) {
                    sender.sendMessage(color(plugin.getConfigManager().getMessage("no_permission")));
                    return true;
                }
                try {
                    plugin.getConfigManager().reloadAll();
                    plugin.getWeatherHook().reloadFromConfig();
                    iconManager.start();
                    plugin.getScheduleManager().start();
                    plugin.getMotdManager().invalidateCache();
                    sender.sendMessage(color(plugin.getConfigManager().getMessage("reloaded")));
                } catch (IOException e) {
                    sender.sendMessage("Failed to reload: " + e.getMessage());
                }
                return true;
            case "preview":
                if (!sender.hasPermission("wmotd.admin.preview")) {
                    sender.sendMessage(color(plugin.getConfigManager().getMessage("no_permission")));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("/wmotd preview <player>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(color(plugin.getConfigManager().getMessage("player_not_found")));
                    return true;
                }
                Component motd = motdManager.buildMotd(target.getUniqueId(), target.getName(), target.getAddress() != null ? target.getAddress().getAddress().getHostAddress() : null, null);
                target.sendMessage(motd);
                sender.sendMessage(color(plugin.getConfigManager().getMessage("preview_sent").replace("%player%", target.getName())));
                return true;
            case "set":
                if (!sender.hasPermission("wmotd.admin.set")) {
                    sender.sendMessage(color(plugin.getConfigManager().getMessage("no_permission")));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("/wmotd set <template>");
                    return true;
                }
                String template = args[1];
                if (template.equalsIgnoreCase("off") || template.equalsIgnoreCase("clear") || template.equals("-")) {
                    motdManager.setForcedTemplate(null);
                    sender.sendMessage(color(plugin.getConfigManager().getMessage("set_template_ok").replace("%template%", "off")));
                    return true;
                }
                if (!plugin.getConfigManager().getTemplates().isConfigurationSection(template)) {
                    sender.sendMessage("Template not found: " + template);
                    return true;
                }
                motdManager.setForcedTemplate(template);
                sender.sendMessage(color(plugin.getConfigManager().getMessage("set_template_ok").replace("%template%", template)));
                return true;
            case "stats":
                if (!sender.hasPermission("wmotd.admin.stats")) {
                    sender.sendMessage(color(plugin.getConfigManager().getMessage("no_permission")));
                    return true;
                }
                sender.sendMessage(color(plugin.getConfigManager().getMessage("stats.header")));
                sender.sendMessage(color(plugin.getConfigManager().getMessage("stats.motd_generated").replace("%count%", String.valueOf(motdManager.getMotdGeneratedCount()))));
                sender.sendMessage(color(plugin.getConfigManager().getMessage("stats.cache_hits")
                        .replace("%hits%", String.valueOf(motdManager.getCacheHits()))
                        .replace("%misses%", String.valueOf(motdManager.getCacheMisses()))));
                return true;
            default:
                sender.sendMessage("/wmotd <reload|preview|set|stats>");
                return true;
        }
    }

    private Component color(String msg) {
        String withPrefix = plugin.getConfigManager().getPrefix() + msg;
        return LegacyComponentSerializer.legacyAmpersand().deserialize(withPrefix);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "preview", "set", "stats");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return new ArrayList<>(plugin.getConfigManager().getTemplates().getKeys(false));
        }
        return List.of();
    }
}


