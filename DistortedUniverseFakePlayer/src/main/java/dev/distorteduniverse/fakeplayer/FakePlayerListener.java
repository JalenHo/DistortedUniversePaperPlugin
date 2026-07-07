package dev.distorteduniverse.fakeplayer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class FakePlayerListener implements Listener {
    private final FakePlayerManager manager;
    private final FakePlayerStore store;
    private final FakePlayerSettings settings;
    private final Set<UUID> protectedEntities;

    public FakePlayerListener(FakePlayerManager manager, FakePlayerStore store, FakePlayerSettings settings, Set<UUID> protectedEntities) {
        this.manager = manager;
        this.store = store;
        this.settings = settings;
        this.protectedEntities = protectedEntities;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        manager.onPlayerJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().getUniqueId() != null &&
            protectedEntities.contains(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager().getUniqueId() != null &&
            protectedEntities.contains(event.getDamager().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (!settings.chat().enabled()) {
            return;
        }

        String message = event.getMessage().toLowerCase();
        String playerName = event.getPlayer().getName();

        for (Map.Entry<String, String> entry : settings.chat().responses().entrySet()) {
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
        var names = settings.names();
        if (names.isEmpty()) {
            return "Steve";
        }
        return names.get((int) (Math.random() * names.size()));
    }

    public void registerProtectedEntity(UUID uuid) {
        protectedEntities.add(uuid);
    }

    public void unregisterProtectedEntity(UUID uuid) {
        protectedEntities.remove(uuid);
    }
}
