package com.forge.chat.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Shared player-name tab completion. */
public final class TabUtil {
    private TabUtil() {
    }

    public static List<String> players(String partial) {
        String lower = partial.toLowerCase();
        List<String> out = new java.util.ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(lower)) {
                out.add(player.getName());
            }
        }
        return out;
    }
}
