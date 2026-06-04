package dev.distorteduniverse.playerdisguise;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class DisguiseSettingsTest {
    @Test
    void loadsDefaultsFromEmptyConfiguration() {
        DisguiseSettings settings = DisguiseSettings.from(new YamlConfiguration());

        assertTrue(settings.enabled());
        assertFalse(settings.visibility().selfSeesDisguise());
        assertTrue(settings.profileLookup().refreshExpiredCache());
        assertTrue(settings.apply().chatDisplayName());
        assertTrue(settings.apply().tabListName());
        assertTrue(settings.apply().protocolProfile());
    }

    @Test
    void detectsExpiredProfileCache() {
        DisguiseSettings settings = DisguiseSettings.from(new YamlConfiguration());
        long now = 1_000_000_000L;

        assertFalse(settings.isCacheExpired(now - 86_400_000L, now));
        assertTrue(settings.isCacheExpired(now - (8L * 86_400_000L), now));
    }
}

