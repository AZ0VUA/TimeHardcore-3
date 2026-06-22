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
        long remaining = hardcoreThreshold - totalSeconds;

        sender.sendMessage("§6§l=== TimeHardcore ===");
        sender.sendMessage("§eГравець: §f" + name);
        sender.sendMessage(String.format("§eЧас онлайн: §f%d год %d хв %d сек", hours, minutes, seconds));

        if (isHardcore) {
            sender.sendMessage("§4§l☠ Статус: HARDCORE режим!");
        } else {
            long remHours = remaining / 3600;
            long remMinutes = (remaining % 3600) / 60;
            sender.sendMessage("§aСтатус: §2Звичайний режим");
            sender.sendMessage(String.format("§eДо Hardcore: §c%d год %d хв", remHours, remMinutes));
        }
        sender.sendMessage("§6§l==================");
    }
}
