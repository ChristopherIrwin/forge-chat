package com.forge.chat.commands;

import com.forge.chat.ForgeChat;
import com.forge.chat.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** /fchat reload */
public final class ChatAdminCommand implements CommandExecutor {

    private final ForgeChat plugin;

    public ChatAdminCommand(ForgeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(TextUtil.safe("<red>Usage: /" + label + " reload</red>"));
            return true;
        }
        plugin.reloadLocal();
        sender.sendMessage(TextUtil.safe(plugin.getConfig().getString("messages.reloaded",
                "<green>ForgeChat configuration reloaded.</green>")));
        return true;
    }
}
