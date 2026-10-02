package com.forge.chat.commands;

import com.forge.chat.ChatChannel;
import com.forge.chat.ChatPipeline;
import com.forge.chat.ForgeChat;
import com.forge.chat.TextUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Handles /ch (set default channel) and /g /l /staff (one-shot channel messages). */
public final class ChannelCommand implements CommandExecutor, TabCompleter {

    private final ForgeChat plugin;

    public ChannelCommand(ForgeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.safe(plugin.getConfig().getString("messages.player-only", "<red>Players only.</red>")));
            return true;
        }
        if (command.getName().equals("ch")) {
            if (args.length == 0) {
                player.sendMessage(TextUtil.safe("<gray>Current channel: <white>"
                        + plugin.data().channel(player.getUniqueId()).display()
                        + "</white>. Use <white>/ch <global|local|staff></white>.</gray>"));
                return true;
            }
            ChatChannel channel = ChatChannel.fromString(args[0]);
            if (channel == null) {
                player.sendMessage(TextUtil.safe("<red>Unknown channel. Use global, local or staff.</red>"));
                return true;
            }
            if (channel == ChatChannel.STAFF && !plugin.meta(player.getUniqueId()).staff()) {
                player.sendMessage(TextUtil.safe(plugin.getConfig().getString("messages.no-permission", "<red>No permission.</red>")));
                return true;
            }
            plugin.data().channel(player.getUniqueId(), channel);
            player.sendMessage(TextUtil.safe("<green>Chat channel set to <white>" + channel.display() + "</white>.</green>"));
            return true;
        }

        ChatChannel channel = switch (command.getName()) {
            case "g" -> ChatChannel.GLOBAL;
            case "l" -> ChatChannel.LOCAL;
            default -> ChatChannel.STAFF;
        };
        if (args.length == 0) {
            player.sendMessage(TextUtil.safe("<red>Usage: /" + label + " <message></red>"));
            return true;
        }
        ChatPipeline.Result result = plugin.pipeline().process(player, channel, String.join(" ", args));
        if (result == null) {
            return true;
        }
        Function<Audience, Component> line = result.line();
        for (Audience audience : result.viewers()) {
            audience.sendMessage(line.apply(audience));
        }
        plugin.pingPlayers(result.pings());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equals("ch") && args.length == 1) {
            List<String> out = new ArrayList<>();
            String partial = args[0].toLowerCase();
            for (ChatChannel channel : ChatChannel.values()) {
                if (channel == ChatChannel.STAFF && sender instanceof Player player
                        && !plugin.meta(player.getUniqueId()).staff()) {
                    continue;
                }
                if (channel.key().startsWith(partial)) {
                    out.add(channel.key());
                }
            }
            return out;
        }
        return List.of();
    }
}
