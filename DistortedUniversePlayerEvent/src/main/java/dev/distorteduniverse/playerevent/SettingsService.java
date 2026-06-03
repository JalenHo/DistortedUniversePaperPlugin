package dev.distorteduniverse.playerevent;

import org.bukkit.plugin.java.JavaPlugin;

public final class SettingsService {
    private final JavaPlugin plugin;
    private PluginSettings settings;

    public SettingsService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        settings = PluginSettings.from(plugin.getConfig());
    }

    public void reload() {
        plugin.reloadConfig();
        settings = PluginSettings.from(plugin.getConfig());
    }

    public void save() {
        settings.writeTo(plugin.getConfig());
        plugin.saveConfig();
    }

    public PluginSettings settings() {
        return settings;
    }

    public SettingResult set(String path, String rawValue) {
        String normalizedPath = SettingValueParser.normalizePath(path);
        Object parsedValue;
        try {
            parsedValue = SettingValueParser.parseSettingValue(normalizedPath, rawValue);
        } catch (IllegalArgumentException exception) {
            return SettingResult.failure(exception.getMessage());
        }

        plugin.getConfig().set(normalizedPath, parsedValue);
        settings = PluginSettings.from(plugin.getConfig());
        save();
        return SettingResult.success(normalizedPath + " = " + settings.valueAsString(normalizedPath));
    }

    public SettingResult setModuleEnabled(String module, boolean enabled) {
        String path;
        try {
            path = SettingValueParser.enabledPathForModule(module);
        } catch (IllegalArgumentException exception) {
            return SettingResult.failure(exception.getMessage());
        }
        return set(path, Boolean.toString(enabled));
    }

    public SettingResult get(String path) {
        String normalizedPath = SettingValueParser.normalizePath(path);
        if (!SettingValueParser.PATHS.contains(normalizedPath)) {
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
