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

/** /tempmute <player> <time> [reason...] — time is required. */
public final class TempmuteCommand implements CommandExecutor, TabCompleter {

    private final ForgeChat plugin;

    public TempmuteCommand(ForgeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(TextUtil.safe("<red>Usage: /" + label + " <player> <time> [reason...]</red>"));
            return true;
        }
        Player target = plugin.findPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(TextUtil.safe(TextUtil.placeholders(
                    plugin.getConfig().getString("messages.player-not-found", "<red>Player not found.</red>"),
                    "{name}", args[0])));
            return true;
        }
        long seconds = TextUtil.parseDuration(args[1]);
        if (seconds == -2 || seconds == 0) {
            sender.sendMessage(TextUtil.safe("<red>Invalid duration. Use e.g. 10m, 2h, 1d.</red>"));
            return true;
        }
        if (seconds < 0) {
            sender.sendMessage(TextUtil.safe("<red>Use /mute for permanent mutes.</red>"));
            return true;
        }
        String reason = args.length > 2
                ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : "No reason given";
        plugin.mutes().mute(target.getUniqueId(), seconds, reason);
        plugin.saveAll();
        sender.sendMessage(TextUtil.safe(TextUtil.placeholders(
                plugin.getConfig().getString("messages.muted-player", "<green>Muted {name} ({duration}): {reason}</green>"),
                "{name}", target.getName(),
                "{duration}", TextUtil.formatDuration(seconds),
                "{reason}", reason)));
        target.sendMessage(TextUtil.safe(TextUtil.placeholders(
                plugin.getConfig().getString("messages.you-are-muted", "<red>You have been muted ({duration}): {reason}</red>"),
                "{duration}", TextUtil.formatDuration(seconds),
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
