package dev.distorteduniverse.fakeplayer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

public class FakePlayerListener implements Listener {
    private final FakePlayerManager manager;
    private final FakePlayerStore store;
    private final FakePlayerSettingsService settingsService;
    private final FakePlayerLifecycleService lifecycleService;

    public FakePlayerListener(
        FakePlayerManager manager,
        FakePlayerStore store,
        FakePlayerSettingsService settingsService,
        FakePlayerLifecycleService lifecycleService
    ) {
        this.manager = manager;
        this.store = store;
        this.settingsService = settingsService;
        this.lifecycleService = lifecycleService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !FakePlayerMarkers.isFakePlayer(player)) {
            return;
        }

        FakePlayerSettings.BehaviorSettings behavior = settingsService.settings().behavior();
        if (!behavior.invulnerable()) {
            return;
        }

        if (behavior.knockbackWhenInvulnerable()) {
            event.setDamage(0.0D);
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!settingsService.settings().behavior().invulnerable()) {
            return;
        }

        if (event.getDamager() instanceof Player damager && FakePlayerMarkers.isFakePlayer(damager)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (!FakePlayerMarkers.isFakePlayer(player)) {
            return;
        }

        event.deathMessage(null);
        store.findKeyByUuid(player.getUniqueId()).ifPresent(key ->
            lifecycleService.remove(key, FakePlayerLifecycleService.RemovalReason.DEATH)
        );
    }
}
