package dev.distorteduniverse.immortal;

import java.util.List;
import java.util.Locale;

public final class ImmortalSettingParser {
    public static final List<String> PATHS = List.of(
        "enabled",
        "minimum-health",
        "totem-compatibility.enabled"
    );

    private ImmortalSettingParser() {
    }

    public static String normalizePath(String path) {
        return path.trim().toLowerCase(Locale.ROOT);
    }

    public static Object parseSettingValue(String path, String rawValue) {
        String normalizedPath = normalizePath(path);
        if (!PATHS.contains(normalizedPath)) {
            throw new IllegalArgumentException("Unknown setting path: " + path);
        }

        String value = rawValue.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Value cannot be empty.");
        }

        return switch (normalizedPath) {
            case "enabled", "totem-compatibility.enabled" -> parseBoolean(value);
            case "minimum-health" -> parseMinimumHealth(value);
            default -> throw new IllegalArgumentException("Unsupported setting path: " + path);
        };
    }

    public static boolean parseBoolean(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true", "on", "yes", "1", "enable", "enabled" -> true;
            case "false", "off", "no", "0", "disable", "disabled" -> false;
            default -> throw new IllegalArgumentException("Expected boolean value: true/false, on/off, yes/no.");
        };
    }

    public static double parseMinimumHealth(String value) {
        try {
            double parsed = Double.parseDouble(value.trim());
            if (!Double.isFinite(parsed)
                || parsed < ImmortalSettings.MINIMUM_ALLOWED_HEALTH
                || parsed > ImmortalSettings.MAXIMUM_ALLOWED_HEALTH) {
                throw new IllegalArgumentException(
                    "Minimum health must be between "
                        + ImmortalSettings.MINIMUM_ALLOWED_HEALTH
                        + " and "
                        + ImmortalSettings.MAXIMUM_ALLOWED_HEALTH
                        + "."
                );
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Minimum health must be a number.");
        }
    }
}
