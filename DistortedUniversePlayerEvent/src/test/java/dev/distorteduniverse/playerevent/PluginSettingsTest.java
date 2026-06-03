package dev.distorteduniverse.playerevent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.SoundCategory;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class PluginSettingsTest {
    @Test
    void loadsDefaultsFromEmptyConfiguration() {
        PluginSettings settings = PluginSettings.from(new YamlConfiguration());

        assertTrue(settings.deathSound().enabled());
        assertEquals(64.0D, settings.deathSound().radius());
        assertEquals("minecraft:entity.wither.death", settings.deathSound().sound());
        assertEquals(SoundCategory.PLAYERS, settings.deathSound().category());
        assertTrue(settings.deathSound().suppressVanilla());

        assertTrue(settings.deathMessage().enabled());
        assertEquals("<death_message>", settings.deathMessage().template());
        assertTrue(settings.deathMessage().respectGamerule());

        assertTrue(settings.leaveMessage().enabled());
        assertEquals("<quit_message>", settings.leaveMessage().template());

        assertTrue(settings.joinMessage().enabled());
        assertEquals("<join_message>", settings.joinMessage().template());

        assertTrue(settings.deathKick().enabled());
        assertFalse(settings.deathKick().showLeaveMessage());
        assertEquals("You died.", settings.deathKick().kickMessage());
    }

    @Test
    void clampsUnsafeNumericValuesAndFallsBackForInvalidSoundValues() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("death-sound.radius", -5.0D);
        config.set("death-sound.volume", 500.0D);
        config.set("death-sound.pitch", 0.1D);
        config.set("death-sound.sound", "bad key");
        config.set("death-sound.category", "not_a_category");
        config.set("death-kick.delay-ticks", 5000);

        PluginSettings settings = PluginSettings.from(config);

        assertEquals(0.0D, settings.deathSound().radius());
        assertEquals(10.0F, settings.deathSound().volume());
        assertEquals(0.5F, settings.deathSound().pitch());
        assertEquals("minecraft:entity.wither.death", settings.deathSound().sound());
        assertEquals(SoundCategory.PLAYERS, settings.deathSound().category());
        assertEquals(1200, settings.deathKick().delayTicks());
    }

    @Test
    void acceptsFloatValuesWrittenByOlderCommandParser() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("death-sound.volume", 10.0F);
        config.set("death-sound.pitch", 1.5F);

        PluginSettings settings = PluginSettings.from(config);

        assertEquals(10.0F, settings.deathSound().volume());
        assertEquals(1.5F, settings.deathSound().pitch());
    }
}
