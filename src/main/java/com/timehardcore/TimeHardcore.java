package com.timehardcore;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class TimeHardcore extends JavaPlugin implements Listener {

    private TimeManager timeManager;
    private static final long HARDCORE_THRESHOLD = 15 * 60 * 60; // 15 годин в секундах

    @Override
    public void onEnable() {
        timeManager = new TimeManager(this);
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("playtime").setExecutor(new PlaytimeCommand(timeManager, HARDCORE_THRESHOLD));
        getCommand("starttimer").setExecutor(new StartTimerCommand(timeManager));

        // Тік кожні 20 секунд — зберігає час онлайн гравців
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    timeManager.addTime(uuid, 20); // +20 секунд

                    long totalSeconds = timeManager.getTime(uuid);
                    if (totalSeconds >= HARDCORE_THRESHOLD && player.getGameMode() != GameMode.SPECTATOR) {
                        if (!timeManager.isHardcore(uuid)) {
                            timeManager.setHardcore(uuid, true);
                            activateHardcore(player);
                        }
                    }
                }
                timeManager.save();
            }
        }.runTaskTimer(this, 400L, 400L); // кожні 20 секунд (400 тіків)

        getLogger().info("TimeHardcore увімкнено! Поріг: 15 годин онлайн.");
    }

    @Override
    public void onDisable() {
        // Зберігаємо час всіх онлайн гравців перед вимкненням
        for (Player player : Bukkit.getOnlinePlayers()) {
            timeManager.save();
        }
        getLogger().info("TimeHardcore вимкнено. Дані збережено.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Якщо гравець вже набрав 15 годин — відразу хардкор
        if (timeManager.isHardcore(uuid)) {
            activateHardcore(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        timeManager.save();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();

        if (timeManager.isHardcore(uuid)) {
            // Бан після смерті в хардкорі
            Bukkit.getScheduler().runTaskLater(this, () -> {
                player.kickPlayer("§4☠ Ти помер у Hardcore режимі. Ти заблокований назавжди.");
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(
                    player.getName(),
                    "§4Смерть у Hardcore режимі після 15 годин гри.",
                    null,
                    "TimeHardcore"
                );
                getLogger().info("[TimeHardcore] Гравець " + player.getName() + " заблокований після смерті в Hardcore.");
            }, 20L);
        }
    }

    private void activateHardcore(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        // Встановлюємо хардкор через атрибут серця — 1 life
        player.setHealthScale(2.0);
        player.sendMessage("§4§l☠ УВАГА! §r§c Ти зіграв 15+ годин. Ти тепер у §4§lHARDCORE §cрежимі!");
        player.sendMessage("§cЯкщо помреш — §4§lПОСТІЙНИЙ БАН§c!");
        Bukkit.broadcastMessage("§4[TimeHardcore] §e" + player.getName() + " §cтепер у §4§lHARDCORE §cрежимі!");
        getLogger().info("[TimeHardcore] " + player.getName() + " переведений у Hardcore режим.");
    }
}
