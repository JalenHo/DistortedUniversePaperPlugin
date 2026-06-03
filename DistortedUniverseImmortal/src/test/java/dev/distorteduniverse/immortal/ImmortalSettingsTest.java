package dev.distorteduniverse.immortal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ImmortalSettingsTest {
    @Test
    void loadsDefaultsFromEmptyConfiguration() {
        ImmortalSettings settings = ImmortalSettings.from(new YamlConfiguration());

        assertTrue(settings.enabled());
        assertEquals(1.0D, settings.minimumHealth());
        assertTrue(settings.totemCompatibilityEnabled());
    }

    @Test
    void clampsUnsafeNumericValues() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("minimum-health", -5.0D);

        ImmortalSettings settings = ImmortalSettings.from(config);

        assertEquals(0.5D, settings.minimumHealth());
    }

    @Test
    void fallsBackForNonNumericMinimumHealth() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("minimum-health", "bad");

        ImmortalSettings settings = ImmortalSettings.from(config);

        assertEquals(1.0D, settings.minimumHealth());
    }
}
