package com.forge.chat.commands;

import com.forge.chat.ForgeChat;
import com.forge.chat.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class MsgCommand implements CommandExecutor, TabCompleter {

    private final ForgeChat plugin;

    public MsgCommand(ForgeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.safe(plugin.getConfig().getString("messages.player-only", "<red>Players only.</red>")));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(TextUtil.safe("<red>Usage: /" + label + " <player> <message></red>"));
            return true;
        }
        Player target = plugin.findPlayer(args[0]);
        if (target == null) {
            player.sendMessage(TextUtil.safe(TextUtil.placeholders(
                    plugin.getConfig().getString("messages.player-not-found", "<red>Player not found.</red>"),
                    "{name}", args[0])));
            return true;
        }
        send(player, target, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        return true;
    }

    /** Shared by /msg and /r. */
    public void send(Player from, Player to, String text) {
        if (plugin.mutes().isMuted(from.getUniqueId())) {
            from.sendMessage(TextUtil.safe(TextUtil.placeholders(
                    plugin.getConfig().getString("messages.muted-pm", "<red>You are muted and cannot send private messages.</red>"),
                    "{remaining}", plugin.mutes().remaining(from.getUniqueId()))));
            return;
        }
        if (to.getUniqueId().equals(from.getUniqueId())) {
            from.sendMessage(TextUtil.safe("<red>You cannot message yourself.</red>"));
            return;
        }
        if (plugin.data().isIgnoring(to.getUniqueId(), from.getUniqueId())) {
            from.sendMessage(TextUtil.safe(plugin.getConfig().getString("messages.pm-ignored",
                    "<red>That player is not accepting private messages.</red>")));
            return;
        }
        boolean color = plugin.meta(from.getUniqueId()).color();
        Component body = color ? TextUtil.safe(text) : Component.text(text);
        Component fromName = plugin.group(from.getUniqueId()).nameComponent(from.getName());
        Component toName = plugin.group(to.getUniqueId()).nameComponent(to.getName());
        TagResolver senderR = TagResolver.resolver("sender", Tag.inserting(fromName));
        TagResolver recipientR = TagResolver.resolver("recipient", Tag.inserting(toName));
        TagResolver messageR = TagResolver.resolver("message", Tag.inserting(body));

        to.sendMessage(TextUtil.render(
                TextUtil.bracesToTags(plugin.getConfig().getString("pm.format-to", "<gray>[<red>PM</red>]</gray> {sender}: {message}")),
                Component.text().append(fromName).append(Component.text(": ")).append(body).build(),
                senderR, messageR));
        from.sendMessage(TextUtil.render(
                TextUtil.bracesToTags(plugin.getConfig().getString("pm.format-from", "<gray>[<red>PM</red>]</gray> to {recipient}: {message}")),
                Component.text().append(Component.text("to ")).append(toName).append(Component.text(": ")).append(body).build(),
                recipientR, messageR));

        Component spyLine = TextUtil.render(
                TextUtil.bracesToTags(plugin.getConfig().getString("pm.format-spy", "<dark_gray>[SPY] {sender} -> {recipient}: {message}</dark_gray>")),
                Component.text().append(fromName).append(Component.text(" -> ")).append(toName)
                        .append(Component.text(": ")).append(body).build(),
                senderR, recipientR, messageR);
        for (UUID spyId : plugin.data().spies()) {
            if (spyId.equals(from.getUniqueId()) || spyId.equals(to.getUniqueId())) {
                continue;
            }
            Player spy = Bukkit.getPlayer(spyId);
            if (spy != null && spy.isOnline() && spy.hasPermission("forgechat.spy")) {
                spy.sendMessage(spyLine);
            }
        }
        plugin.data().replyTarget(from.getUniqueId(), to.getUniqueId());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return TabUtil.players(args[0]);
        }
        return List.of();
    }
}
