package dev.distorteduniverse.immortal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ImmortalPlayerStore {
    private final JavaPlugin plugin;
    private final File dataFile;
    private final Map<UUID, ImmortalPlayerEntry> players = new LinkedHashMap<>();

    public ImmortalPlayerStore(JavaPlugin plugin) {
        this.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        players.clear();
        if (!dataFile.exists()) {
            save();
            return;
        }

        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = data.getConfigurationSection("immortal-players");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(key);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Ignoring invalid UUID in data.yml: " + key);
                continue;
            }

            String name = section.getString(key + ".name", playerId.toString());
            players.put(playerId, new ImmortalPlayerEntry(playerId, name));
        }
    }

    public void save() {
        File parent = dataFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder: " + parent);
            return;
        }

        YamlConfiguration data = new YamlConfiguration();
        data.set("data-version", 1);
        for (ImmortalPlayerEntry entry : players.values()) {
            String basePath = "immortal-players." + entry.playerId();
            data.set(basePath + ".name", entry.name());
        }

        try {
            data.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data.yml.", exception);
        }
    }

    public boolean isImmortal(UUID playerId) {
        return players.containsKey(playerId);
    }

    public boolean setImmortal(UUID playerId, String name) {
        ImmortalPlayerEntry existing = players.get(playerId);
        ImmortalPlayerEntry entry = new ImmortalPlayerEntry(playerId, name);
        players.put(playerId, entry);
        return !entry.equals(existing);
    }

    public boolean unsetImmortal(UUID playerId) {
        return players.remove(playerId) != null;
    }

    public boolean updateKnownName(UUID playerId, String name) {
        ImmortalPlayerEntry existing = players.get(playerId);
        if (existing == null || existing.name().equals(name)) {
            return false;
        }
        players.put(playerId, new ImmortalPlayerEntry(playerId, name));
        return true;
    }

    public Optional<ImmortalPlayerEntry> findByName(String name) {
        String normalizedName = name.toLowerCase(Locale.ROOT);
        return players.values().stream()
            .filter(entry -> entry.name().toLowerCase(Locale.ROOT).equals(normalizedName))
            .findFirst();
    }

    public List<ImmortalPlayerEntry> entries() {
        List<ImmortalPlayerEntry> entries = new ArrayList<>(players.values());
        entries.sort(Comparator.comparing(entry -> entry.name().toLowerCase(Locale.ROOT)));
        return entries;
    }

    public List<String> knownNames() {
        return entries().stream().map(ImmortalPlayerEntry::name).toList();
    }

    public int size() {
        return players.size();
    }

    public record ImmortalPlayerEntry(UUID playerId, String name) {
    }
}
