package com.forge.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;

public final class ChatListener implements Listener {

    private final ForgeChat plugin;

    public ChatListener(ForgeChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String text = TextUtil.plain(event.message());
        ChatChannel channel = plugin.data().channel(player.getUniqueId());
        ChatPipeline.Result result = plugin.pipeline().process(player, channel, text);
        if (result == null) {
            event.setCancelled(true);
            return;
        }
        Set<Audience> viewers = event.viewers();
        viewers.clear();
        viewers.addAll(result.viewers());
        event.renderer((source, sourceDisplayName, message, viewer) -> result.line().apply(viewer));
        plugin.pingPlayers(result.pings());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.playerJoined(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.playerQuit(event.getPlayer());
    }
}
