package com.timehardcore;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TimeManager {

    private final JavaPlugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;

    private final Map<UUID, Long> playerTime = new HashMap<>();
    private final Map<UUID, Boolean> hardcoreMap = new HashMap<>();
    private final Map<UUID, Boolean> timerRunning = new HashMap<>();

    public TimeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        load();
    }

    /**
     * Додати час до гравця
     */
    public void addTime(UUID uuid, long seconds) {
        if (!isTimerRunning(uuid)) {
            return;
        }
        
        long current = playerTime.getOrDefault(uuid, 0L);
        playerTime.put(uuid, current + seconds);
    }

    /**
     * Отримати час гравця
     */
    public long getTime(UUID uuid) {
        return playerTime.getOrDefault(uuid, 0L);
    }

    /**
     * Перевірити чи гравець в Hardcore
     */
    public boolean isHardcore(UUID uuid) {
        return hardcoreMap.getOrDefault(uuid, false);
    }

    /**
     * Встановити Hardcore статус
     */
    public void setHardcore(UUID uuid, boolean value) {
        hardcoreMap.put(uuid, value);
    }

    /**
     * Перевірити чи таймер запущений
     */
    public boolean isTimerRunning(UUID uuid) {
        return timerRunning.getOrDefault(uuid, false);
    }

    /**
     * Запустити таймер
     */
    public void startTimer(UUID uuid) {
        timerRunning.put(uuid, true);
        if (!playerTime.containsKey(uuid)) {
            playerTime.put(uuid, 0L);
        }
    }

    /**
     * Зупинити таймер
     */
    public void stopTimer(UUID uuid) {
        timerRunning.put(uuid, false);
    }

    /**
     * Завантажити гравця з файлу (коли він заходить)
     */
    public void loadPlayer(UUID uuid) {
        String uuidStr = uuid.toString();
        if (dataConfig.contains("players." + uuidStr)) {
            long time = dataConfig.getLong("players." + uuidStr + ".time", 0L);
            boolean hardcore = dataConfig.getBoolean("players." + uuidStr + ".hardcore", false);
            boolean timerOn = dataConfig.getBoolean("players." + uuidStr + ".timer_running", false);
            
            playerTime.put(uuid, time);
            hardcoreMap.put(uuid, hardcore);
            timerRunning.put(uuid, timerOn);
        } else {
            // Новий гравець
            playerTime.put(uuid, 0L);
            hardcoreMap.put(uuid, false);
            timerRunning.put(uuid, false);
        }
    }

    /**
     * Зберегти всі дані в файл
     */
    public void save() throws IOException {
        for (Map.Entry<UUID, Long> entry : playerTime.entrySet()) {
            dataConfig.set("players." + entry.getKey() + ".time", entry.getValue());
        }
        for (Map.Entry<UUID, Boolean> entry : hardcoreMap.entrySet()) {
            dataConfig.set("players." + entry.getKey() + ".hardcore", entry.getValue());
        }
        for (Map.Entry<UUID, Boolean> entry : timerRunning.entrySet()) {
            dataConfig.set("players." + entry.getKey() + ".timer_running", entry.getValue());
        }
        
        dataConfig.save(dataFile);
    }

    /**
     * Завантажити дані з файлу при запуску плагіна
     */
    private void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
                plugin.getLogger().info("✅ Створено playerdata.yml");
            } catch (IOException e) {
                plugin.getLogger().warning("❌ Помилка створення playerdata.yml: " + e.getMessage());
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (dataConfig.contains("players")) {
            for (String uuidStr : dataConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    long time = dataConfig.getLong("players." + uuidStr + ".time", 0L);
                    boolean hardcore = dataConfig.getBoolean("players." + uuidStr + ".hardcore", false);
                    boolean timerOn = dataConfig.getBoolean("players." + uuidStr + ".timer_running", false);
                    
                    playerTime.put(uuid, time);
                    hardcoreMap.put(uuid, hardcore);
                    timerRunning.put(uuid, timerOn);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("⚠ Невірний UUID: " + uuidStr);
                }
            }
            plugin.getLogger().info("✅ Завантажено " + playerTime.size() + " гравців з playerdata.yml");
        } else {
            plugin.getLogger().info("ℹ playerdata.yml пусто");
        }
    }
}
