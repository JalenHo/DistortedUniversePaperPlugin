package dev.distorteduniverse.playerdisguise;

import java.util.List;
import java.util.Locale;

public final class DisguiseSettingParser {
    public static final List<String> PATHS = List.of(
        "enabled",
        "visibility.self-sees-disguise",
        "profile-lookup.cache-days",
        "profile-lookup.refresh-expired-cache",
        "apply.chat-display-name",
        "apply.tab-list-name",
        "apply.protocol-profile"
    );

    private DisguiseSettingParser() {
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
            case "enabled",
                "visibility.self-sees-disguise",
                "profile-lookup.refresh-expired-cache",
                "apply.chat-display-name",
                "apply.tab-list-name",
                "apply.protocol-profile" -> parseBoolean(value);
            case "profile-lookup.cache-days" -> parseInt(value, 0, 365, "Cache days");
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

    public static String validateUsername(String rawUsername) {
        String username = rawUsername.trim();
        if (!username.matches("[A-Za-z0-9_]{3,16}")) {
            throw new IllegalArgumentException("Minecraft username must be 3-16 letters, numbers, or underscores.");
        }
        return username;
    }

    public static String validateNickname(String rawNickname) {
        String nickname = rawNickname.trim();
        if (!nickname.matches("[A-Za-z0-9_]{1,16}")) {
            throw new IllegalArgumentException("Nickname must be 1-16 letters, numbers, or underscores.");
        }
        return nickname;
    }

    private static int parseInt(String value, int min, int max, String label) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < min || parsed > max) {
                throw new IllegalArgumentException(label + " must be between " + min + " and " + max + ".");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be a whole number.");
        }
    }
}

