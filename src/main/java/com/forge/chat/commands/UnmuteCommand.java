package com.forge.chat.commands;

import com.forge.chat.ForgeChat;
import com.forge.chat.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class UnmuteCommand implements CommandExecutor, TabCompleter {

    private final ForgeChat plugin;

    public UnmuteCommand(ForgeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(TextUtil.safe("<red>Usage: /" + label + " <player></red>"));
            return true;
        }
        Player target = plugin.findPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(TextUtil.safe(TextUtil.placeholders(
                    plugin.getConfig().getString("messages.player-not-found", "<red>Player not found.</red>"),
                    "{name}", args[0])));
            return true;
        }
        if (plugin.mutes().unmute(target.getUniqueId())) {
            plugin.saveAll();
            sender.sendMessage(TextUtil.safe(TextUtil.placeholders(
                    plugin.getConfig().getString("messages.unmuted", "<green>Unmuted {name}.</green>"),
                    "{name}", target.getName())));
            target.sendMessage(TextUtil.safe(plugin.getConfig().getString("messages.you-are-unmuted",
                    "<green>You have been unmuted.</green>")));
        } else {
            sender.sendMessage(TextUtil.safe(TextUtil.placeholders(
                    plugin.getConfig().getString("messages.not-muted", "<yellow>{name} is not muted.</yellow>"),
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
