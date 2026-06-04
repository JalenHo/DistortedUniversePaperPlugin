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

public final class DisguiseStore {
    private final Plugin plugin;
    private final File dataFile;
    private final Map<UUID, DisguiseEntry> disguises = new LinkedHashMap<>();

    public DisguiseStore(Plugin plugin) {
        this.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        disguises.clear();
        if (!dataFile.exists()) {
            save();
            return;
        }

        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = data.getConfigurationSection("disguises");
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

            UUID sourceId = parseUuid(section.getString(key + ".source-uuid"));
            String name = section.getString(key + ".name", playerId.toString());
            String sourceName = section.getString(key + ".source-name", "");
            String profileName = section.getString(key + ".profile-name", sourceName);
            String textureValue = section.getString(key + ".texture-value", "");
            String textureSignature = section.getString(key + ".texture-signature", "");
            long cachedAt = section.getLong(key + ".cached-at-epoch-millis", 0L);
            if (sourceId == null || profileName.isBlank() || textureValue.isBlank()) {
                plugin.getLogger().warning("Ignoring incomplete disguise in data.yml for UUID: " + key);
                continue;
            }
            disguises.put(playerId, new DisguiseEntry(
                playerId,
                name,
                sourceName,
                sourceId,
                profileName,
                textureValue,
                textureSignature,
                cachedAt
            ));
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
        for (DisguiseEntry entry : disguises.values()) {
            String basePath = "disguises." + entry.playerId();
            data.set(basePath + ".name", entry.name());
            data.set(basePath + ".source-name", entry.sourceName());
            data.set(basePath + ".source-uuid", entry.sourceId().toString());
            data.set(basePath + ".profile-name", entry.profileName());
            data.set(basePath + ".texture-value", entry.textureValue());
            data.set(basePath + ".texture-signature", entry.textureSignature());
            data.set(basePath + ".cached-at-epoch-millis", entry.cachedAtEpochMillis());
        }

        try {
            data.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data.yml.", exception);
        }
    }

    public boolean setDisguise(DisguiseEntry entry) {
        DisguiseEntry existing = disguises.get(entry.playerId());
        disguises.put(entry.playerId(), entry);
        return !entry.equals(existing);
    }

    public boolean clearDisguise(UUID playerId) {
        return disguises.remove(playerId) != null;
    }

    public boolean updateKnownName(UUID playerId, String name) {
        DisguiseEntry existing = disguises.get(playerId);
        if (existing == null || existing.name().equals(name)) {
            return false;
        }
        disguises.put(playerId, existing.withName(name));
        return true;
    }

    public Optional<DisguiseEntry> entry(UUID playerId) {
        return Optional.ofNullable(disguises.get(playerId));
    }

    public Optional<DisguiseEntry> findByName(String name) {
        String normalizedName = name.toLowerCase(Locale.ROOT);
        return disguises.values().stream()
            .filter(entry -> entry.name().toLowerCase(Locale.ROOT).equals(normalizedName))
            .findFirst();
    }

    public List<DisguiseEntry> entries() {
        List<DisguiseEntry> entries = new ArrayList<>(disguises.values());
        entries.sort(Comparator.comparing(entry -> entry.name().toLowerCase(Locale.ROOT)));
        return entries;
    }

    public List<String> knownNames() {
        return entries().stream().map(DisguiseEntry::name).toList();
    }

    public int size() {
        return disguises.size();
    }

    private static UUID parseUuid(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(rawValue);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public record DisguiseEntry(
        UUID playerId,
        String name,
        String sourceName,
        UUID sourceId,
        String profileName,
        String textureValue,
        String textureSignature,
        long cachedAtEpochMillis
    ) {
        public DisguiseEntry withName(String newName) {
            return new DisguiseEntry(
                playerId,
                newName,
                sourceName,
                sourceId,
                profileName,
                textureValue,
                textureSignature,
                cachedAtEpochMillis
            );
        }

        public DisguiseEntry withProfile(ProfileLookupService.ResolvedProfile profile) {
            return new DisguiseEntry(
                playerId,
                name,
                profile.sourceName(),
                profile.sourceId(),
                profile.profileName(),
                profile.textureValue(),
                profile.textureSignature(),
                profile.cachedAtEpochMillis()
            );
        }
    }
}
