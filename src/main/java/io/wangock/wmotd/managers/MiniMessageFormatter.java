package io.wangock.wmotd.managers;

import io.wangock.wmotd.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiniMessageFormatter {
    private final Main plugin;
    private final MiniMessage miniMessage;

    private final Pattern hexPattern = Pattern.compile("#([A-Fa-f0-9]{6})");

    public MiniMessageFormatter(Main plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public boolean isMiniMessageEnabled() {
        FileConfiguration cfg = plugin.getConfigManager().getConfig();
        return cfg.getBoolean("minimessage.enabled", true);
    }

    public Component parse(String input) {
        if (input == null || input.isEmpty()) return Component.empty();
        if (isMiniMessageEnabled()) {
            return miniMessage.deserialize(input);
        }
        // Fallback: support § codes and #RRGGBB
        String legacy = translateLegacyAndHex(input);
        return LegacyComponentSerializer.legacySection().deserialize(legacy);
    }

    private String translateLegacyAndHex(String input) {
        String out = input
                .replace('&', '§');
        Matcher m = hexPattern.matcher(out);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group(1);
            // Convert #RRGGBB to §x§R§R§G§G§B§B sequence
            StringBuilder seq = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                seq.append('§').append(Character.toLowerCase(c));
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(seq.toString()));
        }
        m.appendTail(sb);
        // Strip minimessage tags if present
        return sb.toString().replaceAll("<[^>]+>", "");
    }
}


