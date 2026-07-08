package dev.distorteduniverse.fakeplayer;

import org.bukkit.Location;

import java.util.UUID;

public record FakePlayer(
    String name,
    UUID uuid,
    Location location,
    String skin,
    boolean isWandering,
    Boolean invulnerableOverride
) {
    public FakePlayer(String name, UUID uuid, Location location, String skin, boolean isWandering) {
        this(name, uuid, location, skin, isWandering, null);
    }

    public FakePlayer withName(String newName) {
        return new FakePlayer(newName, uuid, location, skin, isWandering, invulnerableOverride);
    }

    public FakePlayer withLocation(Location newLocation) {
        return new FakePlayer(name, uuid, newLocation, skin, isWandering, invulnerableOverride);
    }

    public FakePlayer withSkin(String newSkin) {
        return new FakePlayer(name, uuid, location, newSkin, isWandering, invulnerableOverride);
    }

    public FakePlayer withWandering(boolean wandering) {
        return new FakePlayer(name, uuid, location, skin, wandering, invulnerableOverride);
    }

    public FakePlayer withInvulnerableOverride(Boolean override) {
        return new FakePlayer(name, uuid, location, skin, isWandering, override);
    }

    public boolean resolvesInvulnerable(boolean globalDefault) {
        return invulnerableOverride != null ? invulnerableOverride : globalDefault;
    }
}
