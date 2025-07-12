package com.example.paperwhitelist.util;

import com.example.paperwhitelist.PaperWhitelistPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public class CommandExecutor {
    private static final CommandSender consoleSender = Bukkit.getConsoleSender();

    public static void executeWhitelistAdd(PaperWhitelistPlugin plugin, String playerName) {
        executeCommand(plugin, "whitelist add " + playerName);
    }

    public static void executeWhitelistRemove(PaperWhitelistPlugin plugin, String playerName) {
        executeCommand(plugin, "whitelist remove " + playerName);
    }

    public static void executeWhitelistReload(PaperWhitelistPlugin plugin) {
        executeCommand(plugin, "whitelist reload");
    }

    private static void executeCommand(PaperWhitelistPlugin plugin, String command) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    plugin.getLogger().info("Executing command: " + command);
                    Bukkit.dispatchCommand(consoleSender, command);
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to execute command: " + command + " - " + e.getMessage());
                }
            }
        }.runTask(plugin);
    }
}