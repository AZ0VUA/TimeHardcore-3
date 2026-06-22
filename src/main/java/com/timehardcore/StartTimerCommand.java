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
            main.createBossBar(player);
        }
        
        player.sendMessage("§2✅ Таймер запущено!");
        player.sendMessage("§e⏱ Ви маєте 15 годин щоб набрати максимум часу!");
        player.sendMessage("§c☠ При досягненні 15 годин - Hardcore режим включиться!");
        player.sendMessage("§6Тип /playtime для перегляду прогресу");
        player.sendMessage("§b🎯 Полоска вище показує ваш прогрес!");

        Bukkit.broadcastMessage("§6[TimeHardcore] §e" + player.getName() + " §aзапустив таймер!");

        return true;
    }

    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("§a%d§7год §a%02d§7хв §a%02d§7сек", hours, minutes, secs);
    }
}
