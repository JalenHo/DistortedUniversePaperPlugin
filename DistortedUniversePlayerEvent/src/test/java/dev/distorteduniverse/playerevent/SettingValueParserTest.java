package dev.distorteduniverse.playerevent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SettingValueParserTest {
    @Test
    void parsesBooleanAliases() {
        assertTrue(SettingValueParser.parseBoolean("on"));
        assertTrue(SettingValueParser.parseBoolean("enabled"));
        assertFalse(SettingValueParser.parseBoolean("off"));
        assertFalse(SettingValueParser.parseBoolean("disabled"));
        assertThrows(IllegalArgumentException.class, () -> SettingValueParser.parseBoolean("maybe"));
    }

    @Test
    void normalizesSoundKeys() {
        assertEquals("minecraft:entity.player.death", SettingValueParser.normalizeSoundKey("entity.player.death"));
        assertEquals("custom:death.boom", SettingValueParser.normalizeSoundKey("CUSTOM:Death.Boom"));
    }

    @Test
    void rejectsInvalidCommandValues() {
        assertThrows(
            IllegalArgumentException.class,
            () -> SettingValueParser.parseSettingValue("death-sound.radius", "-1")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> SettingValueParser.parseSettingValue("death-sound.sound", "bad key")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> SettingValueParser.parseSettingValue("unknown.path", "true")
        );
    }

    @Test
    void parsesTypedCommandValues() {
        assertEquals(true, SettingValueParser.parseSettingValue("death-message.enabled", "yes"));
        assertEquals(48.5D, SettingValueParser.parseSettingValue("leave-message.radius", "48.5"));
        assertEquals(48.5D, SettingValueParser.parseSettingValue("join-message.radius", "48.5"));
        assertEquals("<join_message>", SettingValueParser.parseSettingValue("join-message.template", "<join_message>"));
        assertEquals(10.0D, SettingValueParser.parseSettingValue("death-sound.volume", "10"));
        assertEquals(1.5D, SettingValueParser.parseSettingValue("death-sound.pitch", "1.5"));
        assertEquals("PLAYERS", SettingValueParser.parseSettingValue("death-sound.category", "players"));
        assertEquals(20, SettingValueParser.parseSettingValue("death-kick.delay-ticks", "20"));
    }
}
