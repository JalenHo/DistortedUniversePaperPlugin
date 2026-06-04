package dev.distorteduniverse.playerdisguise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DisguiseSettingParserTest {
    @Test
    void parsesBooleanAliasesAndCacheDays() {
        assertTrue(DisguiseSettingParser.parseBoolean("on"));
        assertTrue(DisguiseSettingParser.parseBoolean("enabled"));
        assertFalse(DisguiseSettingParser.parseBoolean("off"));
        assertFalse(DisguiseSettingParser.parseBoolean("disabled"));
        assertEquals(7, DisguiseSettingParser.parseSettingValue("profile-lookup.cache-days", "7"));
    }

    @Test
    void validatesUsernamesAndRejectsBadSettings() {
        assertEquals("Notch", DisguiseSettingParser.validateUsername("Notch"));
        assertEquals("Name_123", DisguiseSettingParser.validateUsername("Name_123"));

        assertThrows(IllegalArgumentException.class, () -> DisguiseSettingParser.validateUsername("ab"));
        assertThrows(IllegalArgumentException.class, () -> DisguiseSettingParser.validateUsername("name-with-dash"));
        assertThrows(IllegalArgumentException.class, () -> DisguiseSettingParser.parseSettingValue("profile-lookup.cache-days", "500"));
        assertThrows(IllegalArgumentException.class, () -> DisguiseSettingParser.parseSettingValue("unknown.path", "true"));
    }
}

