package dev.distorteduniverse.fakeplayer;

import org.bukkit.Location;

import java.util.Optional;

public class FakePlayerLifecycleService {
    public enum RemovalReason {
        DESPAWN,
        DEATH
    }

    private final FakePlayerStore store;
    private final FakePlayerManager manager;
    private final BotMovementService movementService;
    private final FakePlayerBroadcastService broadcastService;

    public FakePlayerLifecycleService(
        FakePlayerStore store,
        FakePlayerManager manager,
        BotMovementService movementService,
        FakePlayerBroadcastService broadcastService
    ) {
        this.store = store;
        this.manager = manager;
        this.movementService = movementService;
        this.broadcastService = broadcastService;
    }

    public boolean spawn(String key, FakePlayer fakePlayer) {
        if (!manager.spawnFakePlayer(fakePlayer)) {
            return false;
        }

        Location location = manager.getPlayer(fakePlayer.uuid())
            .map(player -> player.getLocation())
            .orElse(fakePlayer.location());
        broadcastService.broadcastJoin(fakePlayer.name(), location);
        return true;
    }

    public Optional<FakePlayer> remove(String identifier, RemovalReason reason) {
        Optional<String> key = store.findKey(identifier);
        if (key.isEmpty()) {
            return Optional.empty();
        }

        Optional<FakePlayer> fakePlayer = store.get(key.get());
        if (fakePlayer.isEmpty()) {
            return Optional.empty();
        }

        FakePlayer fp = fakePlayer.get();
        Location location = manager.getPlayer(fp.uuid())
            .map(player -> player.getLocation())
            .orElse(fp.location());

        switch (reason) {
            case DEATH -> {
                broadcastService.broadcastDeath(fp.name(), location);
                broadcastService.broadcastLeave(fp.name(), location, true);
            }
            case DESPAWN -> broadcastService.broadcastLeave(fp.name(), location, false);
        }

        if (reason == RemovalReason.DEATH) {
            manager.forceDespawnFakePlayer(fp.uuid());
        } else if (manager.isSpawned(fp.uuid())) {
            manager.despawnFakePlayer(fp.uuid());
        }
        movementService.stop(fp.uuid());
        store.remove(key.get());
        store.save();

        return Optional.of(fp);
    }
}
