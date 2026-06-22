package com.timehardcore;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TimeHardcore extends JavaPlugin implements Listener {

    private TimeManager timeManager;
    private static final long HARDCORE_THRESHOLD = 15 * 60 * 60; // 15 годин в секундах
    private final Map<UUID, BossBar> playerBars = new HashMap<>();

    @Override
    public void onEnable() {
        timeManager = new TimeManager(this);
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("playtime").setExecutor(new PlaytimeCommand(timeManager, HARDCORE_THRESHOLD));
        getCommand("starttimer").setExecutor(new StartTimerCommand(timeManager, this));

        // Тік кожну 1 секунду — рахує час і оновлює BossBar
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    timeManager.addTime(uuid, 1); // +1 секунда

                    long totalSeconds = timeManager.getTime(uuid);
                    
                    // Оновлюємо BossBar для гравця
                    updateBossBar(player, uuid, totalSeconds);

                    if (totalSeconds >= HARDCORE_THRESHOLD && player.getGameMode() != GameMode.SPECTATOR) {
                        if (!timeManager.isHardcore(uuid)) {
                            timeManager.setHardcore(uuid, true);
                            activateHardcore(player);
                        }
                    }
                }
                
                // Зберігаємо дані кожні 20 тіків (1 секунда)
                if (Bukkit.getServer().getCurrentTick() % 20 == 0) {
                    timeManager.save();
                }
            }
        }.runTaskTimer(this, 20L, 1L); // кожну секунду (20 тіків = 1 секунда)

        getLogger().info("TimeHardcore увімкнено! Поріг: 15 годин онлайн.");
    }

    @Override
    public void onDisable() {
        // Видаляємо всі BossBars
        for (BossBar bar : playerBars.values()) {
            bar.removeAll();
        }
        playerBars.clear();
        
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
        
        // Якщо таймер запущений — показуємо BossBar
        if (timeManager.isTimerRunning(uuid)) {
            long totalSeconds = timeManager.getTime(uuid);
            updateBossBar(player, uuid, totalSeconds);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Видаляємо BossBar для гравця
        BossBar bar = playerBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
        
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

    public void createBossBar(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Видаляємо старий бар якщо він існує
        BossBar oldBar = playerBars.get(uuid);
        if (oldBar != null) {
            oldBar.removeAll();
        }
        
        // Створюємо новий BossBar
        BossBar bar = Bukkit.createBossBar("§eЧас гри: 0:00:00 / 15:00:00", BarColor.GREEN, BarStyle.SOLID);
        bar.addPlayer(player);
        playerBars.put(uuid, bar);
        
        getLogger().info("[TimeHardcore] BossBar створений для " + player.getName());
    }

    private void updateBossBar(Player player, UUID uuid, long totalSeconds) {
        // Якщо таймер не запущений — не показуємо бар
        if (!timeManager.isTimerRunning(uuid)) {
            BossBar bar = playerBars.remove(uuid);
            if (bar != null) {
                bar.removeAll();
            }
            return;
        }
        
        BossBar bar = playerBars.get(uuid);
        
        // Якщо бара немає — створюємо
        if (bar == null) {
            bar = Bukkit.createBossBar("§eЧас гри: 0:00:00 / 15:00:00", BarColor.GREEN, BarStyle.SOLID);
            bar.addPlayer(player);
            playerBars.put(uuid, bar);
        }

        // Розраховуємо прогрес (0.0 до 1.0)
        double progress = Math.min((double) totalSeconds / HARDCORE_THRESHOLD, 1.0);

        // Форматуємо час
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        String timeStr = String.format("%d:%02d:%02d", hours, minutes, seconds);

        boolean isHardcore = timeManager.isHardcore(uuid);
        
        if (isHardcore) {
            // HARDCORE режим — червона полоска
            bar.setColor(BarColor.RED);
            bar.setTitle("§4§l☠ HARDCORE! §r§e" + timeStr + " §7(Смерть = БАН)");
            bar.setProgress(1.0);
        } else if (progress >= 0.9) {
            // 90% прогресу — червоний
            bar.setColor(BarColor.RED);
            bar.setTitle("§c⚠⚠⚠ ДУЖЕ БЛИЗЬКО! §e" + timeStr + " / 15:00:00");
            bar.setProgress(progress);
        } else if (progress >= 0.75) {
            // 75% прогресу — оранжевий
            bar.setColor(BarColor.ORANGE);
            bar.setTitle("§6⚠⚠ Дуже близько! §e" + timeStr + " / 15:00:00");
            bar.setProgress(progress);
        } else if (progress >= 0.5) {
            // 50% прогресу — жовтий
            bar.setColor(BarColor.YELLOW);
            bar.setTitle("§e⏱ Час гри: " + timeStr + " / 15:00:00");
            bar.setProgress(progress);
        } else if (progress > 0) {
            // Менше 50% — зелений
            bar.setColor(BarColor.GREEN);
            bar.setTitle("§a✓ Час гри: " + timeStr + " / 15:00:00");
            bar.setProgress(progress);
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
