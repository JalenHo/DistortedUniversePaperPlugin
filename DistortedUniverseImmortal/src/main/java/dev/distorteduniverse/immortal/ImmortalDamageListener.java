package dev.distorteduniverse.immortal;

import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

public final class ImmortalDamageListener implements Listener {
    private final Plugin plugin;
    private final ImmortalSettingsService settingsService;
    private final ImmortalPlayerStore playerStore;

    public ImmortalDamageListener(
        Plugin plugin,
        ImmortalSettingsService settingsService,
        ImmortalPlayerStore playerStore
    ) {
        this.plugin = plugin;
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
        double originalRawDamage = event.getDamage();
        double finalDamage = event.getFinalDamage();
        if (settings.totemCompatibilityEnabled()
            && DamageFloorCalculator.wouldKill(currentHealth, finalDamage)
            && hasTotemOfUndying(player)) {
            return;
        }

        double adjustedDamage = DamageFloorCalculator.adjustedRawDamage(
            currentHealth,
            originalRawDamage,
            finalDamage,
            settings.minimumHealth()
        );
        if (Double.compare(adjustedDamage, originalRawDamage) != 0) {
            event.setDamage(adjustedDamage);
            if (adjustedDamage <= 0.0D) {
                applyNormalDamageCooldown(player, originalRawDamage);
            }
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

    private void applyNormalDamageCooldown(Player player, double originalDamage) {
        setDamageCooldown(player, originalDamage);
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player onlinePlayer = Bukkit.getPlayer(player.getUniqueId());
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                setDamageCooldown(onlinePlayer, originalDamage);
            }
        });
    }

    private static void setDamageCooldown(Player player, double originalDamage) {
        int maximumNoDamageTicks = player.getMaximumNoDamageTicks();
        if (maximumNoDamageTicks <= 0) {
            return;
        }
        player.setNoDamageTicks(Math.max(player.getNoDamageTicks(), maximumNoDamageTicks));
        player.setLastDamage(Math.max(player.getLastDamage(), originalDamage));
    }
}
