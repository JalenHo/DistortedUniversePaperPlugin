package dev.distorteduniverse.fakeplayer;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class BotMovementService {
    private enum Mode {
        WANDER,
        MOVE_TO
    }

    private final JavaPlugin plugin;
    private final FakePlayerManager manager;
    private final FakePlayerStore store;
    private FakePlayerSettings.MovementSettings settings;
    private final Map<UUID, MovementState> activeTasks = new HashMap<>();
    private final Random random = new Random();

    public BotMovementService(
        JavaPlugin plugin,
        FakePlayerManager manager,
        FakePlayerStore store,
        FakePlayerSettings.MovementSettings settings
    ) {
        this.plugin = plugin;
        this.manager = manager;
        this.store = store;
        this.settings = settings;
    }

    public void startWandering(String key, FakePlayer fakePlayer, double radius) {
        stop(fakePlayer.uuid());

        FakePlayer updated = fakePlayer.withWandering(true);
        store.update(key, updated);

        MovementState state = new MovementState(key, updated.uuid(), Mode.WANDER, radius);
        state.target = pickWanderTarget(updated.location(), radius);
        activeTasks.put(updated.uuid(), state);
        manager.setMovementActive(updated.uuid(), true);
        state.task.runTaskTimer(plugin, 0L, settings.tickInterval());
    }

    public void startMoveTo(String key, FakePlayer fakePlayer, Location destination) {
        stop(fakePlayer.uuid());

        FakePlayer updated = fakePlayer.withWandering(false);
        store.update(key, updated);

        MovementState state = new MovementState(key, updated.uuid(), Mode.MOVE_TO, 0);
        state.target = destination.clone();
        activeTasks.put(updated.uuid(), state);
        manager.setMovementActive(updated.uuid(), true);
        state.task.runTaskTimer(plugin, 0L, settings.tickInterval());
    }

    public void stop(UUID uuid) {
        MovementState state = activeTasks.remove(uuid);
        if (state != null) {
            state.task.cancel();
        }

        manager.setMovementActive(uuid, false);
        store.findKeyByUuid(uuid).ifPresent(key ->
            store.get(key).ifPresent(fp -> store.update(key, fp.withWandering(false)))
        );
    }

    public void stopAll() {
        for (UUID uuid : activeTasks.keySet().toArray(new UUID[0])) {
            stop(uuid);
        }
    }

    public void updateSettings(FakePlayerSettings.MovementSettings newSettings) {
        if (!activeTasks.isEmpty()) {
            stopAll();
        }
        this.settings = newSettings;
    }

    public boolean isMoving(UUID uuid) {
        return activeTasks.containsKey(uuid);
    }

    private void onTick(String storeKey) {
        Optional<FakePlayer> fakePlayer = store.get(storeKey);
        if (fakePlayer.isEmpty()) {
            return;
        }

        UUID uuid = fakePlayer.get().uuid();
        MovementState state = activeTasks.get(uuid);
        if (state == null) {
            return;
        }

        Optional<Player> entity = manager.getPlayer(uuid);
        if (entity.isEmpty()) {
            stop(uuid);
            return;
        }

        Player player = entity.get();
        Location current = player.getLocation();
        Location target = state.target;
        if (target == null || target.getWorld() == null || current.getWorld() == null) {
            stop(uuid);
            return;
        }

        if (!current.getWorld().equals(target.getWorld())) {
            stop(uuid);
            return;
        }

        double horizontalDistance = horizontalDistance(current, target);
        if (horizontalDistance <= settings.arrivalDistance()) {
            if (state.mode == Mode.WANDER) {
                state.target = pickWanderTarget(current, state.wanderRadius);
                return;
            }

            stop(uuid);
            updateStoredLocation(storeKey, current);
            return;
        }

        Location next = stepToward(current, target, settings.speed());
        if (next == null) {
            if (state.mode == Mode.WANDER) {
                state.target = pickWanderTarget(current, state.wanderRadius);
            } else {
                stop(uuid);
            }
            return;
        }

        player.teleport(next);
        updateStoredLocation(storeKey, next);
    }

    private Location stepToward(Location current, Location target, double speed) {
        double dx = target.getX() - current.getX();
        double dz = target.getZ() - current.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 0.0001D) {
            return current;
        }

        double step = Math.min(speed, distance);
        double nx = current.getX() + (dx / distance) * step;
        double nz = current.getZ() + (dz / distance) * step;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

        Location next = MovementCollision.resolveStep(current, nx, nz, yaw);
        return next == null ? null : next;
    }

    private Location pickWanderTarget(Location origin, double radius) {
        World world = origin.getWorld();
        if (world == null) {
            return origin;
        }

        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * radius;
            double x = origin.getX() + Math.cos(angle) * distance;
            double z = origin.getZ() + Math.sin(angle) * distance;
            double y = MovementCollision.findStandableY(world, x, z, origin.getY());
            if (!Double.isNaN(y)) {
                return new Location(world, x, y, z);
            }
        }

        return origin.clone();
    }

    private double horizontalDistance(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private void updateStoredLocation(String storeKey, Location location) {
        store.get(storeKey).ifPresent(fp -> store.update(storeKey, fp.withLocation(location)));
    }

    private final class MovementState {
        private final String storeKey;
        private final UUID uuid;
        private final Mode mode;
        private final double wanderRadius;
        private Location target;
        private final BukkitRunnable task;

        private MovementState(String storeKey, UUID uuid, Mode mode, double wanderRadius) {
            this.storeKey = storeKey;
            this.uuid = uuid;
            this.mode = mode;
            this.wanderRadius = wanderRadius;
            this.task = new BukkitRunnable() {
                @Override
                public void run() {
                    BotMovementService.this.onTick(storeKey);
                }
            };
        }
    }
}
