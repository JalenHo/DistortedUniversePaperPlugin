package dev.distorteduniverse.immortal;

public final class DamageFloorCalculator {
    private DamageFloorCalculator() {
    }

    public static boolean wouldKill(double currentHealth, double finalDamage) {
        return finalDamage >= currentHealth;
    }

    public static double adjustedRawDamage(
        double currentHealth,
        double rawDamage,
        double finalDamage,
        double minimumHealth
    ) {
        if (rawDamage <= 0.0D || finalDamage <= 0.0D) {
            return rawDamage;
        }

        double allowedDamage = Math.max(0.0D, currentHealth - minimumHealth);
        if (finalDamage <= allowedDamage) {
            return rawDamage;
        }
        double finalDamageRatio = allowedDamage / finalDamage;
        double adjustedRawDamage = rawDamage * finalDamageRatio;
        return Math.max(0.0D, Math.min(rawDamage, adjustedRawDamage));
    }
}
