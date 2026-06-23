package com.timehardcore;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlaytimeCommand implements CommandExecutor {

    private final TimeManager timeManager;
    private final long hardcoreThreshold;

    public PlaytimeCommand(TimeManager timeManager, long hardcoreThreshold) {
        this.timeManager = timeManager;
        this.hardcoreThreshold = hardcoreThreshold;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cЦю команду можна використовувати тільки як гравець!");
                return true;
            }
            Player player = (Player) sender;
            showTime(sender, player.getUniqueId(), player.getName());
        } else {
            if (!sender.hasPermission("timehardcore.admin")) {
                sender.sendMessage("§cНемає прав! (timehardcore.admin)");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cГравець §e" + args[0] + " §cне знайдений або не онлайн.");
                return true;
            }
            showTime(sender, target.getUniqueId(), target.getName());
        }
        return true;
    }

    private void showTime(CommandSender sender, UUID uuid, String name) {
        long totalSeconds = timeManager.getTime(uuid);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        boolean isHardcore = timeManager.isHardcore(uuid);
        boolean timerRunning = timeManager.isTimerRunning(uuid);
        
        long remaining = hardcoreThreshold - totalSeconds;
        long remHours = remaining / 3600;
        long remMinutes = (remaining % 3600) / 60;

        sender.sendMessage("\n");
        sender.sendMessage("§6§l╔════════════════════════════════════════╗");
        sender.sendMessage("§6§l║ ⏱ TimeHardcore - Інформація ⏱ §r§6§l  ║");
        sender.sendMessage("§6§l╠════════════════════════════════════════╣");
        sender.sendMessage("§6§l║ §r§eГравець: §a" + padString(name, 31));
        sender.sendMessage("§6§l║ §r");
        sender.sendMessage("§6§l║ §r§eЧас гри: §a" + String.format("%02d", hours) + " §eгод §b" + String.format("%02d", minutes) + " §eхв §c" + String.format("%02d", seconds) + " §eсек");
        sender.sendMessage("§6§l║ §r");
        
        if (timerRunning) {
            sender.sendMessage("§6§l║ §r§6Таймер: §a✓ Запущено");
            sender.sendMessage("§6§l║ " + createProgressBar(totalSeconds, hardcoreThreshold));
        } else {
            sender.sendMessage("§6§l║ §r§6Таймер: §c✗ Не запущено");
        }

        sender.sendMessage("§6§l║ §r");
        
        if (isHardcore) {
            sender.sendMessage("§6§l║ §r§4§l☠ Статус: HARDCORE РЕЖИМ! ☠");
            sender.sendMessage("§6§l║ §r§c⚠ СМЕРТЬ = ПОСТІЙНИЙ БАН!");
        } else {
            sender.sendMessage("§6§l║ §r§aСтатус: §2Звичайний режим");
            sender.sendMessage("§6§l║ §r§eДо Hardcore: §c" + remHours + " §eгод §b" + remMinutes + " §eхв");
        }
        
        sender.sendMessage("§6§l╚════════════════════════════════════════╝");
        sender.sendMessage("\n");
    }

    private String createProgressBar(long current, long max) {
        double progress = Math.min((double) current / max, 1.0);
        int barLength = 30;
        int filledLength = (int) (barLength * progress);
        
        StringBuilder bar = new StringBuilder("§6§l║ §r§l");
        
        for (int i = 0; i < barLength; i++) {
            if (i < filledLength) {
                if (progress >= 0.95) {
                    bar.append("§4");
                } else if (progress >= 0.85) {
                    bar.append("§c");
                } else if (progress >= 0.70) {
                    bar.append("§6");
                } else if (progress >= 0.50) {
                    bar.append("§e");
                } else {
                    bar.append("§a");
                }
                bar.append("█");
            } else {
                bar.append("§7░");
            }
        }
        
        bar.append(" §r§e").append(String.format("%.0f%%", progress * 100)).append(" §6§l║");
        return bar.toString();
    }

    private String padString(String str, int length) {
        if (str.length() >= length) return str;
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < length) {
            sb.append(" ");
        }
        return sb.toString();
    }
}
