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
    
    // Зберігаємо BossBar для кожного гравця
    private final Map<UUID, BossBar> playerBars = new HashMap<>();

    @Override
    public void onEnable() {
        timeManager = new TimeManager(this);
        getServer().getPluginManager().registerEvents(this, this);
        
        // Реєструємо команди
        getCommand("playtime").setExecutor(new PlaytimeCommand(timeManager, HARDCORE_THRESHOLD));
        getCommand("starttimer").setExecutor(new StartTimerCommand(timeManager, this));

        // Основний таймер - оновлюється кожну секунду
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    
                    // Додаємо час тільки якщо таймер запущений
                    if (timeManager.isTimerRunning(uuid)) {
                        timeManager.addTime(uuid, 1); // +1 секунда
                        long totalSeconds = timeManager.getTime(uuid);
                        
                        // Оновлюємо BossBar
                        updateBossBar(player, uuid, totalSeconds);
                        
                        // Перевіряємо чи досягнув Hardcore
                        if (totalSeconds >= HARDCORE_THRESHOLD && !timeManager.isHardcore(uuid)) {
                            if (player.getGameMode() != GameMode.SPECTATOR) {
                                timeManager.setHardcore(uuid, true);
                                activateHardcore(player);
                            }
                        }
                    }
                }
                
                // Зберігаємо дані кожну секунду
                timeManager.save();
            }
        }.runTaskTimer(this, 20L, 20L); // 20 тіків = 1 секунда

        getLogger().info("═══════════════════════════════════════");
        getLogger().info("✅ TimeHardcore v1.0.0 запущено!");
        getLogger().info("⏱ Таймер: 15 годин до Hardcore");
        getLogger().info("☠ При смерті в Hardcore = постійний бан");
        getLogger().info("═══════════════════════════════════════");
    }

    @Override
    public void onDisable() {
        // Видаляємо всі BossBars перед вимкненням
        for (BossBar bar : playerBars.values()) {
            bar.removeAll();
        }
        playerBars.clear();
        
        // Зберігаємо дані
        timeManager.save();
        
        getLogger().info("═══════════════════════════════════════");
        getLogger().info("❌ TimeHardcore вимкнено");
        getLogger().info("✅ Дані збережено");
        getLogger().info("═══════════════════════════════════════");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Якщо гравець вже був у Hardcore - активуємо його
        if (timeManager.isHardcore(uuid)) {
            activateHardcore(player);
        }
        
        // Якщо таймер був запущений раніше - відновлюємо BossBar
        if (timeManager.isTimerRunning(uuid)) {
            long totalSeconds = timeManager.getTime(uuid);
            createBossBar(player, uuid);
            updateBossBar(player, uuid, totalSeconds);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Видаляємо BossBar
        BossBar bar = playerBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
        
        // Зберігаємо дані
        timeManager.save();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();

        if (timeManager.isHardcore(uuid)) {
            // Видаляємо BossBar перед баном
            BossBar bar = playerBars.remove(uuid);
            if (bar != null) {
                bar.removeAll();
            }
            
            // Бан із затримкою щоб гравець бачив повідомлення
            Bukkit.getScheduler().runTaskLater(this, () -> {
                player.kickPlayer("§4☠ Ти помер у Hardcore режимі!\n§4Ти заблокований назавжди!");
                
                // Банимо гравця
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(
                    player.getName(),
                    "§c[TimeHardcore] Смерть у Hardcore режимі після " + 
                    formatTime(timeManager.getTime(uuid)) + " гри.",
                    null,
                    "TimeHardcore Plugin"
                );
                
                getLogger().warning("════════════════════════════════════════");
                getLogger().warning("☠ ГРАВЕЦЬ ЗАБАНЕНИЙ");
                getLogger().warning("Ім'я: " + player.getName());
                getLogger().warning("Час гри: " + formatTime(timeManager.getTime(uuid)));
                getLogger().warning("Причина: Смерть у Hardcore режимі");
                getLogger().warning("════════════════════════════════════════");
            }, 10L);
        }
    }

    /**
     * Створює новий BossBar для гравця
     */
    public void createBossBar(Player player, UUID uuid) {
        // Видаляємо старий якщо існує
        BossBar oldBar = playerBars.get(uuid);
        if (oldBar != null) {
            oldBar.removeAll();
        }
        
        // Створюємо новий
        BossBar bar = Bukkit.createBossBar(
            "§e⏱ TimeHardcore: 0:00:00 / 15:00:00",
            BarColor.GREEN,
            BarStyle.SOLID
        );
        
        bar.addPlayer(player);
        bar.setProgress(0.0);
        playerBars.put(uuid, bar);
        
        getLogger().info("✅ BossBar створений для " + player.getName());
    }

    /**
     * Перевантажує BossBar з новими даними
     */
    private void updateBossBar(Player player, UUID uuid, long totalSeconds) {
        BossBar bar = playerBars.get(uuid);
        
        // Якщо бара немає - створюємо
        if (bar == null) {
            createBossBar(player, uuid);
            bar = playerBars.get(uuid);
        }

        // Розраховуємо прогрес
        double progress = Math.min((double) totalSeconds / HARDCORE_THRESHOLD, 1.0);

        // Форматуємо час
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        String currentTime = String.format("%d:%02d:%02d", hours, minutes, seconds);
        
        // Залишилось часу
        long remaining = HARDCORE_THRESHOLD - totalSeconds;
        long remHours = remaining / 3600;
        long remMinutes = (remaining % 3600) / 60;

        boolean isHardcore = timeManager.isHardcore(uuid);
        
        // Встановлюємо колір і заголовок залежно від прогресу
        if (isHardcore) {
            // ☠ HARDCORE режим!
            bar.setColor(BarColor.RED);
            bar.setStyle(BarStyle.SOLID);
            bar.setTitle("§4§l☠ HARDCORE РЕЖИМ! ☠ §r§e" + currentTime + " §7| §cСмерть = БАН");
            bar.setProgress(1.0);
            
        } else if (progress >= 0.95) {
            // Дуже дуже близько!
            bar.setColor(BarColor.RED);
            bar.setTitle("§c⚠⚠⚠ КРИТИЧНО БЛИЗЬКО! §e" + currentTime + " / 15:00:00 §7(осталось §c" + remHours + "h " + remMinutes + "m§7)");
            bar.setProgress(progress);
            
        } else if (progress >= 0.85) {
            // Дуже близько
            bar.setColor(BarColor.RED);
            bar.setTitle("§6⚠⚠ ДУЖЕ БЛИЗЬКО! §e" + currentTime + " / 15:00:00 §7(осталось §c" + remHours + "h " + remMinutes + "m§7)");
            bar.setProgress(progress);
            
        } else if (progress >= 0.70) {
            // Близько
            bar.setColor(BarColor.ORANGE);
            bar.setTitle("§6⚠ Близько до Hardcore! §e" + currentTime + " / 15:00:00 §7(осталось §c" + remHours + "h " + remMinutes + "m§7)");
            bar.setProgress(progress);
            
        } else if (progress >= 0.50) {
            // Середина
            bar.setColor(BarColor.YELLOW);
            bar.setTitle("§e⏱ Час гри: " + currentTime + " / 15:00:00 §7(осталось §a" + remHours + "h " + remMinutes + "m§7)");
            bar.setProgress(progress);
            
        } else if (progress > 0) {
            // Початок
            bar.setColor(BarColor.GREEN);
            bar.setTitle("§a✓ Час гри: " + currentTime + " / 15:00:00 §7(осталось §a" + remHours + "h " + remMinutes + "m§7)");
            bar.setProgress(progress);
        }
    }

    /**
     * Активує Hardcore режим для гравця
     */
    private void activateHardcore(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Встановлюємо GameMode
        player.setGameMode(GameMode.SURVIVAL);
        
        // Встановлюємо максимум 1 сердце (2.0 = 1 сердце)
        player.setHealthScale(2.0);
        player.setMaxHealth(2.0);
        player.setHealth(2.0);
        
        // Повідомлення гравцю
        player.sendMessage("\n");
        player.sendMessage("§4§l╔════════════════════════════════════════╗");
        player.sendMessage("§4§l║ ☠ ЛАДНО! ТИ ПРОЖИВ 15 ГОДИН! ☠ §r§4§l║");
        player.sendMessage("§4§l╠════════════════════════════════════════╣");
        player.sendMessage("§4§l║ §r§cТи тепер у HARDCORE РЕЖИМІ!        §4§l║");
        player.sendMessage("§4§l║ §r§cПри смерті → ПОСТІЙНИЙ БАН!         §4§l║");
        player.sendMessage("§4§l║ §r§aТобі осталось 1 сердце!            §4§l║");
        player.sendMessage("§4§l╚════════════════════════════════════════╝");
        player.sendMessage("\n");
        
        // BroadCast для всіх
        Bukkit.broadcastMessage("§4§l╔════════════════════════════════════════╗");
        Bukkit.broadcastMessage("§4§l║ §r§e" + player.getName() + " §c перейшов у HARDCORE РЕЖИМ! ☠");
        Bukkit.broadcastMessage("§4§l╚════════════════════════════════════════╝");
        
        getLogger().warning("════════════════════════════════════════");
        getLogger().warning("☠ ГРАВЕЦЬ ПЕРЕВЕДЕНИЙ У HARDCORE");
        getLogger().warning("Ім'я: " + player.getName());
        getLogger().warning("Час гри: " + formatTime(timeManager.getTime(uuid)));
        getLogger().warning("════════════════════════════════════════");
    }

    /**
     * Форматує час у красивий вигляд
     */
    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%d годин %d хвилин %d секунд", hours, minutes, secs);
    }
}
