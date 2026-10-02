package com.forge.chat;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * Player social state persisted to data.yml: ignores, social-spy toggles and
 * default chat channels. Thread-safe.
 */
public final class DataStore {

    private final Set<UUID> spies = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Set<UUID>> ignores = new ConcurrentHashMap<>();
    private final Map<UUID, ChatChannel> channels = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> replyTo = new ConcurrentHashMap<>();
    private final Logger log;

    public DataStore(Logger log) {
        this.log = log;
    }

    public boolean isSpy(UUID id) {
        return spies.contains(id);
    }

    public void setSpy(UUID id, boolean on) {
        if (on) {
            spies.add(id);
        } else {
            spies.remove(id);
        }
    }

    public Set<UUID> spies() {
        return spies;
    }

    public boolean isIgnoring(UUID player, UUID target) {
        Set<UUID> set = ignores.get(player);
        return set != null && set.contains(target);
    }

    public boolean ignore(UUID player, UUID target) {
        return ignores.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).add(target);
    }

    public boolean unignore(UUID player, UUID target) {
        Set<UUID> set = ignores.get(player);
        return set != null && set.remove(target);
    }

    public ChatChannel channel(UUID id) {
        return channels.getOrDefault(id, ChatChannel.GLOBAL);
    }

    public void channel(UUID id, ChatChannel channel) {
        channels.put(id, channel);
    }

    public @Nullable UUID replyTarget(UUID id) {
        return replyTo.get(id);
    }

    public void replyTarget(UUID first, UUID second) {
        replyTo.put(first, second);
        replyTo.put(second, first);
    }

    public void forget(UUID id) {
        replyTo.remove(id);
    }

    public void load(File file) {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String entry : yaml.getStringList("spies")) {
            uuid(entry).ifPresent(spies::add);
        }
        var ignoreSection = yaml.getConfigurationSection("ignores");
        if (ignoreSection != null) {
            for (String key : ignoreSection.getKeys(false)) {
                uuid(key).ifPresent(id -> {
                    Set<UUID> set = ConcurrentHashMap.newKeySet();
                    for (String target : ignoreSection.getStringList(key)) {
                        uuid(target).ifPresent(set::add);
                    }
                    ignores.put(id, set);
                });
            }
        }
        var channelSection = yaml.getConfigurationSection("channels");
        if (channelSection != null) {
            for (String key : channelSection.getKeys(false)) {
                uuid(key).ifPresent(id -> {
                    ChatChannel channel = ChatChannel.fromString(channelSection.getString(key));
                    if (channel != null) {
                        channels.put(id, channel);
                    }
                });
            }
        }
    }

    public void save(File file) {
        YamlConfiguration yaml = new YamlConfiguration();
        List<String> spyList = new ArrayList<>();
        spies.forEach(id -> spyList.add(id.toString()));
        yaml.set("spies", spyList);
        ignores.forEach((id, set) -> {
            List<String> list = new ArrayList<>();
            set.forEach(target -> list.add(target.toString()));
            yaml.set("ignores." + id, list);
        });
        channels.forEach((id, channel) -> yaml.set("channels." + id, channel.key()));
        try {
            yaml.save(file);
        } catch (IOException e) {
            log.warning("[ForgeChat] Could not save data.yml: " + e.getMessage());
        }
    }

    private static Optional<UUID> uuid(String input) {
        try {
            return Optional.of(UUID.fromString(input));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
