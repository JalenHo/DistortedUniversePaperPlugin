package dev.distorteduniverse.fakeplayer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;

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

    @EventHandler(priority = EventPriority.NORMAL)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (!settingsService.settings().chat().enabled()) {
            return;
        }

        String message = event.getMessage().toLowerCase();
        String playerName = event.getPlayer().getName();

        for (Map.Entry<String, String> entry : settingsService.settings().chat().responses().entrySet()) {
            String trigger = entry.getKey().toLowerCase();
            String responseTemplate = entry.getValue();

            if (message.contains(trigger)) {
                String response = responseTemplate
                    .replace("{player}", pickRandomFakePlayer())
                    .replace("{sender}", playerName);

                Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("DistortedUniverseFakePlayer"),
                    () -> Bukkit.broadcast(net.kyori.adventure.text.Component.text(response))
                );
                return;
            }
        }
    }

    private String pickRandomFakePlayer() {
        var names = settingsService.settings().names();
        if (names.isEmpty()) {
            return "Steve";
        }
        return names.get((int) (Math.random() * names.size()));
    }
}
