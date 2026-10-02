package com.forge.chat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/**
 * @mention parsing. Bukkit-free: resolution is generic over {@link Named},
 * so the logic is unit-testable without a server.
 */
public final class MentionParser {
    private MentionParser() {
    }

    private static final Pattern TOKEN = Pattern.compile("@([A-Za-z0-9_]{1,16})");

    /** Anything with a Minecraft-style name that can be mentioned. */
    public interface Named {
        String name();
    }

    public record Span(String token, int start, int end) {
    }

    /** Find every @token span in the text, in order, non-overlapping. */
    public static List<Span> findSpans(String text) {
        List<Span> out = new ArrayList<>();
        Matcher m = TOKEN.matcher(text);
        while (m.find()) {
            out.add(new Span(m.group(1), m.start(), m.end()));
        }
        return out;
    }

    /**
     * Resolve a token to a candidate. Exact (case-insensitive) match wins;
     * otherwise a single unambiguous prefix match. Returns null when there is
     * no match or several candidates share the prefix. The sender itself is
     * never a valid target.
     */
    public static <T extends Named> @Nullable T resolve(String token, T sender, Collection<T> candidates) {
        String lower = token.toLowerCase();
        T prefix = null;
        boolean ambiguous = false;
        for (T candidate : candidates) {
            if (candidate == sender) {
                continue;
            }
            String name = candidate.name().toLowerCase();
            if (name.equals(lower)) {
                return candidate;
            }
            if (name.startsWith(lower)) {
                if (prefix != null) {
                    ambiguous = true;
                } else {
                    prefix = candidate;
                }
            }
        }
        return ambiguous ? null : prefix;
    }

    public static boolean isBroadcastToken(String token) {
        return token.equalsIgnoreCase("here") || token.equalsIgnoreCase("everyone");
    }
}
