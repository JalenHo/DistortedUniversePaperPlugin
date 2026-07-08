package dev.distorteduniverse.fakeplayer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Mannequin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Random;
import java.util.UUID;

public class BotMovementService {
    private static final double FLOAT_SURFACE_OFFSET = 0.10;

    private enum Mode {
        WANDER,
        MOVE_TO
    }

    private final JavaPlugin plugin;
    private final FakePlayerManager manager;
    private final FakePlayerStore store;
    private FakePlayerSettings.MovementSettings settings;
    private final Map<UUID, MovementState> activeTasks = new HashMap<>();
    private final Map<UUID, Integer> movementPauseTicks = new HashMap<>();
    private final Random random = new Random();
    private BukkitTask ambientTask;

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

    public void start() {
        if (ambientTask != null) {
            return;
        }

        ambientTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickAmbientMovement();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public void shutdown() {
        if (ambientTask != null) {
            ambientTask.cancel();
            ambientTask = null;
        }
        stopAll(false);
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

        movementPauseTicks.remove(uuid);
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

    public void pauseForMovement(UUID uuid, int ticks) {
        movementPauseTicks.put(uuid, Math.max(movementPauseTicks.getOrDefault(uuid, 0), ticks));
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
        if (consumeMovementPause(uuid)) {
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
        next.setY(movementY(world, nx, nz, current.getY()));
        return next;
    }

    private Location pickWanderTarget(Location origin, double radius) {
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = random.nextDouble() * radius;
        double x = origin.getX() + Math.cos(angle) * distance;
        double z = origin.getZ() + Math.sin(angle) * distance;
        World world = origin.getWorld();
        double y = movementY(world, x, z, origin.getY());
        return new Location(world, x, y, z);
    }

    private void tickAmbientMovement() {
        for (UUID uuid : manager.getSpawnedUuids()) {
            manager.getMannequin(uuid).ifPresent(mannequin -> {
                applyWaterMovement(mannequin);
                updateStoredLocation(uuid, mannequin.getLocation());
            });
        }
    }

    private void applyWaterMovement(Mannequin mannequin) {
        Location location = mannequin.getLocation();
        OptionalDouble floatingY = floatingY(location);
        if (floatingY.isEmpty()) {
            return;
        }

        double targetY = floatingY.getAsDouble();
        double deltaY = targetY - location.getY();
        Vector velocity = mannequin.getVelocity();

        if (deltaY > 0.05) {
            double upward = Math.min(0.18, Math.max(0.06, deltaY * 0.10));
            velocity.setY(Math.max(velocity.getY(), upward));
            mannequin.setVelocity(velocity);
            return;
        }

        if (deltaY > -0.20 && velocity.getY() < -0.02) {
            velocity.setY(-0.02);
            mannequin.setVelocity(velocity);
        }
    }

    private double movementY(World world, double x, double z, double currentY) {
        OptionalDouble waterY = floatingY(world, x, z, currentY);
        if (waterY.isPresent()) {
            return waterY.getAsDouble();
        }

        int groundY = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z));
        double snapped = groundY + 1.0;
        if (snapped < world.getMinHeight()) {
            return currentY;
        }
        return snapped;
    }

    private OptionalDouble floatingY(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return OptionalDouble.empty();
        }
        return floatingY(world, location.getX(), location.getZ(), location.getY());
    }

    private OptionalDouble floatingY(World world, double x, double z, double referenceY) {
        OptionalDouble surfaceY = waterSurfaceY(world, x, z, referenceY);
        if (surfaceY.isEmpty()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(surfaceY.getAsDouble() - FLOAT_SURFACE_OFFSET);
    }

    private OptionalDouble waterSurfaceY(World world, double x, double z, double referenceY) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int centerY = (int) Math.floor(referenceY);
        int minY = Math.max(world.getMinHeight(), centerY - 6);
        int maxY = Math.min(world.getMaxHeight() - 1, centerY + 3);

        for (int y = maxY; y >= minY; y--) {
            if (!isWater(world.getBlockAt(blockX, y, blockZ))) {
                continue;
            }

            int surfaceBlockY = y;
            while (surfaceBlockY + 1 < world.getMaxHeight()
                && isWater(world.getBlockAt(blockX, surfaceBlockY + 1, blockZ))) {
                surfaceBlockY++;
            }
            if (!hasClearColumnToReference(world, blockX, blockZ, surfaceBlockY, centerY)) {
                return OptionalDouble.empty();
            }
            return OptionalDouble.of(surfaceBlockY + 1.0);
        }

        return OptionalDouble.empty();
    }

    private boolean hasClearColumnToReference(World world, int x, int z, int surfaceBlockY, int referenceY) {
        int fromY = Math.max(world.getMinHeight(), surfaceBlockY + 1);
        int toY = Math.min(world.getMaxHeight() - 1, referenceY);
        for (int y = fromY; y <= toY; y++) {
            Block block = world.getBlockAt(x, y, z);
            if (!isWater(block) && !block.isPassable()) {
                return false;
            }
        }
        return true;
    }

    private boolean isWater(Block block) {
        if (block.getType() == Material.WATER) {
            return true;
        }
        return block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged();
    }

    private boolean consumeMovementPause(UUID uuid) {
        Integer ticks = movementPauseTicks.get(uuid);
        if (ticks == null || ticks <= 0) {
            movementPauseTicks.remove(uuid);
            return false;
        }

        if (ticks == 1) {
            movementPauseTicks.remove(uuid);
        } else {
            movementPauseTicks.put(uuid, ticks - 1);
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

    private void updateStoredLocation(UUID uuid, Location location) {
        store.findKeyByUuid(uuid).ifPresent(key ->
            store.get(key).ifPresent(fakePlayer -> store.update(key, fakePlayer.withLocation(location)))
        );
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
