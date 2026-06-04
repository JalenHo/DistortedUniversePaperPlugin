package dev.distorteduniverse.playernickname;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class NicknameSettingsTest {
    @Test
    void loadsDefaultsFromEmptyConfiguration() {
        NicknameSettings settings = NicknameSettings.from(new YamlConfiguration());

        assertTrue(settings.enabled());
        assertTrue(settings.apply().chatDisplayName());
        assertTrue(settings.apply().tabListName());
        assertEquals(NicknameSettings.AboveHeadMode.TEXT_DISPLAY, settings.aboveHead().mode());
        assertTrue(settings.aboveHead().hideVanillaName());
        assertEquals(2.25D, settings.aboveHead().yOffset());
        assertEquals(2, settings.aboveHead().updateIntervalTicks());
        assertEquals(64.0D, settings.aboveHead().viewRange());
        assertFalse(settings.scoreboard().overrideExistingTeams());
        assertEquals(32, settings.validation().maxPlainLength());
    }

    @Test
    void clampsUnsafeValuesAndFallsBackForInvalidMode() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("above-head.mode", "packet");
        config.set("above-head.y-offset", 50.0D);
        config.set("above-head.update-interval-ticks", 0);
        config.set("above-head.view-range", 500.0D);
        config.set("validation.min-plain-length", 100);
        config.set("validation.max-plain-length", 1);

        NicknameSettings settings = NicknameSettings.from(config);

        assertEquals(NicknameSettings.AboveHeadMode.TEXT_DISPLAY, settings.aboveHead().mode());
        assertEquals(8.0D, settings.aboveHead().yOffset());
        assertEquals(1, settings.aboveHead().updateIntervalTicks());
        assertEquals(256.0D, settings.aboveHead().viewRange());
        assertEquals(64, settings.validation().minPlainLength());
        assertEquals(64, settings.validation().maxPlainLength());
    }
}

