package com.forge.chat.commands;

import com.forge.chat.ForgeChat;
import com.forge.chat.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class UnignoreCommand implements CommandExecutor, TabCompleter {

    private final ForgeChat plugin;

    public UnignoreCommand(ForgeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.safe(plugin.getConfig().getString("messages.player-only", "<red>Players only.</red>")));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(TextUtil.safe("<red>Usage: /" + label + " <player></red>"));
            return true;
        }
        Player target = plugin.findPlayer(args[0]);
        if (target == null) {
            player.sendMessage(TextUtil.safe(TextUtil.placeholders(
                    plugin.getConfig().getString("messages.player-not-found", "<red>Player not found.</red>"),
                    "{name}", args[0])));
            return true;
        }
        if (plugin.data().unignore(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(TextUtil.safe(TextUtil.placeholders(
                    plugin.getConfig().getString("messages.unignored", "<green>You are no longer ignoring {name}.</green>"),
                    "{name}", target.getName())));
        } else {
            player.sendMessage(TextUtil.safe(TextUtil.placeholders(
                    plugin.getConfig().getString("messages.not-ignored", "<yellow>You are not ignoring {name}.</yellow>"),
                    "{name}", target.getName())));
        }
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
