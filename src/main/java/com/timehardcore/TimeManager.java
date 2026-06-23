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

    public void addTime(UUID uuid, long seconds) {
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
    }

    public void stopTimer(UUID uuid) {
        timerRunning.put(uuid, false);
    }

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
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    long time = dataConfig.getLong("players." + uuidStr + ".time", 0L);
                    boolean hardcore = dataConfig.getBoolean("players." + uuidStr + ".hardcore", false);
                    boolean timerOn = dataConfig.getBoolean("players." + uuidStr + ".timer_running", false);
                    playerTime.put(uuid, time);
                    hardcoreMap.put(uuid, hardcore);
                    timerRunning.put(uuid, timerOn);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Невірний UUID у playerdata.yml: " + uuidStr);
                }
            }
        }
        plugin.getLogger().info("Дані гравців завантажено з playerdata.yml");
    }
}
