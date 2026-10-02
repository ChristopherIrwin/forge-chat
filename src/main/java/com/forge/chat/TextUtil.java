package com.forge.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/**
 * MiniMessage helpers, plain-text extraction and duration parsing/formatting.
 * Bukkit-free: safe to unit test on the plain JVM.
 */
public final class TextUtil {
    private TextUtil() {
    }

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final Pattern DURATION = Pattern.compile("^(\\d+)(s|m|h|d|w)$", Pattern.CASE_INSENSITIVE);

    public static MiniMessage mm() {
        return MM;
    }

    public static String plain(Component component) {
        return PLAIN.serialize(component);
    }

    /**
     * Deserialize MiniMessage, falling back to literal text when the input
     * contains malformed tags instead of throwing.
     */
    public static Component safe(String input) {
        try {
            return MM.deserialize(input);
        } catch (Exception e) {
            return Component.text(input);
        }
    }

    /**
     * Converts ForgeChat's {placeholder} config style to MiniMessage
     * &lt;placeholder&gt; tags so both styles work in format templates.
     * Configs document {tag} {name} {message}; the renderer resolves
     * &lt;tag&gt; &lt;name&gt; &lt;message&gt;.
     */
    public static String bracesToTags(String template) {
        return template.replace("{tag}", "<tag>")
                .replace("{name}", "<name>")
                .replace("{message}", "<message>")
                .replace("{sender}", "<sender>")
                .replace("{recipient}", "<recipient>");
    }

    /**
     * Deserialize a template with tag resolvers, falling back to the given
     * component when the template has malformed tags instead of throwing.
     */
    public static Component render(String template, Component fallback, TagResolver... resolvers) {
        try {
            return resolvers.length == 0 ? MM.deserialize(template) : MM.deserialize(template, TagResolver.resolver(resolvers));
        } catch (Exception e) {
            return fallback;
        }
    }

    /**
     * Parse "10s", "5m", "2h", "1d", "1w", "perm"/"permanent".
     *
     * @return seconds, -1 for permanent, -2 for invalid input
     */
    public static long parseDuration(@Nullable String input) {
        if (input == null) {
            return -2;
        }
        String text = input.trim().toLowerCase();
        if (text.equals("perm") || text.equals("permanent") || text.equals("forever")) {
            return -1;
        }
        Matcher m = DURATION.matcher(text);
        if (!m.matches()) {
            return -2;
        }
        long amount = Long.parseLong(m.group(1));
        long multiplier = switch (m.group(2)) {
            case "s" -> 1L;
            case "m" -> 60L;
            case "h" -> 3600L;
            case "d" -> 86400L;
            case "w" -> 604800L;
            default -> 1L;
        };
        return amount * multiplier;
    }

    public static String formatDuration(long totalSeconds) {
        if (totalSeconds < 0) {
            return "permanent";
        }
        if (totalSeconds < 60) {
            return totalSeconds + "s";
        }
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long hours = minutes / 60;
        minutes %= 60;
        long days = hours / 24;
        hours %= 24;
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append('d').append(' ');
        }
        if (hours > 0) {
            sb.append(hours).append('h').append(' ');
        }
        if (minutes > 0) {
            sb.append(minutes).append('m').append(' ');
        }
        if (seconds > 0 && days == 0 && hours == 0) {
            sb.append(seconds).append('s');
        }
        return sb.toString().trim();
    }

    /** Replace {key} placeholders; keys and values alternate. */
    public static String placeholders(String template, String... pairs) {
        String out = template;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            out = out.replace(pairs[i], pairs[i + 1]);
        }
        return out;
    }
}
