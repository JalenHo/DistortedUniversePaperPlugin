package dev.distorteduniverse.fakeplayer;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

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
    private final Map<UUID, Long> pausedUntilTick = new HashMap<>();
    private final Random random = new Random();
    private BukkitRunnable buoyancyTask;

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
        startBuoyancyTask();
    }

    private void startBuoyancyTask() {
        if (buoyancyTask != null) {
            buoyancyTask.cancel();
        }
        buoyancyTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : manager.getSpawnedUuids()) {
                    // Active walkers already apply buoyancy in onTick.
                    if (activeTasks.containsKey(uuid) && !isPaused(uuid)) {
                        continue;
                    }
                    manager.getPlayer(uuid).ifPresent(BotMovementService.this::applyWaterBuoyancy);
                }
            }
        };
        buoyancyTask.runTaskTimer(plugin, 1L, 2L);
    }

    public void startWandering(String key, FakePlayer fakePlayer, double radius) {
        stop(fakePlayer.uuid(), false);

        FakePlayer updated = fakePlayer.withWandering(true);
        store.update(key, updated);

        MovementState state = new MovementState(key, updated.uuid(), Mode.WANDER, radius);
        state.target = pickWanderTarget(updated.location(), radius);
        activeTasks.put(updated.uuid(), state);
        manager.setMovementActive(updated.uuid(), true);
        state.task.runTaskTimer(plugin, 0L, settings.tickInterval());
    }

    public void startMoveTo(String key, FakePlayer fakePlayer, Location destination) {
        stop(fakePlayer.uuid(), false);

        FakePlayer updated = fakePlayer.withWandering(false);
        store.update(key, updated);

        MovementState state = new MovementState(key, updated.uuid(), Mode.MOVE_TO, 0);
        state.target = destination.clone();
        activeTasks.put(updated.uuid(), state);
        manager.setMovementActive(updated.uuid(), true);
        state.task.runTaskTimer(plugin, 0L, settings.tickInterval());
    }

    public void stop(UUID uuid) {
        stop(uuid, true);
    }

    public void stop(UUID uuid, boolean clearWanderingFlag) {
        MovementState state = activeTasks.remove(uuid);
        if (state != null) {
            state.task.cancel();
        }

        pausedUntilTick.remove(uuid);
        manager.setMovementActive(uuid, false);
        if (clearWanderingFlag) {
            store.findKeyByUuid(uuid).ifPresent(key ->
                store.get(key).ifPresent(fp -> store.update(key, fp.withWandering(false)))
            );
        }
    }

    public void stopAll() {
        stopAll(true);
    }

    public void stopAll(boolean clearWanderingFlag) {
        for (UUID uuid : activeTasks.keySet().toArray(new UUID[0])) {
            stop(uuid, clearWanderingFlag);
        }
    }

    public void shutdown() {
        stopAll(false);
        if (buoyancyTask != null) {
            buoyancyTask.cancel();
            buoyancyTask = null;
        }
    }

    /**
     * Temporarily pause teleport stepping so knockback / buoyancy velocity can apply.
     */
    public void pauseMovement(UUID uuid, int ticks) {
        long until = plugin.getServer().getCurrentTick() + Math.max(1, ticks);
        pausedUntilTick.merge(uuid, until, Math::max);
    }

    public void updateSettings(FakePlayerSettings.MovementSettings newSettings) {
        this.settings = newSettings;
    }

    /**
     * Soft-reload movement: keep wandering flags, restart active tasks with new interval/speed.
     */
    public void reloadAndResume() {
        Map<UUID, ResumeInfo> toResume = new HashMap<>();
        for (Map.Entry<UUID, MovementState> entry : activeTasks.entrySet()) {
            MovementState state = entry.getValue();
            toResume.put(entry.getKey(), new ResumeInfo(state.storeKey, state.mode, state.wanderRadius, state.target));
        }

        stopAll(false);

        for (Map.Entry<UUID, ResumeInfo> entry : toResume.entrySet()) {
            ResumeInfo info = entry.getValue();
            Optional<FakePlayer> fp = store.get(info.storeKey);
            if (fp.isEmpty() || !manager.isSpawned(fp.get().uuid())) {
                continue;
            }
            if (info.mode == Mode.WANDER) {
                startWandering(info.storeKey, fp.get(), info.wanderRadius > 0 ? info.wanderRadius : settings.wanderRadius());
            } else if (info.target != null) {
                startMoveTo(info.storeKey, fp.get(), info.target);
            }
        }

        // Also resume any stored wandering bots that were not actively ticking.
        for (String key : store.getKeys()) {
            store.get(key).ifPresent(fp -> {
                if (fp.isWandering() && manager.isSpawned(fp.uuid()) && !isMoving(fp.uuid())) {
                    startWandering(key, fp, settings.wanderRadius());
                }
            });
        }
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
            stop(uuid, false);
            return;
        }

        Player player = entity.get();
        if (isPaused(uuid)) {
            applyWaterBuoyancy(player);
            return;
        }

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

        // Always keep bots floating near the water surface like villagers/pigs.
        applyWaterBuoyancy(player);
        current = player.getLocation();

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
        if (MovementCollision.isInFluid(next)) {
            player.setSwimming(false);
            Vector velocity = player.getVelocity();
            if (velocity.getY() < 0.02D) {
                player.setVelocity(new Vector(velocity.getX() * 0.6D, 0.04D, velocity.getZ() * 0.6D));
            }
        }
        updateStoredLocation(storeKey, next);
    }

    private void applyWaterBuoyancy(Player player) {
        Location location = player.getLocation();
        if (!MovementCollision.isInFluid(location)) {
            return;
        }

        World world = location.getWorld();
        if (world == null) {
            return;
        }

        Double surfaceY = MovementCollision.findWaterSurfaceY(world, location.getX(), location.getZ(), location.getY());
        if (surfaceY == null) {
            return;
        }

        double delta = surfaceY - location.getY();
        Vector velocity = player.getVelocity();

        if (Math.abs(delta) > 0.15D) {
            // Softly float toward the surface instead of sinking to the seafloor.
            double lift = Math.max(-0.08D, Math.min(0.12D, delta * 0.35D));
            player.setVelocity(new Vector(velocity.getX() * 0.7D, Math.max(lift, 0.03D), velocity.getZ() * 0.7D));
        } else if (velocity.getY() < -0.01D) {
            player.setVelocity(new Vector(velocity.getX() * 0.7D, 0.02D, velocity.getZ() * 0.7D));
        }

        // Keep the entity near the surface with a gentle teleport correction when deeply submerged.
        if (delta > 0.6D) {
            Location floated = location.clone();
            floated.setY(Math.min(location.getY() + 0.35D, surfaceY));
            player.teleport(floated);
        }
    }

    private boolean isPaused(UUID uuid) {
        Long until = pausedUntilTick.get(uuid);
        if (until == null) {
            return false;
        }
        if (plugin.getServer().getCurrentTick() >= until) {
            pausedUntilTick.remove(uuid);
            return false;
        }
        return true;
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

        boolean preferWater = MovementCollision.isInFluid(origin);
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * radius;
            double x = origin.getX() + Math.cos(angle) * distance;
            double z = origin.getZ() + Math.sin(angle) * distance;

            if (preferWater) {
                Double surfaceY = MovementCollision.findWaterSurfaceY(world, x, z, origin.getY());
                if (surfaceY != null) {
                    return new Location(world, x, surfaceY, z);
                }
            }

            double y = MovementCollision.findStandableY(world, x, z, origin.getY());
            if (!Double.isNaN(y)) {
                return new Location(world, x, y, z);
            }

            Double surfaceY = MovementCollision.findWaterSurfaceY(world, x, z, origin.getY());
            if (surfaceY != null) {
                return new Location(world, x, surfaceY, z);
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

    private record ResumeInfo(String storeKey, Mode mode, double wanderRadius, Location target) {
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
