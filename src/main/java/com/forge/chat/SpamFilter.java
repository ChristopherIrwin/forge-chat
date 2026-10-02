package com.forge.chat;

import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/**
 * Anti-spam checks. Per-player rate state lives here; the individual
 * detectors are static and Bukkit-free for unit testing.
 */
public final class SpamFilter {
    private SpamFilter() {
    }

    private static final Pattern LINK = Pattern.compile(
            "(?i)\\b((https?://|www\\.)\\S+|[a-z0-9-]+\\.(com|net|org|io|gg|dev|me|co|us|uk|de|fr|xyz|info|biz|tv|cc)(\\b|/\\S*))");

    public enum Fail {
        RATE, REPEAT, CAPS, LINK
    }

    private static final Map<UUID, Deque<Long>> HISTORY = new ConcurrentHashMap<>();

    /**
     * Run every check. Returns null when the message passes.
     * Thread-safe: may be called from the async chat thread.
     */
    public static @Nullable Fail check(Player player, String text, int maxMessages, int windowSeconds,
                             int maxRepeatChars, int capsPercent, int capsMinLength,
                             boolean blockLinks, List<String> linkWhitelist) {
        long now = System.currentTimeMillis();
        Deque<Long> times = HISTORY.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() > windowSeconds * 1000L) {
                times.pollFirst();
            }
            times.addLast(now);
            if (times.size() > maxMessages) {
                return Fail.RATE;
            }
        }
        if (hasRepeatChars(text, maxRepeatChars)) {
            return Fail.REPEAT;
        }
        if (capsPercent >= 0 && text.length() >= capsMinLength && capsRatio(text) * 100.0 >= capsPercent) {
            return Fail.CAPS;
        }
        if (blockLinks && containsBlockedLink(text, linkWhitelist)) {
            return Fail.LINK;
        }
        return null;
    }

    /** True when more than {@code max} identical characters run consecutively. */
    public static boolean hasRepeatChars(String text, int max) {
        if (max < 1) {
            return false;
        }
        int run = 1;
        for (int i = 1; i < text.length(); i++) {
            if (text.charAt(i) == text.charAt(i - 1)) {
                if (++run > max) {
                    return true;
                }
            } else {
                run = 1;
            }
        }
        return false;
    }

    /** Fraction of letters that are uppercase, 0.0 when there are no letters. */
    public static double capsRatio(String text) {
        int letters = 0;
        int caps = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) {
                    caps++;
                }
            }
        }
        return letters == 0 ? 0.0 : (double) caps / letters;
    }

    /** True when the text contains a link whose domain is not whitelisted. */
    public static boolean containsBlockedLink(String text, List<String> whitelist) {
        Matcher m = LINK.matcher(text);
        while (m.find()) {
            String url = m.group(1).toLowerCase();
            boolean allowed = false;
            for (String entry : whitelist) {
                if (!entry.isBlank() && url.contains(entry.toLowerCase())) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                return true;
            }
        }
        return false;
    }
}
