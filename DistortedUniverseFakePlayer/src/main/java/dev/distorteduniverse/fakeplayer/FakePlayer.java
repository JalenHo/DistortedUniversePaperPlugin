package dev.distorteduniverse.fakeplayer;

import org.bukkit.Location;

import java.util.UUID;

public record FakePlayer(
    String name,
    UUID uuid,
    Location location,
    String skin,
    boolean isWandering
) {
    public FakePlayer withName(String newName) {
        return new FakePlayer(newName, uuid, location, skin, isWandering);
    }

    public FakePlayer withLocation(Location newLocation) {
        return new FakePlayer(name, uuid, newLocation, skin, isWandering);
    }

    public FakePlayer withSkin(String newSkin) {
        return new FakePlayer(name, uuid, location, newSkin, isWandering);
    }

    public FakePlayer withWandering(boolean wandering) {
        return new FakePlayer(name, uuid, location, skin, wandering);
    }
}
