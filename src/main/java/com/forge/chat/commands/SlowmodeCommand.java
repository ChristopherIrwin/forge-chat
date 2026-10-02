package com.forge.chat.commands;

import com.forge.chat.ChatChannel;
import com.forge.chat.ForgeChat;
import com.forge.chat.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/** /slowmode <channel> <seconds> — 0 disables. Persists to config. */
public final class SlowmodeCommand implements CommandExecutor, TabCompleter {

    private final ForgeChat plugin;

    public SlowmodeCommand(ForgeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(TextUtil.safe("<red>Usage: /" + label + " <global|local|staff> <seconds></red>"));
            return true;
        }
        ChatChannel channel = ChatChannel.fromString(args[0]);
        if (channel == null) {
            sender.sendMessage(TextUtil.safe("<red>Unknown channel. Use global, local or staff.</red>"));
            return true;
        }
        long seconds;
        try {
            seconds = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(TextUtil.safe("<red>Seconds must be a number.</red>"));
            return true;
        }
        if (seconds < 0) {
            sender.sendMessage(TextUtil.safe("<red>Seconds cannot be negative.</red>"));
            return true;
        }
        plugin.setSlowmode(channel, seconds);
        if (seconds == 0) {
            sender.sendMessage(TextUtil.safe(TextUtil.placeholders(
                    plugin.getConfig().getString("messages.slowmode-off", "<green>Slow mode disabled for {channel}.</green>"),
                    "{channel}", channel.display())));
        } else {
            sender.sendMessage(TextUtil.safe(TextUtil.placeholders(
                    plugin.getConfig().getString("messages.slowmode-on", "<green>Slow mode for {channel}: {seconds}s.</green>"),
                    "{channel}", channel.display(),
                    "{seconds}", String.valueOf(seconds))));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            String partial = args[0].toLowerCase();
            for (ChatChannel channel : ChatChannel.values()) {
                if (channel.key().startsWith(partial)) {
                    out.add(channel.key());
                }
            }
            return out;
        }
        return List.of();
    }
}
