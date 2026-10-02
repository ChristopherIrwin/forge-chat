package com.forge.chat.commands;

import com.forge.chat.ForgeChat;
import com.forge.chat.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/** /mute <player> [time] [reason...] — time optional, defaults to permanent. */
public final class MuteCommand implements CommandExecutor, TabCompleter {

    private final ForgeChat plugin;

    public MuteCommand(ForgeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(TextUtil.safe("<red>Usage: /" + label + " <player> [time] [reason...]</red>"));
            return true;
        }
        Player target = plugin.findPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(TextUtil.safe(TextUtil.placeholders(
                    plugin.getConfig().getString("messages.player-not-found", "<red>Player not found.</red>"),
                    "{name}", args[0])));
            return true;
        }
        long seconds = -1;
        int reasonStart = 1;
        if (args.length >= 2) {
            long parsed = TextUtil.parseDuration(args[1]);
            if (parsed != -2) {
                seconds = parsed;
                reasonStart = 2;
            }
        }
        String reason = args.length > reasonStart
                ? String.join(" ", Arrays.copyOfRange(args, reasonStart, args.length))
                : "No reason given";
        plugin.mutes().mute(target.getUniqueId(), seconds, reason);
        plugin.saveAll();
        sender.sendMessage(TextUtil.safe(TextUtil.placeholders(
                plugin.getConfig().getString("messages.muted-player", "<green>Muted {name} ({duration}): {reason}</green>"),
                "{name}", target.getName(),
                "{duration}", seconds < 0 ? "permanent" : TextUtil.formatDuration(seconds),
                "{reason}", reason)));
        target.sendMessage(TextUtil.safe(TextUtil.placeholders(
                plugin.getConfig().getString("messages.you-are-muted", "<red>You have been muted ({duration}): {reason}</red>"),
                "{duration}", seconds < 0 ? "permanent" : TextUtil.formatDuration(seconds),
                "{reason}", reason)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return TabUtil.players(args[0]);
        }
        return List.of();
    }
}
