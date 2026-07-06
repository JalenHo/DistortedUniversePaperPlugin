package dev.distorteduniverse.fakeplayer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FakePlayerStore {
    private final File dataFile;
    private final Map<String, FakePlayer> players = new HashMap<>();

    public FakePlayerStore(File dataFolder) {
        this.dataFile = new File(dataFolder, "fakeplayers.yml");
    }

    public void load() {
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection playersSection = config.getConfigurationSection("players");
        if (playersSection == null) {
            return;
        }

        for (String key : playersSection.getKeys(false)) {
            ConfigurationSection playerSection = playersSection.getConfigurationSection(key);
            if (playerSection == null) {
                continue;
            }

            String name = playerSection.getString("name", key);
            UUID uuid = UUID.fromString(playerSection.getString("uuid", UUID.randomUUID().toString()));
            Location location = new Location(
                Bukkit.getWorld(playerSection.getString("world", "world")),
                playerSection.getDouble("x", 0),
                playerSection.getDouble("y", 64),
                playerSection.getDouble("z", 0),
                (float) playerSection.getDouble("yaw", 0),
                (float) playerSection.getDouble("pitch", 0)
            );
            String skin = playerSection.getString("skin", "default");
            boolean isWandering = playerSection.getBoolean("wandering", false);

            players.put(key, new FakePlayer(name, uuid, location, skin, isWandering));
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();

        ConfigurationSection playersSection = config.createSection("players");
        for (Map.Entry<String, FakePlayer> entry : players.entrySet()) {
            FakePlayer player = entry.getValue();
            ConfigurationSection playerSection = playersSection.createSection(entry.getKey());

            playerSection.set("name", player.name());
            playerSection.set("uuid", player.uuid().toString());
            playerSection.set("world", player.location().getWorld().getName());
            playerSection.set("x", player.location().getX());
            playerSection.set("y", player.location().getY());
            playerSection.set("z", player.location().getZ());
            playerSection.set("yaw", player.location().getYaw());
            playerSection.set("pitch", player.location().getPitch());
            playerSection.set("skin", player.skin());
            playerSection.set("wandering", player.isWandering());
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save fake players data", e);
        }
    }

    public void add(String key, FakePlayer player) {
        players.put(key, player);
    }

    public void remove(String key) {
        players.remove(key);
    }

    public Optional<FakePlayer> get(String key) {
        return Optional.ofNullable(players.get(key));
    }

    public Collection<FakePlayer> getAll() {
        return players.values();
    }

    public Set<String> getKeys() {
        return players.keySet();
    }

    public void update(String key, FakePlayer player) {
        players.put(key, player);
    }

    public boolean contains(String key) {
        return players.containsKey(key);
    }
}
