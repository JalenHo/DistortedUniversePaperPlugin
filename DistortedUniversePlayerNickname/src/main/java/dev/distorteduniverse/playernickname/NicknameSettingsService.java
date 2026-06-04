package dev.distorteduniverse.playernickname;

import org.bukkit.plugin.java.JavaPlugin;

public final class NicknameSettingsService {
    private final JavaPlugin plugin;
    private NicknameSettings settings;

    public NicknameSettingsService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        settings = NicknameSettings.from(plugin.getConfig());
    }

    public void reload() {
        plugin.reloadConfig();
        settings = NicknameSettings.from(plugin.getConfig());
    }

    public void save() {
        settings.writeTo(plugin.getConfig());
        plugin.saveConfig();
    }

    public NicknameSettings settings() {
        return settings;
    }

    public SettingResult set(String path, String rawValue) {
        String normalizedPath = NicknameSettingParser.normalizePath(path);
        Object parsedValue;
        try {
            parsedValue = NicknameSettingParser.parseSettingValue(normalizedPath, rawValue);
        } catch (IllegalArgumentException exception) {
            return SettingResult.failure(exception.getMessage());
        }

        plugin.getConfig().set(normalizedPath, parsedValue);
        settings = NicknameSettings.from(plugin.getConfig());
        save();
        return SettingResult.success(normalizedPath + " = " + settings.valueAsString(normalizedPath));
    }

    public SettingResult get(String path) {
        String normalizedPath = NicknameSettingParser.normalizePath(path);
        if (!NicknameSettingParser.PATHS.contains(normalizedPath)) {
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

