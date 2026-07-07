package dev.distorteduniverse.fakeplayer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class FakePlayerListener implements Listener {
    private final FakePlayerManager manager;
    private final FakePlayerSettingsService settingsService;

    public FakePlayerListener(FakePlayerManager manager, FakePlayerSettingsService settingsService) {
        this.manager = manager;
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

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!settingsService.settings().behavior().invulnerable()) {
            return;
        }

        if (manager.isManagedEntity(event.getDamager().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
