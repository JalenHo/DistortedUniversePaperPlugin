package dev.distorteduniverse.immortal;

import org.bukkit.plugin.java.JavaPlugin;

public final class ImmortalSettingsService {
    private final JavaPlugin plugin;
    private ImmortalSettings settings;

    public ImmortalSettingsService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        settings = ImmortalSettings.from(plugin.getConfig());
    }

    public void reload() {
        plugin.reloadConfig();
        settings = ImmortalSettings.from(plugin.getConfig());
    }

    public void save() {
        settings.writeTo(plugin.getConfig());
        plugin.saveConfig();
    }

    public ImmortalSettings settings() {
        return settings;
    }

    public SettingResult set(String path, String rawValue) {
        String normalizedPath = ImmortalSettingParser.normalizePath(path);
        Object parsedValue;
        try {
            parsedValue = ImmortalSettingParser.parseSettingValue(normalizedPath, rawValue);
        } catch (IllegalArgumentException exception) {
            return SettingResult.failure(exception.getMessage());
        }

        plugin.getConfig().set(normalizedPath, parsedValue);
        settings = ImmortalSettings.from(plugin.getConfig());
        save();
        return SettingResult.success(normalizedPath + " = " + settings.valueAsString(normalizedPath));
    }

    public SettingResult get(String path) {
        String normalizedPath = ImmortalSettingParser.normalizePath(path);
        if (!ImmortalSettingParser.PATHS.contains(normalizedPath)) {
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
