package dev.distorteduniverse.immortal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ImmortalSettingParserTest {
    @Test
    void parsesBooleanAliases() {
        assertTrue(ImmortalSettingParser.parseBoolean("enabled"));
        assertTrue(ImmortalSettingParser.parseBoolean("yes"));
        assertFalse(ImmortalSettingParser.parseBoolean("disabled"));
        assertFalse(ImmortalSettingParser.parseBoolean("off"));
        assertThrows(IllegalArgumentException.class, () -> ImmortalSettingParser.parseBoolean("maybe"));
    }

    @Test
    void parsesMinimumHealth() {
        assertEquals(0.5D, ImmortalSettingParser.parseMinimumHealth("0.5"));
        assertEquals(1.0D, ImmortalSettingParser.parseMinimumHealth("1"));
        assertEquals(20.0D, ImmortalSettingParser.parseMinimumHealth("20.0"));
    }

    @Test
    void rejectsInvalidSettingValues() {
        assertThrows(IllegalArgumentException.class, () -> ImmortalSettingParser.parseSettingValue("minimum-health", "0"));
        assertThrows(IllegalArgumentException.class, () -> ImmortalSettingParser.parseSettingValue("minimum-health", "NaN"));
        assertThrows(IllegalArgumentException.class, () -> ImmortalSettingParser.parseSettingValue("unknown", "true"));
    }

    @Test
    void parsesTypedCommandValues() {
        assertEquals(true, ImmortalSettingParser.parseSettingValue("enabled", "on"));
        assertEquals(false, ImmortalSettingParser.parseSettingValue("totem-compatibility.enabled", "off"));
        assertEquals(2.0D, ImmortalSettingParser.parseSettingValue("minimum-health", "2"));
    }
}
