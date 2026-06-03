package dev.distorteduniverse.immortal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DamageFloorCalculatorTest {
    @Test
    void leavesNormalDamageUnchanged() {
        double adjusted = DamageFloorCalculator.adjustedRawDamage(20.0D, 4.0D, 4.0D, 1.0D);

        assertEquals(4.0D, adjusted);
    }

    @Test
    void reducesDamageThatWouldCrossHealthFloor() {
        double adjusted = DamageFloorCalculator.adjustedRawDamage(5.0D, 8.0D, 8.0D, 1.0D);

        assertEquals(4.0D, adjusted);
    }

    @Test
    void scalesRawDamageWhenFinalDamageIsReducedByModifiers() {
        double adjusted = DamageFloorCalculator.adjustedRawDamage(3.0D, 10.0D, 5.0D, 1.0D);

        assertEquals(4.0D, adjusted);
    }

    @Test
    void scalesRawDamageWhenFinalDamageIsAmplifiedByModifiers() {
        double adjusted = DamageFloorCalculator.adjustedRawDamage(5.0D, 2.0D, 6.0D, 1.0D);

        assertEquals(4.0D / 3.0D, adjusted, 0.000001D);
    }

    @Test
    void preventsDamageAtOrBelowHealthFloor() {
        double adjusted = DamageFloorCalculator.adjustedRawDamage(1.0D, 2.0D, 2.0D, 1.0D);

        assertEquals(0.0D, adjusted);
    }

    @Test
    void neverIncreasesRawDamageWhenDamageIsAlreadySafe() {
        double adjusted = DamageFloorCalculator.adjustedRawDamage(20.0D, 2.0D, 6.0D, 1.0D);

        assertEquals(2.0D, adjusted);
    }

    @Test
    void detectsLethalDamageForTotemCompatibility() {
        assertTrue(DamageFloorCalculator.wouldKill(4.0D, 4.0D));
        assertTrue(DamageFloorCalculator.wouldKill(4.0D, 5.0D));
        assertFalse(DamageFloorCalculator.wouldKill(4.0D, 3.5D));
    }
}
