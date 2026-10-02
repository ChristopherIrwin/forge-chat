package com.forge.chat;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The shared chat pipeline used by both the AsyncChatEvent listener and the
 * /g /l /staff one-shot commands. Runs mute, slowmode, anti-spam and word
 * filter checks, resolves @mentions, computes the viewer set per channel and
 * builds the final formatted line.
 *
 * Thread-safe: only touches concurrent maps and thread-safe config reads.
 */
public final class ChatPipeline {

    public record Result(Function<Audience, Component> line, Set<Audience> viewers, List<Player> pings) {
    }

    private record Mention(MentionParser.Span span, Player target) {
    }

    private record PlayerNamed(Player player) implements MentionParser.Named {
        @Override
        public String name() {
            return player.getName();
        }
    }

    private final ForgeChat plugin;

    public ChatPipeline(ForgeChat plugin) {
        this.plugin = plugin;
    }

    /**
     * Process a chat message. Returns null when the message was blocked, in
     * which case the sender has already been notified.
     */
    public @Nullable Result process(Player sender, ChatChannel channel, @Nullable String rawText) {
        String text = rawText == null ? "" : rawText.trim();
        if (text.isEmpty()) {
            return null;
        }
        FileConfiguration config = plugin.getConfig();
        UUID id = sender.getUniqueId();
        ForgeChat.PlayerMeta meta = plugin.meta(id);

        var mute = plugin.mutes().get(id);
        if (mute != null) {
            sender.sendMessage(TextUtil.safe(TextUtil.placeholders(
                    config.getString("messages.muted", "<red>You are muted."),
                    "{remaining}", plugin.mutes().remaining(id),
                    "{reason}", mute.reason())));
            return null;
        }

        if (channel == ChatChannel.STAFF && !meta.staff()) {
            sender.sendMessage(TextUtil.safe(config.getString("messages.no-permission", "<red>No permission.")));
            return null;
        }

        long slowSeconds = plugin.slowmode(channel);
        if (slowSeconds > 0) {
            long elapsed = (System.currentTimeMillis() - plugin.lastChat(id, channel)) / 1000L;
            if (elapsed < slowSeconds) {
                sender.sendMessage(TextUtil.safe(TextUtil.placeholders(
                        config.getString("messages.slowmode", "<red>Slow down."),
                        "{seconds}", String.valueOf(slowSeconds - elapsed))));
                return null;
            }
        }

        if (config.getBoolean("antispam.enabled", true) && !meta.spamBypass()) {
            SpamFilter.Fail fail = SpamFilter.check(sender, text,
                    config.getInt("antispam.max-messages", 4),
                    config.getInt("antispam.window-seconds", 5),
                    config.getInt("antispam.max-repeat-chars", 5),
                    config.getInt("antispam.caps-percent", 70),
                    config.getInt("antispam.caps-min-length", 8),
                    config.getBoolean("antispam.block-links", true),
                    config.getStringList("antispam.link-whitelist"));
            if (fail != null) {
                String key = switch (fail) {
                    case RATE -> "messages.spam-rate";
                    case REPEAT -> "messages.spam-repeat";
                    case CAPS -> "messages.spam-caps";
                    case LINK -> "messages.spam-link";
                };
                sender.sendMessage(TextUtil.safe(config.getString(key, "<red>Blocked by anti-spam.</red>")));
                return null;
            }
        }

        if (config.getBoolean("filter.enabled", true) && !meta.filterBypass()) {
            List<String> words = config.getStringList("filter.words");
            boolean hit = false;
            String lower = text.toLowerCase();
            for (String word : words) {
                if (!word.isBlank() && lower.contains(word.toLowerCase())) {
                    hit = true;
                    break;
                }
            }
            if (hit) {
                if (config.getString("filter.action", "replace").equalsIgnoreCase("cancel")) {
                    sender.sendMessage(TextUtil.safe(config.getString("messages.filtered", "<red>Blocked by filter.</red>")));
                    return null;
                }
                String replacement = config.getString("filter.replacement", "***");
                for (String word : words) {
                    if (word.isBlank()) {
                        continue;
                    }
                    text = text.replaceAll("(?i)" + Pattern.quote(word), Matcher.quoteReplacement(replacement));
                }
            }
        }
        plugin.markChat(id, channel);

        boolean mentionsOn = config.getBoolean("mentions.enabled", true);
        Set<Audience> viewers = viewersFor(sender, channel);
        List<Player> candidates = new ArrayList<>();
        for (Audience audience : viewers) {
            if (audience instanceof Player player) {
                candidates.add(player);
            }
        }

        List<Mention> hits = new ArrayList<>();
        boolean pingEveryone = false;
        if (mentionsOn) {
            Map<String, PlayerNamed> byName = new HashMap<>();
            PlayerNamed self = new PlayerNamed(sender);
            for (Player candidate : candidates) {
                byName.put(candidate.getName().toLowerCase(), new PlayerNamed(candidate));
            }
            for (MentionParser.Span span : MentionParser.findSpans(text)) {
                if (MentionParser.isBroadcastToken(span.token())) {
                    if (meta.mentionEveryone()) {
                        pingEveryone = true;
                    }
                    continue;
                }
                PlayerNamed resolved = MentionParser.resolve(span.token(), self, byName.values());
                if (resolved != null) {
                    Player target = resolved.player();
                    boolean duplicate = false;
                    for (Mention existing : hits) {
                        if (existing.target().equals(target)) {
                            duplicate = true;
                            break;
                        }
                    }
                    if (!duplicate) {
                        hits.add(new Mention(span, target));
                    }
                }
            }
        }

        Component messageComponent = buildMessage(text, hits, meta.color(), config);
        Component nameComponent = plugin.group(id).nameComponent(sender.getName())
                .hoverEvent(HoverEvent.showText(hoverCard(sender, channel, meta)));
        Component tagComponent = TextUtil.safe(config.getString("channels." + channel.key() + ".tag", ""));
        String format = TextUtil.bracesToTags(
                config.getString("channels." + channel.key() + ".format", "{tag}{name}: {message}"));

        Component tagFinal = tagComponent;
        Component nameFinal = nameComponent;
        Component messageFinal = messageComponent;
        Component fallbackLine = Component.text().append(tagFinal).append(nameFinal)
                .append(Component.text(": ")).append(messageFinal).build();
        TagResolver tagR = TagResolver.resolver("tag", Tag.inserting(tagFinal));
        TagResolver nameR = TagResolver.resolver("name", Tag.inserting(nameFinal));
        TagResolver messageR = TagResolver.resolver("message", Tag.inserting(messageFinal));
        Function<Audience, Component> line = viewer -> TextUtil.render(format, fallbackLine, tagR, nameR, messageR);

        List<Player> pings = new ArrayList<>();
        for (Mention hit : hits) {
            pings.add(hit.target());
        }
        if (pingEveryone) {
            for (Player candidate : candidates) {
                if (!candidate.getUniqueId().equals(id) && !pings.contains(candidate)) {
                    pings.add(candidate);
                }
            }
        }
        return new Result(line, viewers, pings);
    }

    private Set<Audience> viewersFor(Player sender, ChatChannel channel) {
        Set<Audience> viewers = new HashSet<>();
        switch (channel) {
            case GLOBAL -> viewers.addAll(Bukkit.getOnlinePlayers());
            case LOCAL -> {
                double radius = plugin.getConfig().getDouble("channels.local.radius", 50.0);
                viewers.addAll(sender.getWorld().getNearbyPlayers(sender.getLocation(), radius));
                viewers.add(sender);
            }
            case STAFF -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (plugin.isStaff(player.getUniqueId())) {
                        viewers.add(player);
                    }
                }
            }
        }
        viewers.add(Bukkit.getConsoleSender());
        return viewers;
    }

    private Component buildMessage(String text, List<Mention> mentions, boolean allowColor, FileConfiguration config) {
        String highlight = config.getString("mentions.highlight", "<yellow><bold>@{name}</bold></yellow>");
        List<Mention> ordered = new ArrayList<>(mentions);
        ordered.sort(Comparator.comparingInt(m -> m.span().start()));
        TextComponent.Builder out = Component.text();
        int index = 0;
        for (Mention mention : ordered) {
            int start = mention.span().start();
            int end = mention.span().end();
            if (start > index) {
                out.append(segment(text.substring(index, start), allowColor));
            }
            out.append(TextUtil.safe(highlight.replace("{name}", mention.target().getName())));
            index = end;
        }
        if (index < text.length()) {
            out.append(segment(text.substring(index), allowColor));
        }
        return out.build();
    }

    private Component segment(String text, boolean allowColor) {
        return allowColor ? TextUtil.safe(text) : Component.text(text);
    }

    private Component hoverCard(Player sender, ChatChannel channel, ForgeChat.PlayerMeta meta) {
        List<String> lines = plugin.getConfig().getStringList("hover-format");
        TextComponent.Builder out = Component.text();
        String session = TextUtil.formatDuration(plugin.sessionSeconds(sender.getUniqueId()));
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                out.append(Component.newline());
            }
            out.append(TextUtil.safe(TextUtil.placeholders(lines.get(i),
                    "{name}", sender.getName(),
                    "{group}", plugin.group(sender.getUniqueId()).label(),
                    "{channel}", channel.display(),
                    "{session}", session)));
        }
        return out.build();
    }
}
