package com.forge.chat.commands;

import com.forge.chat.ForgeChat;
import com.forge.chat.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Toggle social-spy: see everyone's private messages. */
public final class SpyCommand implements CommandExecutor {

    private final ForgeChat plugin;

    public SpyCommand(ForgeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.safe(plugin.getConfig().getString("messages.player-only", "<red>Players only.</red>")));
            return true;
        }
        boolean on = !plugin.data().isSpy(player.getUniqueId());
        plugin.data().setSpy(player.getUniqueId(), on);
        player.sendMessage(TextUtil.safe(on
                ? plugin.getConfig().getString("messages.spy-on", "<green>Social-spy enabled.</green>")
                : plugin.getConfig().getString("messages.spy-off", "<yellow>Social-spy disabled.</yellow>")));
        return true;
    }
}
