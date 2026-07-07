package dev.distorteduniverse.team;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AutoKitIntegration {
    private final JavaPlugin plugin;
    private final boolean available;
    private Method giveKitMethod;
    private Method listKitsMethod;
    private Method createKitMethod;
    private Method saveKitFromPlayerMethod;

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

            Class<?> kitManagerClass = Class.forName("com.autokit.api.KitManager");
            Method getInstance = kitManagerClass.getMethod("getInstance");
            Object kitManager = getInstance.invoke(null);

            giveKitMethod = findMethod(kitManagerClass, "giveKit", Player.class, String.class).orElse(null);

            listKitsMethod = findMethod(kitManagerClass, "getKitNames")
                .or(() -> findMethod(kitManagerClass, "getKits"))
                .or(() -> findMethod(kitManagerClass, "listKits"))
                .orElse(null);

            createKitMethod = findMethod(kitManagerClass, "createKit", String.class)
                .or(() -> findMethod(kitManagerClass, "registerKit", String.class))
                .orElse(null);

            saveKitFromPlayerMethod = findMethod(kitManagerClass, "saveKitFromPlayer", Player.class, String.class)
                .or(() -> findMethod(kitManagerClass, "setKitFromPlayer", Player.class, String.class))
                .or(() -> findMethod(kitManagerClass, "updateKitFromPlayer", Player.class, String.class))
                .orElse(null);

            plugin.getLogger().info("AutoKit integration enabled");
            return giveKitMethod != null;
        } catch (Exception e) {
            plugin.getLogger().warning("AutoKit detection failed: " + e.getMessage());
            return false;
        }
    }

    private static java.util.Optional<Method> findMethod(Class<?> clazz, String name, Class<?>... params) {
        try {
            return java.util.Optional.of(clazz.getMethod(name, params));
        } catch (NoSuchMethodException e) {
            return java.util.Optional.empty();
        }
    }

    private Object kitManager() throws Exception {
        Class<?> kitManagerClass = Class.forName("com.autokit.api.KitManager");
        Method getInstance = kitManagerClass.getMethod("getInstance");
        return getInstance.invoke(null);
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean supportsKitManagement() {
        return available && listKitsMethod != null;
    }

    public List<String> listKits() {
        if (!available || listKitsMethod == null) {
            return Collections.emptyList();
        }

        try {
            Object result = listKitsMethod.invoke(kitManager());
            if (result instanceof Collection<?> collection) {
                List<String> kits = new ArrayList<>();
                for (Object item : collection) {
                    kits.add(String.valueOf(item));
                }
                kits.sort(String.CASE_INSENSITIVE_ORDER);
                return kits;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to list AutoKit kits: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    public boolean createKit(String kitId) {
        if (!available || createKitMethod == null) {
            return false;
        }

        try {
            createKitMethod.invoke(kitManager(), kitId);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create AutoKit kit " + kitId + ": " + e.getMessage());
            return false;
        }
    }

    public boolean saveKitFromPlayer(Player player, String kitId) {
        if (!available || saveKitFromPlayerMethod == null) {
            return false;
        }

        try {
            saveKitFromPlayerMethod.invoke(kitManager(), player, kitId);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save AutoKit kit " + kitId + ": " + e.getMessage());
            return false;
        }
    }

    public void giveKit(Player player, String kitId) {
        if (!available || giveKitMethod == null) {
            return;
        }

        try {
            giveKitMethod.invoke(kitManager(), player, kitId);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to give kit " + kitId + " to " + player.getName() + ": " + e.getMessage());
        }
    }
}
