package dev.distorteduniverse.team;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TeamPlayerStore {
    private final File dataFile;
    private final Map<UUID, PlayerData> players = new HashMap<>();

    public TeamPlayerStore(File dataFolder) {
        this.dataFile = new File(dataFolder, "players.yml");
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

        for (String uuidStr : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidStr);
                if (playerSection == null) {
                    continue;
                }

                String name = playerSection.getString("name", "Unknown");
                String teamId = playerSection.getString("team", null);
                long joinedAt = playerSection.getLong("joined-at", System.currentTimeMillis());

                players.put(uuid, new PlayerData(uuid, name, teamId, joinedAt));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("data-version", 1);

        ConfigurationSection playersSection = config.createSection("players");
        for (Map.Entry<UUID, PlayerData> entry : players.entrySet()) {
            ConfigurationSection playerSection = playersSection.createSection(entry.getKey().toString());
            playerSection.set("name", entry.getValue().name());
            playerSection.set("team", entry.getValue().teamId());
            playerSection.set("joined-at", entry.getValue().joinedAt());
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save player data", e);
        }
    }

    public void update(UUID uuid, String name, String teamId) {
        PlayerData existing = players.get(uuid);
        long joinedAt = existing != null ? existing.joinedAt() : System.currentTimeMillis();
        players.put(uuid, new PlayerData(uuid, name, teamId, joinedAt));
    }

    public void remove(UUID uuid) {
        players.remove(uuid);
    }

    public Optional<PlayerData> get(UUID uuid) {
        return Optional.ofNullable(players.get(uuid));
    }

    public Optional<String> getTeamId(UUID uuid) {
        PlayerData data = players.get(uuid);
        return Optional.ofNullable(data != null ? data.teamId() : null);
    }

    public Collection<PlayerData> getAll() {
        return players.values();
    }

    public record PlayerData(
        UUID uuid,
        String name,
        String teamId,
        long joinedAt
    ) {}
}
