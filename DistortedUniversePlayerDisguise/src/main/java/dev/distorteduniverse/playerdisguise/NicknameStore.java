package dev.distorteduniverse.playerdisguise;

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
import org.bukkit.plugin.Plugin;

/**
 * Persistent store for custom nicknames, kept deliberately separate from {@link DisguiseStore}:
 * a nickname changes only the shown name (nametag/tab/chat/kill feed) and is independent of whether
 * the player also has a skin disguise. Nicknames take precedence over a disguise's name; the disguise
 * still supplies the skin.
 */
public final class NicknameStore {
    private final Plugin plugin;
    private final File dataFile;
    private final Map<UUID, NicknameEntry> nicknames = new LinkedHashMap<>();

    public NicknameStore(Plugin plugin) {
        this.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "nicknames.yml");
    }

    public void load() {
        nicknames.clear();
        if (!dataFile.exists()) {
            save();
            return;
        }

        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = data.getConfigurationSection("nicknames");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(key);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Ignoring invalid UUID in nicknames.yml: " + key);
                continue;
            }

            String name = section.getString(key + ".name", playerId.toString());
            String nickname = section.getString(key + ".nickname", "");
            if (nickname.isBlank()) {
                plugin.getLogger().warning("Ignoring blank nickname in nicknames.yml for UUID: " + key);
                continue;
            }
            nicknames.put(playerId, new NicknameEntry(playerId, name, nickname));
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
        for (NicknameEntry entry : nicknames.values()) {
            String basePath = "nicknames." + entry.playerId();
            data.set(basePath + ".name", entry.name());
            data.set(basePath + ".nickname", entry.nickname());
        }

        try {
            data.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save nicknames.yml.", exception);
        }
    }

    public boolean set(UUID playerId, String name, String nickname) {
        NicknameEntry existing = nicknames.get(playerId);
        NicknameEntry updated = new NicknameEntry(playerId, name, nickname);
        nicknames.put(playerId, updated);
        return !updated.equals(existing);
    }

    public boolean clear(UUID playerId) {
        return nicknames.remove(playerId) != null;
    }

    public boolean updateKnownName(UUID playerId, String name) {
        NicknameEntry existing = nicknames.get(playerId);
        if (existing == null || existing.name().equals(name)) {
            return false;
        }
        nicknames.put(playerId, new NicknameEntry(playerId, name, existing.nickname()));
        return true;
    }

    public Optional<String> nickname(UUID playerId) {
        return Optional.ofNullable(nicknames.get(playerId)).map(NicknameEntry::nickname);
    }

    public Optional<NicknameEntry> entry(UUID playerId) {
        return Optional.ofNullable(nicknames.get(playerId));
    }

    public Optional<NicknameEntry> findByName(String name) {
        String normalizedName = name.toLowerCase(Locale.ROOT);
        return nicknames.values().stream()
            .filter(entry -> entry.name().toLowerCase(Locale.ROOT).equals(normalizedName))
            .findFirst();
    }

    public List<NicknameEntry> entries() {
        List<NicknameEntry> entries = new ArrayList<>(nicknames.values());
        entries.sort(Comparator.comparing(entry -> entry.name().toLowerCase(Locale.ROOT)));
        return entries;
    }

    public int size() {
        return nicknames.size();
    }

    public record NicknameEntry(UUID playerId, String name, String nickname) {
    }
}
