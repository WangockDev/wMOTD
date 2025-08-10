package io.wangock.wmotd.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ColorUtil {
    private ColorUtil() {}

    public static String legacyToString(String legacy) {
        Component c = LegacyComponentSerializer.legacyAmpersand().deserialize(legacy);
        return c.toString();
    }
}


