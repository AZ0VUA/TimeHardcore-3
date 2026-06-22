package com.timehardcore;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StartTimerCommand implements CommandExecutor {

    private final TimeHardcore plugin;
    private final TimeManager timeManager;

    public StartTimerCommand(TimeHardcore plugin, TimeManager timeManager) {
        this.plugin = plugin;
        this.timeManager = timeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cЦю команду можна використовувати тільки як гравець!");
            return true;
        }

        Player player = (Player) sender;
        
        if (timeManager.isTimerRunning(player.getUniqueId())) {
            player.sendMessage("§cТаймер вже запущений! Використайте §e/stoptimer §cщоб зупинити.");
            return true;
        }

        timeManager.startTimer(player.getUniqueId());
        
        player.sendTitle(
            "§6§lТаймер запущено!",
            "§eЧекайте 15 годин...",
            10, 70, 10
        );
        
        player.sendMessage("§6§l=== TimeHardcore ===");
        player.sendMessage("§a✓ Таймер запущено!");
        player.sendMessage("§eПосле 15 годин онлайн → HARDCORE режим");
        player.sendMessage("§4Смерть в HARDCORE → ПОСТІЙНИЙ БАН ☠");
        player.sendMessage("§eКоманди: §f/playtime §eі §f/stoptimer");
        player.sendMessage("§6§l==================");

        return true;
    }
}
