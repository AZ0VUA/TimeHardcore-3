package com.timehardcore;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TimeHardcore extends JavaPlugin implements Listener {

    private TimeManager timeManager;
    private final long HARDCORE_THRESHOLD = 15 * 3600; // 15 годин в секундах
    private Map<UUID, BossBar> playerBossBars = new HashMap<>();

    @Override
    public void onEnable() {
        this.timeManager = new TimeManager(this);
        
        getCommand("starttimer").setExecutor(new StartTimerCommand(this, timeManager));
        getCommand("playtime").setExecutor(new PlaytimeCommand(timeManager, HARDCORE_THRESHOLD));
        getCommand("stoptimer").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cТільки гравці можуть це робити!");
                return true;
            }
            Player player = (Player) sender;
            timeManager.stopTimer(player.getUniqueId());
            removeBossBar(player.getUniqueId());
            player.sendMessage("§6Таймер зупинено!");
            return true;
        });

        getServer().getPluginManager().registerEvents(this, this);
        
        getLogger().info("§6TimeHardcore плагін запущений! (Версія 1.20.1)");
        startTickerTask();
    }

    @Override
    public void onDisable() {
        // Очистити БоссБари
        for (BossBar bar : playerBossBars.values()) {
            bar.removeAll();
        }
        playerBossBars.clear();
        timeManager.saveAll();
    }

    private void startTickerTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    
                    if (timeManager.isTimerRunning(uuid)) {
                        // Збільшуємо час на 1 секунду
                        timeManager.addTime(uuid, 1);
                        long currentTime = timeManager.getTime(uuid);
                        
                        // Оновлюємо БоссБар
                        updateBossBar(player, currentTime);
                        
                        // Перевіряємо, чи досягли 15 годин
                        if (currentTime >= HARDCORE_THRESHOLD && !timeManager.isHardcore(uuid)) {
                            enableHardcore(player);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L); // Кожні 1 секунду (20 тіків)
    }

    private void updateBossBar(Player player, long seconds) {
        UUID uuid = player.getUniqueId();
        
        // Створюємо БоссБар якщо його немає
        if (!playerBossBars.containsKey(uuid)) {
            BossBar bar = Bukkit.createBossBar(
                "⏱ TimeHardcore Таймер",
                BarColor.GREEN,
                BarStyle.SOLID
            );
            bar.addPlayer(player);
            playerBossBars.put(uuid, bar);
        }
        
        BossBar bar = playerBossBars.get(uuid);
        
        // Розраховуємо прогрес (0.0 - 1.0)
        double progress = Math.min(1.0, (double) seconds / HARDCORE_THRESHOLD);
        
        // Визначаємо колір залежно від прогресу
        BarColor color;
        if (progress < 0.5) {
            color = BarColor.GREEN;
        } else if (progress < 0.75) {
            color = BarColor.YELLOW;
        } else if (progress < 0.9) {
            color = BarColor.RED; // Використовуємо RED замість ORANGE
        } else {
            color = BarColor.RED;
        }
        
        // Оновлюємо вміст БоссБара
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        String title = String.format("⏱ %02d:%02d:%02d / 15:00:00", hours, minutes, secs);
        
        if (timeManager.isHardcore(uuid)) {
            title = "☠ HARDCORE РЕЖИМ! Смерть = БАН ☠";
            color = BarColor.RED;
        }
        
        bar.setTitle(title);
        bar.setColor(color);
        bar.setProgress(progress);
    }

    private void enableHardcore(Player player) {
        UUID uuid = player.getUniqueId();
        timeManager.setHardcore(uuid, true);
        
        player.sendTitle(
            "§c§lHARDCORE РЕЖИМ",
            "§4Смерть = ПОСТІЙНИЙ БАН!",
            10, 70, 10
        );
        
        Bukkit.broadcastMessage("§4§l[⚠] " + player.getName() + " §4увійшов в HARDCORE режим!");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        
        if (timeManager.isHardcore(uuid)) {
            // Забанити гравця назавжди
            player.setWhitelisted(false);
            Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(
                player.getName(),
                "☠ Ви помер в HARDCORE режимі! Постійний бан.",
                null,
                null
            );
            
            Bukkit.broadcastMessage("§4§l[☠] " + player.getName() + " §4помер в HARDCORE режимі! ПОСТІЙНИЙ БАН!");
            
            // Кікнути гравця
            player.kickPlayer("§4☠ Ви помер в HARDCORE режимі!\n§4ПОСТІЙНИЙ БАН!");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        timeManager.loadPlayer(player.getUniqueId());
        
        long time = timeManager.getTime(player.getUniqueId());
        if (time > 0) {
            long hours = time / 3600;
            long minutes = (time % 3600) / 60;
            player.sendMessage(String.format("§6Ваш час онлайн: §f%d год %d хв", hours, minutes));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        timeManager.stopTimer(uuid);
        removeBossBar(uuid);
    }

    private void removeBossBar(UUID uuid) {
        BossBar bar = playerBossBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
    }

    public long getHardcoreThreshold() {
        return HARDCORE_THRESHOLD;
    }
}
