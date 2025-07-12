package com.example.paperwhitelist.command;

import com.example.paperwhitelist.PaperWhitelistPlugin;
import com.example.paperwhitelist.util.PasswordUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WhitelistWebCommand implements CommandExecutor, TabCompleter {
    private final PaperWhitelistPlugin plugin;

    public WhitelistWebCommand(PaperWhitelistPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("paperwhitelist.admin")) {
            sender.sendMessage("§c你没有权限执行此命令！");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                reloadPlugin(sender);
                break;
            case "hash":
                generatePasswordHash(sender, args);
                break;
            case "status":
                showStatus(sender);
                break;
            default:
                sender.sendMessage("§c未知命令，请使用 /whitelistweb help 查看帮助");
        }

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§6===== §e白名单网页管理插件 §6=====");
        sender.sendMessage("§b/whitelistweb reload §7- 重新加载插件配置");
        sender.sendMessage("§b/whitelistweb hash <密码> §7- 生成密码哈希值");
        sender.sendMessage("§b/whitelistweb status §7- 显示插件状态信息");
        sender.sendMessage("§b/whitelistweb help §7- 显示帮助信息");
    }

    private void reloadPlugin(CommandSender sender) {
        try {
            plugin.getConfigManager().loadConfig();
            sender.sendMessage("§a配置文件已重新加载！");
        } catch (Exception e) {
            sender.sendMessage("§c重新加载配置文件失败: " + e.getMessage());
            plugin.getLogger().warning("配置文件重新加载失败: " + e.getMessage());
        }
    }

    private void generatePasswordHash(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /whitelistweb hash <密码>");
            return;
        }

        String password = args[1];
        String hash = PasswordUtil.hashPassword(password);
        
        sender.sendMessage("§6密码哈希值已生成:");
        sender.sendMessage("§e" + hash);
        sender.sendMessage("§7请将此哈希值复制到 config.yml 中的 admin.password-hash 字段");
    }

    private void showStatus(CommandSender sender) {
        sender.sendMessage("§6===== §e插件状态 §6=====");
        sender.sendMessage("§b版本: §7" + plugin.getDescription().getVersion());
        sender.sendMessage("§bWeb服务器: §7" + (plugin.getServer().getPluginManager().isPluginEnabled(plugin) ? "运行中" : "已停止"));
        sender.sendMessage("§b端口: §7" + plugin.getConfigManager().getInt("web.port", 8080));
        sender.sendMessage("§b数据库: §7" + plugin.getConfigManager().getString("database.file", "whitelist.db"));
        sender.sendMessage("§b待处理申请: §7" + plugin.getDatabaseManager().getApplicationsByStatus("PENDING").size());
        sender.sendMessage("§b白名单总数: §7" + plugin.getDatabaseManager().getAllWhitelistEntries().size());
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("reload");
            completions.add("hash");
            completions.add("status");
            completions.add("help");
        }
        
        return completions;
    }
}