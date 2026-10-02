package com.forge.chat.commands;

import com.forge.chat.ForgeChat;
import com.forge.chat.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;

public final class ReplyCommand implements CommandExecutor {

    private final ForgeChat plugin;

    public ReplyCommand(ForgeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.safe(plugin.getConfig().getString("messages.player-only", "<red>Players only.</red>")));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(TextUtil.safe("<red>Usage: /" + label + " <message></red>"));
            return true;
        }
        UUID targetId = plugin.data().replyTarget(player.getUniqueId());
        Player target = targetId == null ? null : Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            player.sendMessage(TextUtil.safe(plugin.getConfig().getString("messages.no-reply-target",
                    "<red>You have no one to reply to.</red>")));
            return true;
        }
        MsgCommand msg = new MsgCommand(plugin);
        msg.send(player, target, String.join(" ", args));
        return true;
    }
}
