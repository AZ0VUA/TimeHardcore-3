
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

    // UUID -> секунди онлайн
    private final Map<UUID, Long> playerTime = new HashMap<>();
    // UUID -> чи в хардкорі
    private final Map<UUID, Boolean> hardcoreMap = new HashMap<>();
    // UUID -> чи таймер запущений для гравця
    private final Map<UUID, Boolean> timerRunning = new HashMap<>();

    public TimeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        load();
    }

    public void addTime(UUID uuid, long seconds) {
        // Рахуємо час тільки якщо таймер запущений для цього гравця
        if (!isTimerRunning(uuid)) {
            return;
        }
        
        long current = playerTime.getOrDefault(uuid, 0L);
        playerTime.put(uuid, current + seconds);
    }

    public long getTime(UUID uuid) {
        return playerTime.getOrDefault(uuid, 0L);
    }

    public boolean isHardcore(UUID uuid) {
        return hardcoreMap.getOrDefault(uuid, false);
    }

    public void setHardcore(UUID uuid, boolean value) {
        hardcoreMap.put(uuid, value);
    }

    public boolean isTimerRunning(UUID uuid) {
        return timerRunning.getOrDefault(uuid, false);
    }

    public void startTimer(UUID uuid) {
        timerRunning.put(uuid, true);
        save();
    }

    public void stopTimer(UUID uuid) {
        timerRunning.put(uuid, false);
        save();
    }

    public void save() {
        for (Map.Entry<UUID, Long> entry : playerTime.entrySet()) {
            dataConfig.set("players." + entry.getKey() + ".time", entry.getValue());
        }
        for (Map.Entry<UUID, Boolean> entry : hardcoreMap.entrySet()) {
            dataConfig.set("players." + entry.getKey() + ".hardcore", entry.getValue());
        }
        for (Map.Entry<UUID, Boolean> entry : timerRunning.entrySet()) {
            dataConfig.set("players." + entry.getKey() + ".timer_running", entry.getValue());
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Не вдалося зберегти playerdata.yml: " + e.getMessage());
        }
    }

    private void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Не вдалося створити playerdata.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (dataConfig.contains("players")) {
            for (String uuidStr : dataConfig.getConfigurationSection("players").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                long time = dataConfig.getLong("players." + uuidStr + ".time", 0L);
                boolean hardcore = dataConfig.getBoolean("players." + uuidStr + ".hardcore", false);
                boolean timerOn = dataConfig.getBoolean("players." + uuidStr + ".timer_running", false);
                playerTime.put(uuid, time);
                hardcoreMap.put(uuid, hardcore);
                timerRunning.put(uuid, timerOn);
            }
        }
        plugin.getLogger().info("Дані гравців завантажено з playerdata.yml");
    }
}
