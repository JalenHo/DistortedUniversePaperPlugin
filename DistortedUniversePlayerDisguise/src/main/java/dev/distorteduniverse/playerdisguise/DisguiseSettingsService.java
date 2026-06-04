package dev.distorteduniverse.playerdisguise;

import org.bukkit.plugin.java.JavaPlugin;

public final class DisguiseSettingsService {
    private final JavaPlugin plugin;
    private DisguiseSettings settings;

    public DisguiseSettingsService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        settings = DisguiseSettings.from(plugin.getConfig());
    }

    public void reload() {
        plugin.reloadConfig();
        settings = DisguiseSettings.from(plugin.getConfig());
    }

    public void save() {
        settings.writeTo(plugin.getConfig());
        plugin.saveConfig();
    }

    public DisguiseSettings settings() {
        return settings;
    }

    public SettingResult set(String path, String rawValue) {
        String normalizedPath = DisguiseSettingParser.normalizePath(path);
        Object parsedValue;
        try {
            parsedValue = DisguiseSettingParser.parseSettingValue(normalizedPath, rawValue);
        } catch (IllegalArgumentException exception) {
            return SettingResult.failure(exception.getMessage());
        }

        plugin.getConfig().set(normalizedPath, parsedValue);
        settings = DisguiseSettings.from(plugin.getConfig());
        save();
        return SettingResult.success(normalizedPath + " = " + settings.valueAsString(normalizedPath));
    }

    public SettingResult get(String path) {
        String normalizedPath = DisguiseSettingParser.normalizePath(path);
        if (!DisguiseSettingParser.PATHS.contains(normalizedPath)) {
            return SettingResult.failure("Unknown setting path: " + path);
        }
        return SettingResult.success(normalizedPath + " = " + settings.valueAsString(normalizedPath));
    }

    public record SettingResult(boolean success, String message) {
        public static SettingResult success(String message) {
            return new SettingResult(true, message);
        }

        public static SettingResult failure(String message) {
            return new SettingResult(false, message);
        }
    }
}

