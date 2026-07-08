package dev.distorteduniverse.fakeplayer;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Mannequin;
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
    private final Map<UUID, Integer> physicsPauseTicks = new HashMap<>();
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
        state.start(settings.tickInterval());
    }

    public void startMoveTo(String key, FakePlayer fakePlayer, Location destination) {
        stop(fakePlayer.uuid());

        FakePlayer updated = fakePlayer.withWandering(false);
        store.update(key, updated);

        MovementState state = new MovementState(key, updated.uuid(), Mode.MOVE_TO, 0);
        state.target = destination.clone();
        activeTasks.put(updated.uuid(), state);
        manager.setMovementActive(updated.uuid(), true);
        state.start(settings.tickInterval());
    }

    public void stop(UUID uuid) {
        stop(uuid, true);
    }

    public void stop(UUID uuid, boolean clearWandering) {
        MovementState state = activeTasks.remove(uuid);
        if (state != null) {
            state.cancel();
        }

        physicsPauseTicks.remove(uuid);
        manager.setMovementActive(uuid, false);
        if (clearWandering) {
            store.findKeyByUuid(uuid).ifPresent(key ->
                store.get(key).ifPresent(fp -> store.update(key, fp.withWandering(false)))
            );
        }
    }

    public void stopAll() {
        stopAll(true);
    }

    public void stopAll(boolean clearWandering) {
        for (UUID uuid : activeTasks.keySet().toArray(new UUID[0])) {
            stop(uuid, clearWandering);
        }
    }

    public void updateSettings(FakePlayerSettings.MovementSettings newSettings) {
        int oldTickInterval = settings.tickInterval();
        this.settings = newSettings;
        if (oldTickInterval != newSettings.tickInterval()) {
            for (MovementState state : activeTasks.values()) {
                state.restart(newSettings.tickInterval());
            }
        }
    }

    public boolean isMoving(UUID uuid) {
        return activeTasks.containsKey(uuid);
    }

    public void pauseForPhysics(UUID uuid, int ticks) {
        physicsPauseTicks.put(uuid, Math.max(physicsPauseTicks.getOrDefault(uuid, 0), ticks));
    }

    public void updateStoreKey(UUID uuid, String newKey) {
        MovementState state = activeTasks.get(uuid);
        if (state != null) {
            state.storeKey = newKey;
        }
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

        Optional<Mannequin> entity = manager.getMannequin(uuid);
        if (entity.isEmpty()) {
            stop(uuid);
            return;
        }

        Mannequin mannequin = entity.get();
        Location current = mannequin.getLocation();
        if (consumePhysicsPause(uuid)) {
            updateStoredLocation(state.storeKey, current);
            return;
        }

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
        mannequin.teleport(next);
        updateStoredLocation(state.storeKey, next);
    }

    private Location stepToward(Location current, Location target, double speed) {
        World world = current.getWorld();
        double dx = target.getX() - current.getX();
        double dz = target.getZ() - current.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        double step = Math.min(speed, distance);

        double nx = current.getX() + (dx / distance) * step;
        double nz = current.getZ() + (dz / distance) * step;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

        Location next = new Location(world, nx, current.getY(), nz, yaw, current.getPitch());
        next.setY(WaterPhysics.walkingY(world, nx, nz, current.getY()));
        return next;
    }

    private Location pickWanderTarget(Location origin, double radius) {
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = random.nextDouble() * radius;
        double x = origin.getX() + Math.cos(angle) * distance;
        double z = origin.getZ() + Math.sin(angle) * distance;
        World world = origin.getWorld();
        double y = WaterPhysics.walkingY(world, x, z, origin.getY());
        return new Location(world, x, y, z);
    }

    private boolean consumePhysicsPause(UUID uuid) {
        Integer ticks = physicsPauseTicks.get(uuid);
        if (ticks == null || ticks <= 0) {
            physicsPauseTicks.remove(uuid);
            return false;
        }

        if (ticks == 1) {
            physicsPauseTicks.remove(uuid);
        } else {
            physicsPauseTicks.put(uuid, ticks - 1);
        }
        return true;
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
        private String storeKey;
        private final UUID uuid;
        private final Mode mode;
        private final double wanderRadius;
        private Location target;
        private BukkitRunnable task;

        private MovementState(String storeKey, UUID uuid, Mode mode, double wanderRadius) {
            this.storeKey = storeKey;
            this.uuid = uuid;
            this.mode = mode;
            this.wanderRadius = wanderRadius;
        }

        private void start(int tickInterval) {
            this.task = new BukkitRunnable() {
                @Override
                public void run() {
                    BotMovementService.this.onTick(MovementState.this.storeKey);
                }
            };
            this.task.runTaskTimer(plugin, 0L, tickInterval);
        }

        private void cancel() {
            if (task != null) {
                task.cancel();
                task = null;
            }
        }

        private void restart(int tickInterval) {
            cancel();
            start(tickInterval);
        }
    }
}
