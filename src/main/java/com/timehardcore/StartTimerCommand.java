package com.timehardcore;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class StartTimerCommand implements CommandExecutor {

    private final TimeManager timeManager;
    private final JavaPlugin plugin;

    public StartTimerCommand(TimeManager timeManager, JavaPlugin plugin) {
        this.timeManager = timeManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cЦю команду можна використовувати тільки як гравець!");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        // Перевіряємо чи таймер вже запущений
        if (timeManager.isTimerRunning(uuid)) {
            player.sendMessage("§c⚠ Таймер вже запущений!");
            player.sendMessage("§eВаш час: §a" + formatTime(timeManager.getTime(uuid)));
            return true;
        }

        // Запускаємо таймер
        timeManager.startTimer(uuid);
        
        // Показуємо BossBar (потрібно мати доступ до TimeHardcore)
        if (plugin instanceof TimeHardcore) {
            TimeHardcore main = (TimeHardcore) plugin;
            main.createBossBar(player, uuid);
        }
        
        // Красивое повідомлення
        player.sendMessage("\n");
        player.sendMessage("§2§l╔════════════════════════════════════════╗");
        player.sendMessage("§2§l║ ✅ ТАЙМЕР ЗАПУЩЕНО! ✅ §r§2§l          ║");
        player.sendMessage("§2§l╠════════════════════════════════════════╣");
        player.sendMessage("§2§l║ §r§aТи маєш 15 годин на Hardcore!      §2§l║");
        player.sendMessage("§2§l║ §r§cБез смерті можеш досягти!           §2§l║");
        player.sendMessage("§2§l║ §r§eВ полосці вище видно прогрес      §2§l║");
        player.sendMessage("§2§l║ §r§6Тип /playtime для перегляду часу   §2§l║");
        player.sendMessage("§2§l╚════════════════════════════════════════╝");
        player.sendMessage("\n");

        Bukkit.broadcastMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage("§6[TimeHardcore] §e" + player.getName() + " §aзапустив таймер!");
        Bukkit.broadcastMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        plugin.getLogger().info("✅ Таймер запущено для гравця: " + player.getName());
        
        return true;
    }

    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("§a%d§7h §a%02d§7m §a%02d§7s", hours, minutes, secs);
    }
}
