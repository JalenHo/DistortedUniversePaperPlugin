package dev.distorteduniverse.fakeplayer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

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
        if (!manager.isManagedEntity(event.getEntity().getUniqueId())) {
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

        if (manager.isManagedEntity(event.getDamager().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        manager.getFakePlayerUuid(event.getEntity().getUniqueId()).ifPresent(fakeUuid ->
            store.findKeyByUuid(fakeUuid).ifPresent(key ->
                lifecycleService.remove(key, FakePlayerLifecycleService.RemovalReason.DEATH)
            )
        );
    }
}
