package com.forge.chat;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * Timed and permanent mutes, persisted to mutes.yml.
 * Thread-safe: safe to query from the async chat thread.
 */
public final class MuteManager {

    public record Mute(long untilMillis, String reason) {
        public boolean permanent() {
            return untilMillis < 0;
        }

        public boolean expired() {
            return !permanent() && System.currentTimeMillis() >= untilMillis;
        }
    }

    private final ConcurrentHashMap<UUID, Mute> mutes = new ConcurrentHashMap<>();
    private final Logger log;

    public MuteManager(Logger log) {
        this.log = log;
    }

    public void load(File file) {
        mutes.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                long until = yaml.getLong(key + ".until", -1L);
                String reason = yaml.getString(key + ".reason", "No reason given");
                Mute mute = new Mute(until, reason);
                if (!mute.expired()) {
                    mutes.put(id, mute);
                }
            } catch (IllegalArgumentException e) {
                log.warning("[ForgeChat] Skipping invalid mute entry: " + key);
            }
        }
    }

    public void save(File file) {
        YamlConfiguration yaml = new YamlConfiguration();
        mutes.forEach((id, mute) -> {
            if (mute.expired()) {
                return;
            }
            yaml.set(id.toString() + ".until", mute.untilMillis());
            yaml.set(id.toString() + ".reason", mute.reason());
        });
        try {
            yaml.save(file);
        } catch (IOException e) {
            log.warning("[ForgeChat] Could not save mutes.yml: " + e.getMessage());
        }
    }

    /** Mute a player; seconds &lt; 0 means permanent. */
    public void mute(UUID id, long seconds, String reason) {
        long until = seconds < 0 ? -1L : System.currentTimeMillis() + seconds * 1000L;
        mutes.put(id, new Mute(until, reason));
    }

    public boolean unmute(UUID id) {
        return mutes.remove(id) != null;
    }

    /** Lazily expires timed mutes. */
    public @Nullable Mute get(UUID id) {
        Mute mute = mutes.get(id);
        if (mute != null && mute.expired()) {
            mutes.remove(id);
            return null;
        }
        return mute;
    }

    public boolean isMuted(UUID id) {
        return get(id) != null;
    }

    public String remaining(UUID id) {
        Mute mute = get(id);
        if (mute == null) {
            return "0s";
        }
        if (mute.permanent()) {
            return "permanent";
        }
        return TextUtil.formatDuration((mute.untilMillis() - System.currentTimeMillis()) / 1000L);
    }
}
