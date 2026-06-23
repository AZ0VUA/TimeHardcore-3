package com.timehardcore;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TimeHardcore extends JavaPlugin implements Listener {

    private TimeManager timeManager;
    private static final long HARDCORE_THRESHOLD = 15 * 60 * 60; // 15 годин в секундах
    private final Map<UUID, BossBar> playerBars = new HashMap<>();
    private final Map<UUID, Long> hardcoreStartTime = new HashMap<>(); // Коли став Hardcore

    @Override
    public void onEnable() {
        timeManager = new TimeManager(this);
        getServer().getPluginManager().registerEvents(this, this);
        
        // Команди
        getCommand("playtime").setExecutor(new PlaytimeCommand(timeManager, HARDCORE_THRESHOLD, hardcoreStartTime));
        getCommand("starttimer").setExecutor(new StartTimerCommand(timeManager, this));
        
        // Запустити таймер у ВСІХ
        getCommand("startalltime").setExecutor((sender, cmd, label, args) -> {
            if (!sender.hasPermission("timehardcore.admin")) {
                sender.sendMessage("§cНемає прав!");
                return true;
            }
            
            int count = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!timeManager.isTimerRunning(player.getUniqueId())) {
                    timeManager.startTimer(player.getUniqueId());
                    createBossBar(player, player.getUniqueId());
                    count++;
                }
            }
            
            Bukkit.broadcastMessage("\n§6§l╔════════════════════════════════════════╗");
            Bukkit.broadcastMessage("§6§l║ ✅ ТАЙМЕР ЗАПУЩЕНО ДЛЯ ВСІХ! §r§6§l  ║");
            Bukkit.broadcastMessage("§6§l╠════════════════════════════════════════╣");
            Bukkit.broadcastMessage("§6§l║ §r§eГравців: §a" + count);
            Bukkit.broadcastMessage("§6§l║ §r§c15 годин до HARDCORE режиму!        §6§l║");
            Bukkit.broadcastMessage("§6§l║ §r§cСмерть = ПОСТІЙНИЙ БАН ☠            §6§l║");
            Bukkit.broadcastMessage("§6§l╚════════════════════════════════════════╝\n");
            
            return true;
        });
        
        // Зупинити таймер у ВСІХ
        getCommand("stopalltime").setExecutor((sender, cmd, label, args) -> {
            if (!sender.hasPermission("timehardcore.admin")) {
                sender.sendMessage("§cНемає прав!");
                return true;
            }
            
            int count = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (timeManager.isTimerRunning(player.getUniqueId())) {
                    timeManager.stopTimer(player.getUniqueId());
                    BossBar bar = playerBars.remove(player.getUniqueId());
                    if (bar != null) {
                        bar.removeAll();
                    }
                    count++;
                }
            }
            
            Bukkit.broadcastMessage("\n§c§l╔════════════════════════════════════════╗");
            Bukkit.broadcastMessage("§c§l║ ⏹ ТАЙМЕР ЗУПИНЕНО ДЛЯ ВСІХ! §r§c§l   ║");
            Bukkit.broadcastMessage("§c§l║ §r§eГравців: §a" + count);
            Bukkit.broadcastMessage("§c§l╚════════════════════════════════════════╝\n");
            
            return true;
        });
        
        // Пропустити час (для тестування)
        getCommand("skiptime").setExecutor((sender, cmd, label, args) -> {
            if (!sender.hasPermission("timehardcore.admin")) {
                sender.sendMessage("§cНемає прав!");
                return true;
            }
            
            if (args.length == 0) {
                sender.sendMessage("§cВикористання: /skiptime <секунди>");
                sender.sendMessage("§eПриклади: /skiptime 3600 (1 час), /skiptime 54000 (15 часов)");
                return true;
            }
            
            try {
                long seconds = Long.parseLong(args[0]);
                int count = 0;
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    if (timeManager.isTimerRunning(uuid)) {
                        timeManager.addTime(uuid, seconds);
                        count++;
                    }
                }
                
                sender.sendMessage("\n§e§l╔════════════════════════════════════════╗");
                sender.sendMessage("§e§l║ ⏩ ВРЕМЯ ПРОПУЩЕНО! §r§e§l              ║");
                sender.sendMessage("§e§l╠════════════════════════════════════════╣");
                sender.sendMessage("§e§l║ §r§eДобавлено: §a" + seconds + " §eсекунд");
                sender.sendMessage("§e§l║ §r§eГравців: §a" + count);
                sender.sendMessage("§e§l╚════════════════════════════════════════╝\n");
                
                return true;
            } catch (NumberFormatException e) {
                sender.sendMessage("§cОшибка: введіть число!");
                return true;
            }
        });
        
        // Перевірити статус
        getCommand("checkstatus").setExecutor((sender, cmd, label, args) -> {
            sender.sendMessage("\n§6§l╔════════════════════════════════════════╗");
            sender.sendMessage("§6§l║ 📊 STATUS ВСІХ ГРАВЦІВ §r§6§l         ║");
            sender.sendMessage("§6§l╠════════════════════════════════════════╣");
            sender.sendMessage("§6§l║ §r§eОнлайн: §a" + Bukkit.getOnlinePlayers().size());
            sender.sendMessage("§6§l║ §r");
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                long time = timeManager.getTime(uuid);
                boolean hardcore = timeManager.isHardcore(uuid);
                boolean running = timeManager.isTimerRunning(uuid);
                
                long hours = time / 3600;
                long minutes = (time % 3600) / 60;
                long secs = time % 60;
                
                String status = running ? "§a✓" : "§c✗";
                String mode;
                
                if (hardcore) {
                    long hardcoreTime = hardcoreStartTime.getOrDefault(uuid, 0L);
                    long daysSurvived = (System.currentTimeMillis() - hardcoreTime) / (24 * 60 * 1000); // дні
                    mode = "§4☠ HC (" + daysSurvived + "д)";
                } else {
                    mode = "§2Норм";
                }
                
                sender.sendMessage(String.format("§6§l║ §r§e%s §a[%02d:%02d:%02d] %s %s", 
                    padName(player.getName(), 12), hours, minutes, secs, status, mode));
            }
            
            sender.sendMessage("§6§l╚════════════════════════════════════════╝\n");
            return true;
        });
        
        // Допомога
        getCommand("thhelp").setExecutor((sender, cmd, label, args) -> {
            sender.sendMessage("\n§6§l╔════════════════════════════════════════╗");
            sender.sendMessage("§6§l║ 📖 TIMEHARDCORE КОМАНДИ §r§6§l         ║");
            sender.sendMessage("§6§l╠════════════════════════════════════════╣");
            sender.sendMessage("§6§l║ §r§eГРАВЦІ:");
            sender.sendMessage("§6§l║ §r§f/starttimer §e- Запустити таймер");
            sender.sendMessage("§6§l║ §r§f/playtime §e - Показати час");
            sender.sendMessage("§6§l║ §r§eАДМІНИ:");
            sender.sendMessage("§6§l║ §r§f/startalltime §e - Запустити ВСІМ");
            sender.sendMessage("§6§l║ §r§f/stopalltime §e  - Зупинити ВСІМ");
            sender.sendMessage("§6§l║ §r§f/skiptime <сек> §e - Пропустити час");
            sender.sendMessage("§6§l║ §r§f/checkstatus §e - Перевірити статус");
            sender.sendMessage("§6§l╚════════════════════════════════════════╝\n");
            return true;
        });

        // Основний цикл - рахує час для гравців
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    
                    if (timeManager.isTimerRunning(uuid)) {
                        timeManager.addTime(uuid, 1);
                        long totalSeconds = timeManager.getTime(uuid);
                        
                        updateBossBar(player, uuid, totalSeconds);
                        
                        // Перевіряємо чи досягли 15 годин
                        if (totalSeconds >= HARDCORE_THRESHOLD && !timeManager.isHardcore(uuid)) {
                            if (player.getGameMode() != GameMode.SPECTATOR) {
                                timeManager.setHardcore(uuid, true);
                                hardcoreStartTime.put(uuid, System.currentTimeMillis()); // Записуємо час запуску
                                activateHardcore(player);
                            }
                        }
                    }
                }
                
                try {
                    timeManager.save();
                } catch (IOException e) {
                    getLogger().warning("Помилка при збереженні: " + e.getMessage());
                }
            }
        }.runTaskTimer(this, 20L, 20L);

        getLogger().info("═══════════════════════════════════════");
        getLogger().info("✅ TimeHardcore v1.0.0 запущено!");
        getLogger().info("⏱ Таймер: 15 годин до Hardcore");
        getLogger().info("☠ Справжній HARDCORE режим Minecraft!");
        getLogger().info("═══════════════════════════════════════");
    }

    @Override
    public void onDisable() {
        for (BossBar bar : playerBars.values()) {
            bar.removeAll();
        }
        playerBars.clear();
        
        try {
            timeManager.save();
        } catch (IOException e) {
            getLogger().warning("Помилка при збереженні: " + e.getMessage());
        }
        
        getLogger().info("═══════════════════════════════════════");
        getLogger().info("❌ TimeHardcore вимкнено");
        getLogger().info("✅ Дані збережено");
        getLogger().info("═══════════════════════════════════════");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        timeManager.loadPlayer(uuid);

        if (timeManager.isHardcore(uuid)) {
            activateHardcore(player);
        }
        
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
        
        BossBar bar = playerBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
        
        try {
            timeManager.save();
        } catch (IOException e) {
            getLogger().warning("Помилка при збереженні: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();

        if (timeManager.isHardcore(uuid)) {
            BossBar bar = playerBars.remove(uuid);
            if (bar != null) {
                bar.removeAll();
            }
            
            long hardcoreTime = hardcoreStartTime.getOrDefault(uuid, 0L);
            long daysSurvived = (System.currentTimeMillis() - hardcoreTime) / (24 * 60 * 1000); // дні
            
            Bukkit.getScheduler().runTaskLater(this, () -> {
                player.kickPlayer("§4☠ Ти помер у Hardcore режимі!\n§4Ти прожив " + daysSurvived + " днів\n§4Ти заблокований назавжди!");
                
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(
                    player.getName(),
                    "§c[TimeHardcore] HARDCORE - Помер після " + daysSurvived + " днів",
                    null,
                    "TimeHardcore Plugin"
                );
                
                getLogger().warning("════════════════════════════════════════");
                getLogger().warning("☠ ГРАВЕЦЬ ЗАБАНЕНИЙ У HARDCORE");
                getLogger().warning("Ім'я: " + player.getName());
                getLogger().warning("Прожив днів: " + daysSurvived);
                getLogger().warning("Час гри: " + formatTime(timeManager.getTime(uuid)));
                getLogger().warning("════════════════════════════════════════");
            }, 10L);
        }
    }
    
    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Якщо гравець в Hardcore - забороняємо змінювати режим
        if (timeManager.isHardcore(uuid) && event.getNewGameMode() != GameMode.SURVIVAL) {
            event.setCancelled(true);
            player.sendMessage("§c☠ Ти не можеш змінити режим гри в Hardcore режимі!");
        }
    }

    public void createBossBar(Player player, UUID uuid) {
        BossBar oldBar = playerBars.get(uuid);
        if (oldBar != null) {
            oldBar.removeAll();
        }
        
        BossBar bar = Bukkit.createBossBar(
            "§e⏱ TimeHardcore: 0:00:00 / 15:00:00",
            BarColor.GREEN,
            BarStyle.SOLID
        );
        
        bar.addPlayer(player);
        bar.setProgress(0.0);
        playerBars.put(uuid, bar);
    }

    private void updateBossBar(Player player, UUID uuid, long totalSeconds) {
        BossBar bar = playerBars.get(uuid);
        
        if (bar == null) {
            createBossBar(player, uuid);
            bar = playerBars.get(uuid);
        }

        double progress = Math.min((double) totalSeconds / HARDCORE_THRESHOLD, 1.0);

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        String currentTime = String.format("%d:%02d:%02d", hours, minutes, seconds);
        
        long remaining = HARDCORE_THRESHOLD - totalSeconds;
        long remHours = remaining / 3600;
        long remMinutes = (remaining % 3600) / 60;

        boolean isHardcore = timeManager.isHardcore(uuid);
        
        if (isHardcore) {
            long hardcoreTime = hardcoreStartTime.getOrDefault(uuid, 0L);
            long daysSurvived = (System.currentTimeMillis() - hardcoreTime) / (24 * 60 * 1000);
            
            bar.setColor(BarColor.RED);
            bar.setTitle("§4§l☠ HARDCORE! (" + daysSurvived + " днів) ☠ §e" + currentTime);
            bar.setProgress(1.0);
            
        } else if (progress >= 0.95) {
            bar.setColor(BarColor.RED);
            bar.setTitle("§c⚠⚠⚠ КРИТИЧНО! §e" + currentTime + " / 15:00:00");
            bar.setProgress(progress);
            
        } else if (progress >= 0.85) {
            bar.setColor(BarColor.RED);
            bar.setTitle("§6⚠⚠ БЛИЗЬКО! §e" + currentTime + " / 15:00:00");
            bar.setProgress(progress);
            
        } else if (progress >= 0.70) {
            bar.setColor(BarColor.YELLOW);
            bar.setTitle("§6⚠ До Hardcore: §e" + currentTime + " / 15:00:00");
            bar.setProgress(progress);
            
        } else if (progress >= 0.50) {
            bar.setColor(BarColor.YELLOW);
            bar.setTitle("§e⏱ Час: " + currentTime + " / 15:00:00");
            bar.setProgress(progress);
            
        } else if (progress > 0) {
            bar.setColor(BarColor.GREEN);
            bar.setTitle("§a✓ Час: " + currentTime + " / 15:00:00");
            bar.setProgress(progress);
        }
    }

    private void activateHardcore(Player player) {
        UUID uuid = player.getUniqueId();
        
        // СПРАВЖНІЙ HARDCORE - SURVIVAL режим на максимальній складності
        player.setGameMode(GameMode.SURVIVAL);
        
        // Встановлюємо максимальну складність для всього світу
        player.getWorld().setDifficulty(Difficulty.HARD);
        
        // Гравець залишається з нормальним здоров'ям, але це СПРАВЖНІЙ HARDCORE
        // При смерті - бан навічки
        
        long hardcoreTime = hardcoreStartTime.getOrDefault(uuid, 0L);
        long daysSurvived = (System.currentTimeMillis() - hardcoreTime) / (24 * 60 * 1000);
        
        player.sendMessage("\n");
        player.sendMessage("§4§l╔════════════════════════════════════════╗");
        player.sendMessage("§4§l║ ☠ HARDCORE РЕЖИМ АКТИВОВАНИЙ! ☠ §r§4§l║");
        player.sendMessage("§4§l╠════════════════════════════════════════╣");
        player.sendMessage("§4§l║ §r§cТи прожив 15 ГОДИН!                 §4§l║");
        player.sendMessage("§4§l║ §r§cТепер СПРАВЖНІЙ HARDCORE РЕЖИМ!      §4§l║");
        player.sendMessage("§4§l║ §r§cСмерть → ПОСТІЙНИЙ БАН ☠            §4§l║");
        player.sendMessage("§4§l║ §r§aСкладність: §cМАКСИМАЛЬНА          §4§l║");
        player.sendMessage("§4§l║ §r§aDні вижити: §f" + daysSurvived);
        player.sendMessage("§4§l╚════════════════════════════════════════╝");
        player.sendMessage("\n");
        
        Bukkit.broadcastMessage("\n§4§l╔════════════════════════════════════════╗");
        Bukkit.broadcastMessage("§4§l║ ☠ " + player.getName() + " УВІЙШОВ В HARDCORE! ☠");
        Bukkit.broadcastMessage("§4§l║ §r§cТепер це справжня гра на виживання!");
        Bukkit.broadcastMessage("§4§l╚════════════════════════════════════════╝\n");
    }

    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%d год %d хв %d сек", hours, minutes, secs);
    }
    
    private String padName(String name, int length) {
        if (name.length() >= length) return name;
        return String.format("%-" + length + "s", name);
    }
}
