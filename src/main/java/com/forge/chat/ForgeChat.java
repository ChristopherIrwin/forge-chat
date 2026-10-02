package com.forge.chat;

import com.forge.chat.commands.ChannelCommand;
import com.forge.chat.commands.ChatAdminCommand;
import com.forge.chat.commands.IgnoreCommand;
import com.forge.chat.commands.MsgCommand;
import com.forge.chat.commands.MuteCommand;
import com.forge.chat.commands.ReplyCommand;
import com.forge.chat.commands.SlowmodeCommand;
import com.forge.chat.commands.SpyCommand;
import com.forge.chat.commands.TempmuteCommand;
import com.forge.chat.commands.UnignoreCommand;
import com.forge.chat.commands.UnmuteCommand;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ForgeChat — all-in-one chat: channels, private messages, mentions,
 * moderation, anti-spam and rich MiniMessage formatting.
 */
public final class ForgeChat extends JavaPlugin {

    /** Cached per-player data. Computed on the main thread at join; read from async chat. */
    public record PlayerMeta(GroupFormat group, boolean staff, boolean spamBypass,
                             boolean filterBypass, boolean color, boolean mentionEveryone) {
    }

    private final Map<UUID, PlayerMeta> metas = new ConcurrentHashMap<>();
    private final Map<UUID, Long> joinTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Map<ChatChannel, Long>> lastChat = new ConcurrentHashMap<>();
    private final Map<ChatChannel, Long> slowmodes = new ConcurrentHashMap<>();

    private MuteManager mutes;
    private DataStore data;
    private ChatPipeline pipeline;
    private List<GroupFormat> groups;
    private Sound ping = defaultPing();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        mutes = new MuteManager(getLogger());
        mutes.load(new File(getDataFolder(), "mutes.yml"));
        data = new DataStore(getLogger());
        data.load(new File(getDataFolder(), "data.yml"));
        pipeline = new ChatPipeline(this);
        reloadLocal();

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        ChannelCommand channelCommand = new ChannelCommand(this);
        command("ch", channelCommand);
        command("g", channelCommand);
        command("l", channelCommand);
        command("staff", channelCommand);
        MsgCommand msgCommand = new MsgCommand(this);
        command("msg", msgCommand);
        ReplyCommand replyCommand = new ReplyCommand(this);
        command("r", replyCommand);
        IgnoreCommand ignoreCommand = new IgnoreCommand(this);
        command("ignore", ignoreCommand);
        UnignoreCommand unignoreCommand = new UnignoreCommand(this);
        command("unignore", unignoreCommand);
        command("spy", new SpyCommand(this));
        MuteCommand muteCommand = new MuteCommand(this);
        command("mute", muteCommand);
        TempmuteCommand tempmuteCommand = new TempmuteCommand(this);
        command("tempmute", tempmuteCommand);
        UnmuteCommand unmuteCommand = new UnmuteCommand(this);
        command("unmute", unmuteCommand);
        SlowmodeCommand slowmodeCommand = new SlowmodeCommand(this);
        command("slowmode", slowmodeCommand);
        command("fchat", new ChatAdminCommand(this));

        for (Player player : Bukkit.getOnlinePlayers()) {
            playerJoined(player);
        }
        getServer().getScheduler().runTaskTimer(this, this::saveAll, 20L * 300, 20L * 300);
        getLogger().info("ForgeChat 1.0.0 enabled.");
    }

    @Override
    public void onDisable() {
        saveAll();
    }

    private void command(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("[ForgeChat] Command not defined in plugin.yml: " + name);
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof org.bukkit.command.TabCompleter completer) {
            command.setTabCompleter(completer);
        }
    }

    /** Reload config-derived state (groups, slowmodes, mention sound). */
    public void reloadLocal() {
        reloadConfig();
        groups = GroupFormat.load(getConfig().getMapList("groups"));
        slowmodes.clear();
        var section = getConfig().getConfigurationSection("slowmode");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ChatChannel channel = ChatChannel.fromString(key);
                if (channel != null) {
                    slowmodes.put(channel, Math.max(0, section.getLong(key)));
                }
            }
        }
        ping = defaultPing();
        String soundKey = getConfig().getString("mentions.sound", "entity.experience_orb_pickup");
        float volume = (float) getConfig().getDouble("mentions.volume", 1.0);
        float pitch = (float) getConfig().getDouble("mentions.pitch", 1.6);
        try {
            ping = Sound.sound(Key.key(soundKey), Sound.Source.PLAYER, volume, pitch);
        } catch (Exception e) {
            getLogger().warning("[ForgeChat] Invalid mention sound key '" + soundKey + "', using default.");
        }
        metas.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            cacheMeta(player);
        }
    }

    /** Default mention ping, used until config is loaded. */
    private static Sound defaultPing() {
        return Sound.sound(Key.key("entity.experience_orb_pickup"), Sound.Source.PLAYER, 1.0f, 1.6f);
    }

    public void saveAll() {
        if (mutes != null) {
            mutes.save(new File(getDataFolder(), "mutes.yml"));
        }
        if (data != null) {
            data.save(new File(getDataFolder(), "data.yml"));
        }
    }

    public MuteManager mutes() {
        return mutes;
    }

    public DataStore data() {
        return data;
    }

    public ChatPipeline pipeline() {
        return pipeline;
    }

    /** Play the mention ping for targets on the main thread. */
    public void pingPlayers(List<Player> targets) {
        if (targets.isEmpty()) {
            return;
        }
        List<Player> copy = List.copyOf(targets);
        Sound sound = ping;
        Bukkit.getScheduler().runTask(this, () -> {
            for (Player player : copy) {
                if (player.isOnline()) {
                    player.playSound(sound);
                }
            }
        });
    }

    /** Find an online player by exact name, falling back to a unique prefix match. */
    public @Nullable Player findPlayer(String input) {
        Player exact = Bukkit.getPlayerExact(input);
        if (exact != null) {
            return exact;
        }
        String lower = input.toLowerCase();
        Player prefix = null;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(lower)) {
                if (prefix != null) {
                    return null;
                }
                prefix = player;
            }
        }
        return prefix;
    }

    public void playerJoined(Player player) {
        joinTimes.put(player.getUniqueId(), System.currentTimeMillis());
        cacheMeta(player);
    }

    public void playerQuit(Player player) {
        UUID id = player.getUniqueId();
        joinTimes.remove(id);
        metas.remove(id);
        lastChat.remove(id);
        data.forget(id);
        saveAll();
    }

    public void cacheMeta(Player player) {
        GroupFormat group = groups.get(groups.size() - 1);
        for (GroupFormat candidate : groups) {
            String permission = candidate.permission();
            if (permission.isEmpty() || player.hasPermission(permission)) {
                group = candidate;
                break;
            }
        }
        metas.put(player.getUniqueId(), new PlayerMeta(
                group,
                player.hasPermission("forgechat.staff") || player.isOp(),
                player.hasPermission("forgechat.spam.bypass") || player.isOp(),
                player.hasPermission("forgechat.filter.bypass") || player.isOp(),
                player.hasPermission("forgechat.color"),
                player.hasPermission("forgechat.mention.everyone") || player.isOp()));
    }

    public PlayerMeta meta(UUID id) {
        PlayerMeta meta = metas.get(id);
        if (meta != null) {
            return meta;
        }
        Player player = Bukkit.getPlayer(id);
        if (player != null) {
            cacheMeta(player);
            return metas.get(id);
        }
        GroupFormat fallback = groups.get(groups.size() - 1);
        return new PlayerMeta(fallback, false, false, false, false, false);
    }

    public GroupFormat group(UUID id) {
        return meta(id).group();
    }

    public boolean isStaff(UUID id) {
        return meta(id).staff();
    }

    public long slowmode(ChatChannel channel) {
        return slowmodes.getOrDefault(channel, 0L);
    }

    public void setSlowmode(ChatChannel channel, long seconds) {
        slowmodes.put(channel, Math.max(0, seconds));
        getConfig().set("slowmode." + channel.key(), Math.max(0, seconds));
        saveConfig();
    }

    public long lastChat(UUID id, ChatChannel channel) {
        Map<ChatChannel, Long> map = lastChat.get(id);
        return map == null ? 0L : map.getOrDefault(channel, 0L);
    }

    public void markChat(UUID id, ChatChannel channel) {
        lastChat.computeIfAbsent(id, k -> new ConcurrentHashMap<>()).put(channel, System.currentTimeMillis());
    }

    public long sessionSeconds(UUID id) {
        Long joined = joinTimes.get(id);
        return joined == null ? 0L : (System.currentTimeMillis() - joined) / 1000L;
    }
}
