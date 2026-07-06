package dev.distorteduniverse.team;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

public class AutoKitIntegration {
    private final JavaPlugin plugin;
    private final boolean available;
    private Object autoKitPlugin;
    private Method giveKitMethod;

    public AutoKitIntegration(JavaPlugin plugin) {
        this.plugin = plugin;
        this.available = detectAutoKit();
    }

    private boolean detectAutoKit() {
        try {
            org.bukkit.plugin.Plugin autokit = Bukkit.getPluginManager().getPlugin("AutoKit");
            if (autokit == null) {
                plugin.getLogger().info("AutoKit not found - kit integration disabled");
                return false;
            }

            this.autoKitPlugin = autokit;

            Class<?> kitManagerClass = Class.forName("com.autokit.api.KitManager");
            Method getInstance = kitManagerClass.getMethod("getInstance");
            Object kitManager = getInstance.invoke(null);

            this.giveKitMethod = kitManagerClass.getMethod("giveKit", Player.class, String.class);

            plugin.getLogger().info("AutoKit integration enabled");
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("AutoKit detection failed: " + e.getMessage());
            return false;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public void giveKit(Player player, String kitId) {
        if (!available) {
            return;
        }

        try {
            Class<?> kitManagerClass = Class.forName("com.autokit.api.KitManager");
            Method getInstance = kitManagerClass.getMethod("getInstance");
            Object kitManager = getInstance.invoke(null);

            giveKitMethod.invoke(kitManager, player, kitId);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to give kit " + kitId + " to " + player.getName() + ": " + e.getMessage());
        }
    }
}
