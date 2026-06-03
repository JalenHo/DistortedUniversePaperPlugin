package dev.distorteduniverse.immortal;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.PlayerInventory;

public final class ImmortalDamageListener implements Listener {
    private final ImmortalSettingsService settingsService;
    private final ImmortalPlayerStore playerStore;

    public ImmortalDamageListener(ImmortalSettingsService settingsService, ImmortalPlayerStore playerStore) {
        this.settingsService = settingsService;
        this.playerStore = playerStore;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ImmortalSettings settings = settingsService.settings();
        if (!settings.enabled() || !playerStore.isImmortal(player.getUniqueId())) {
            return;
        }

        double currentHealth = player.getHealth();
        double finalDamage = event.getFinalDamage();
        if (settings.totemCompatibilityEnabled()
            && DamageFloorCalculator.wouldKill(currentHealth, finalDamage)
            && hasTotemOfUndying(player)) {
            return;
        }

        double adjustedDamage = DamageFloorCalculator.adjustedRawDamage(
            currentHealth,
            event.getDamage(),
            finalDamage,
            settings.minimumHealth()
        );
        if (Double.compare(adjustedDamage, event.getDamage()) != 0) {
            event.setDamage(adjustedDamage);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (playerStore.updateKnownName(event.getPlayer().getUniqueId(), event.getPlayer().getName())) {
            playerStore.save();
        }
    }

    private static boolean hasTotemOfUndying(Player player) {
        PlayerInventory inventory = player.getInventory();
        return inventory.getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING
            || inventory.getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING;
    }
}
