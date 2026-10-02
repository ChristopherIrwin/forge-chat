package com.forge.chat;

import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Per-group chat formatting. Groups are an ordered config list; the first
 * group whose permission the player holds wins.
 */
public record GroupFormat(String permission, String label, Component prefix, Component suffix, String nameFormat) {

    public static List<GroupFormat> load(List<Map<?, ?>> raw) {
        List<GroupFormat> out = new ArrayList<>();
        for (Map<?, ?> entry : raw) {
            String permission = string(entry.get("permission"), "");
            String label = string(entry.get("label"), permission.isEmpty() ? "Default" : permission);
            Component prefix = TextUtil.safe(string(entry.get("prefix"), ""));
            Component suffix = TextUtil.safe(string(entry.get("suffix"), ""));
            String nameFormat = string(entry.get("name-format"), "{name}");
            out.add(new GroupFormat(permission, label, prefix, suffix, nameFormat));
        }
        if (out.isEmpty()) {
            out.add(new GroupFormat("", "Default", Component.empty(), Component.empty(), "{name}"));
        }
        return out;
    }

    private static String string(@Nullable Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    /** Build the display-name component for a player (hover applied separately). */
    public Component nameComponent(String playerName) {
        return Component.text()
                .append(prefix)
                .append(TextUtil.safe(nameFormat.replace("{name}", playerName)))
                .append(suffix)
                .build();
    }
}
