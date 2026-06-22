package com.timehardcore;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TimeManager {

    private final JavaPlugin plugin;
    private final File dataFile;
    private FileConfiguration data;
    private Map<UUID, Long> playerTimes = new HashMap<>();
    private Map<UUID, Boolean> playerHardcore = new HashMap<>();
    private Map<UUID, Boolean> timerRunning = new HashMap<>();

    public TimeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        loadData();
    }

    private void loadData() {
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void loadPlayer(UUID uuid) {
        String uuidStr = uuid.toString();
        long time = data.getLong("players." + uuidStr + ".time", 0);
        boolean hardcore = data.getBoolean("players." + uuidStr + ".hardcore", false);
        boolean running = data.getBoolean("players." + uuidStr + ".timer_running", false);
        
        playerTimes.put(uuid, time);
        playerHardcore.put(uuid, hardcore);
        timerRunning.put(uuid, running);
    }

    public long getTime(UUID uuid) {
        return playerTimes.getOrDefault(uuid, 0L);
    }

    public void addTime(UUID uuid, long seconds) {
        long currentTime = getTime(uuid);
        playerTimes.put(uuid, currentTime + seconds);
        savePlayer(uuid);
    }

    public void setTime(UUID uuid, long seconds) {
        playerTimes.put(uuid, seconds);
        savePlayer(uuid);
    }

    public boolean isHardcore(UUID uuid) {
        return playerHardcore.getOrDefault(uuid, false);
    }

    public void setHardcore(UUID uuid, boolean hardcore) {
        playerHardcore.put(uuid, hardcore);
        savePlayer(uuid);
    }

    public boolean isTimerRunning(UUID uuid) {
        return timerRunning.getOrDefault(uuid, false);
    }

    public void startTimer(UUID uuid) {
        timerRunning.put(uuid, true);
        if (!playerTimes.containsKey(uuid)) {
            playerTimes.put(uuid, 0L);
        }
        savePlayer(uuid);
    }

    public void stopTimer(UUID uuid) {
        timerRunning.put(uuid, false);
        savePlayer(uuid);
    }

    private void savePlayer(UUID uuid) {
        String uuidStr = uuid.toString();
        data.set("players." + uuidStr + ".time", playerTimes.get(uuid));
        data.set("players." + uuidStr + ".hardcore", playerHardcore.get(uuid));
        data.set("players." + uuidStr + ".timer_running", timerRunning.get(uuid));
        
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Не вдалося зберегти дані для " + uuid);
            e.printStackTrace();
        }
    }

    public void saveAll() {
        for (UUID uuid : playerTimes.keySet()) {
            savePlayer(uuid);
        }
    }

    public void resetAll() {
        playerTimes.clear();
        playerHardcore.clear();
        timerRunning.clear();
        
        if (dataFile.exists()) {
            dataFile.delete();
        }
        loadData();
    }
}
