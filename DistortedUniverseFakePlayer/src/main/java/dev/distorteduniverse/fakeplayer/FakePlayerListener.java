package dev.distorteduniverse.fakeplayer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.UUID;

public class FakePlayerListener implements Listener {
    private final JavaPlugin plugin;
    private final FakePlayerManager manager;
    private final FakePlayerStore store;
    private final FakePlayerSettingsService settingsService;
    private final FakePlayerLifecycleService lifecycleService;
    private final BotMovementService movementService;

    public FakePlayerListener(
        JavaPlugin plugin,
        FakePlayerManager manager,
        FakePlayerStore store,
        FakePlayerSettingsService settingsService,
        FakePlayerLifecycleService lifecycleService,
        BotMovementService movementService
    ) {
        this.plugin = plugin;
        this.manager = manager;
        this.store = store;
        this.settingsService = settingsService;
        this.lifecycleService = lifecycleService;
        this.movementService = movementService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !FakePlayerMarkers.isFakePlayer(player)) {
            return;
        }

        if (!manager.isInvulnerable(player.getUniqueId())) {
            return;
        }

        FakePlayerSettings.BehaviorSettings behavior = settingsService.settings().behavior();
        if (behavior.knockbackWhenInvulnerable()) {
            // Keep the hit event so knockback can apply, but deal no damage.
            event.setDamage(0.0D);
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player damager && FakePlayerMarkers.isFakePlayer(damager)) {
            event.setCancelled(true);
            return;
        }

        if (!(event.getEntity() instanceof Player player) || !FakePlayerMarkers.isFakePlayer(player)) {
            return;
        }

        boolean invulnerable = manager.isInvulnerable(player.getUniqueId());
        FakePlayerSettings.BehaviorSettings behavior = settingsService.settings().behavior();
        if (invulnerable && !behavior.knockbackWhenInvulnerable()) {
            return;
        }

        applyManualKnockback(player, resolveAttacker(event.getDamager()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (!FakePlayerMarkers.isFakePlayer(player)) {
            return;
        }

        event.deathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        UUID uuid = player.getUniqueId();
        store.findKeyByUuid(uuid).ifPresentOrElse(
            key -> lifecycleService.remove(key, FakePlayerLifecycleService.RemovalReason.DEATH),
            () -> {
                // Orphaned marked entity: still force-remove the corpse.
                manager.forceDespawnFakePlayer(uuid);
            }
        );

        // Backup cleanup next tick in case death processing re-creates a corpse state.
        Bukkit.getScheduler().runTask(plugin, () -> {
            manager.getTrackedPlayer(uuid).ifPresent(tracked -> manager.forceDespawnFakePlayer(uuid));
            if (player.isValid()) {
                FakePlayerMarkers.unmark(player);
                player.remove();
            }
        });
    }

    private void applyManualKnockback(Player victim, Entity attacker) {
        if (attacker == null) {
            return;
        }

        Vector direction = victim.getLocation().toVector().subtract(attacker.getLocation().toVector());
        direction.setY(0.0D);
        if (direction.lengthSquared() < 1.0E-6D) {
            direction = attacker.getLocation().getDirection().clone().setY(0.0D);
            if (direction.lengthSquared() < 1.0E-6D) {
                return;
            }
        }

        Vector knockback = direction.normalize().multiply(0.42D);
        knockback.setY(0.36D);

        // Pause teleport-based movement briefly so knockback velocity is not overwritten.
        movementService.pauseMovement(victim.getUniqueId(), 8);

        Vector finalKnockback = knockback;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!victim.isValid() || victim.isDead()) {
                return;
            }
            Vector current = victim.getVelocity();
            victim.setVelocity(current.multiply(0.2D).add(finalKnockback));
        });
    }

    private Entity resolveAttacker(Entity damager) {
        if (damager instanceof org.bukkit.entity.Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Entity shooterEntity) {
                return shooterEntity;
            }
        }
        return damager;
    }
}
