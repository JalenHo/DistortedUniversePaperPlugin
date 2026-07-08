package dev.distorteduniverse.fakeplayer;

import org.bukkit.Location;
import org.bukkit.entity.Mannequin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.OptionalDouble;
import java.util.UUID;

public class FakePlayerLifecycleService {
    private final JavaPlugin plugin;
    private final FakePlayerManager manager;
    private final FakePlayerStore store;
    private BukkitTask task;

    public FakePlayerLifecycleService(JavaPlugin plugin, FakePlayerManager manager, FakePlayerStore store) {
        this.plugin = plugin;
        this.manager = manager;
        this.store = store;
    }

    public void start() {
        if (task != null) {
            return;
        }

        task = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public void stop() {
        if (task == null) {
            return;
        }

        task.cancel();
        task = null;
    }

    private void tick() {
        for (UUID uuid : manager.getSpawnedUuids()) {
            manager.getMannequin(uuid).ifPresent(mannequin -> {
                applyWaterFloat(mannequin);
                updateStoredLocation(uuid, mannequin.getLocation());
            });
        }
    }

    private void applyWaterFloat(Mannequin mannequin) {
        Location location = mannequin.getLocation();
        OptionalDouble floatingY = WaterPhysics.floatingY(location);
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

    private void updateStoredLocation(UUID uuid, Location location) {
        store.findKeyByUuid(uuid).ifPresent(key ->
            store.get(key).ifPresent(fakePlayer -> store.update(key, fakePlayer.withLocation(location)))
        );
    }
}
