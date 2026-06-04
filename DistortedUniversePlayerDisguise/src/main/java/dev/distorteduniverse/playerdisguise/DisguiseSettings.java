package dev.distorteduniverse.playerdisguise;

import org.bukkit.configuration.ConfigurationSection;

public record DisguiseSettings(
    boolean enabled,
    VisibilitySettings visibility,
    ProfileLookupSettings profileLookup,
    ApplySettings apply
) {
    public static DisguiseSettings from(ConfigurationSection config) {
        return new DisguiseSettings(
            getBoolean(config, "enabled", true),
            new VisibilitySettings(getBoolean(config, "visibility.self-sees-disguise", false)),
            new ProfileLookupSettings(
                getInt(config, "profile-lookup.cache-days", 7, 0, 365),
                getBoolean(config, "profile-lookup.refresh-expired-cache", true)
            ),
            new ApplySettings(
                getBoolean(config, "apply.chat-display-name", true),
                getBoolean(config, "apply.tab-list-name", true),
                getBoolean(config, "apply.protocol-profile", true)
            )
        );
    }

    public void writeTo(ConfigurationSection config) {
        config.set("config-version", 1);
        config.set("enabled", enabled);
        config.set("visibility.self-sees-disguise", visibility.selfSeesDisguise());
        config.set("profile-lookup.cache-days", profileLookup.cacheDays());
        config.set("profile-lookup.refresh-expired-cache", profileLookup.refreshExpiredCache());
        config.set("apply.chat-display-name", apply.chatDisplayName());
        config.set("apply.tab-list-name", apply.tabListName());
        config.set("apply.protocol-profile", apply.protocolProfile());
    }

    public String valueAsString(String path) {
        return switch (path) {
            case "enabled" -> Boolean.toString(enabled);
            case "visibility.self-sees-disguise" -> Boolean.toString(visibility.selfSeesDisguise());
            case "profile-lookup.cache-days" -> Integer.toString(profileLookup.cacheDays());
            case "profile-lookup.refresh-expired-cache" -> Boolean.toString(profileLookup.refreshExpiredCache());
            case "apply.chat-display-name" -> Boolean.toString(apply.chatDisplayName());
            case "apply.tab-list-name" -> Boolean.toString(apply.tabListName());
            case "apply.protocol-profile" -> Boolean.toString(apply.protocolProfile());
            default -> throw new IllegalArgumentException("Unknown setting path: " + path);
        };
    }

    public boolean isCacheExpired(long cachedAtEpochMillis, long nowEpochMillis) {
        int cacheDays = profileLookup.cacheDays();
        if (cacheDays <= 0) {
            return false;
        }
        long maxAgeMillis = cacheDays * 86_400_000L;
        return cachedAtEpochMillis <= 0L || nowEpochMillis - cachedAtEpochMillis > maxAgeMillis;
    }

    private static boolean getBoolean(ConfigurationSection config, String path, boolean fallback) {
        return config.isBoolean(path) ? config.getBoolean(path) : fallback;
    }

    private static int getInt(ConfigurationSection config, String path, int fallback, int min, int max) {
        if (!config.isInt(path)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, config.getInt(path)));
    }

    public record VisibilitySettings(boolean selfSeesDisguise) {
    }

    public record ProfileLookupSettings(int cacheDays, boolean refreshExpiredCache) {
    }

    public record ApplySettings(boolean chatDisplayName, boolean tabListName, boolean protocolProfile) {
    }
}

