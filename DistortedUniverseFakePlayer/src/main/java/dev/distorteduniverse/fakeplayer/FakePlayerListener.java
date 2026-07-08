package dev.distorteduniverse.fakeplayer;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;

public class FakePlayerListener implements Listener {
    private static final double KNOCKBACK_STRENGTH = 0.45;
    private static final int KNOCKBACK_PAUSE_TICKS = 10;
    private static final long DEATH_CLEANUP_DELAY_TICKS = 40L;

    private final JavaPlugin plugin;
    private final FakePlayerManager manager;
    private final BotMovementService movementService;
    private final FakePlayerSettingsService settingsService;

    public FakePlayerListener(
        JavaPlugin plugin,
        FakePlayerManager manager,
        BotMovementService movementService,
        FakePlayerSettingsService settingsService
    ) {
        this.plugin = plugin;
        this.manager = manager;
        this.movementService = movementService;
        this.settingsService = settingsService;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!settingsService.settings().behavior().invulnerable()) {
            return;
        }

        if (manager.isManagedEntity(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Optional<UUID> targetFakePlayer = manager.getFakePlayerUuid(event.getEntity().getUniqueId());
        if (targetFakePlayer.isPresent()) {
            UUID fakePlayerUuid = targetFakePlayer.get();
            applyKnockback(fakePlayerUuid, event.getDamager());

            if (settingsService.settings().behavior().invulnerable()) {
                event.setCancelled(true);
            }
            return;
        }

        if (settingsService.settings().behavior().invulnerable()
            && manager.isManagedEntity(event.getDamager().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        Optional<UUID> fakePlayerUuid = manager.getFakePlayerUuid(event.getEntity().getUniqueId());
        if (fakePlayerUuid.isEmpty()) {
            return;
        }

        UUID uuid = fakePlayerUuid.get();
        Entity deadEntity = event.getEntity();
        movementService.stop(uuid);
        manager.unregisterFakePlayer(uuid);
        event.getDrops().clear();
        event.setDroppedExp(0);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (deadEntity.isValid()) {
                deadEntity.remove();
            }
        }, DEATH_CLEANUP_DELAY_TICKS);
    }

    private void applyKnockback(UUID fakePlayerUuid, Entity damager) {
        movementService.pauseForPhysics(fakePlayerUuid, KNOCKBACK_PAUSE_TICKS);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            manager.applyKnockback(fakePlayerUuid, damager.getLocation(), KNOCKBACK_STRENGTH);
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                manager.setMovementActive(fakePlayerUuid, movementService.isMoving(fakePlayerUuid)),
                KNOCKBACK_PAUSE_TICKS
            );
        });
    }
}
