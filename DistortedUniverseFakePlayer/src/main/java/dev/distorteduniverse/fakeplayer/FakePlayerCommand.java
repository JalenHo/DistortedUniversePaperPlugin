package dev.distorteduniverse.fakeplayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class FakePlayerCommand implements CommandExecutor, TabExecutor {
    private static final List<String> CONFIG_PATHS = List.of(
        "enabled",
        "movement.speed",
        "movement.arrival-distance",
        "movement.tick-interval",
        "movement.wander-radius",
        "behavior.invulnerable",
        "behavior.immortal",
        "behavior.gravity",
        "behavior.immovable",
        "skins.folder",
        "skins.default"
    );
    private static final List<String> PLAYER_PROPERTIES = List.of(
        "name",
        "skin",
        "immortal",
        "invulnerable",
        "gravity",
        "immovable"
    );

    private final DistortedUniverseFakePlayerPlugin plugin;
    private final FakePlayerSettingsService settingsService;
    private final FakePlayerStore store;
    private final FakePlayerManager manager;
    private final BotMovementService movementService;

    public FakePlayerCommand(DistortedUniverseFakePlayerPlugin plugin) {
        this.plugin = plugin;
        this.settingsService = plugin.getSettingsService();
        this.store = plugin.getStore();
        this.manager = plugin.getManager();
        this.movementService = plugin.getMovementService();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "spawn" -> handleSpawn(sender, args);
            case "despawn", "remove" -> handleDespawn(sender, args);
            case "list" -> handleList(sender);
            case "move" -> handleMove(sender, args);
            case "reload" -> handleReload(sender);
            case "config", "settings" -> handleConfig(sender, args);
            case "set" -> handleSet(sender, args);
            default -> {
                sendHelp(sender);
                yield false;
            }
        };
    }

    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /dfp spawn <name>", NamedTextColor.RED));
            return true;
        }

        String name = args[1];
        String key = name.toLowerCase();
        Optional<FakePlayer> existing = store.get(key);
        if (existing.isPresent()) {
            FakePlayer fakePlayer = existing.get().withLocation(manager.snapSpawnLocation(getSpawnLocation(sender)));
            if (manager.isSpawned(fakePlayer.uuid())) {
                sender.sendMessage(Component.text("Fake player already exists and is spawned: " + name, NamedTextColor.RED));
                return true;
            }

            if (!trySpawnFakePlayer(sender, fakePlayer)) {
                return true;
            }

            store.update(key, fakePlayer);
            store.save();
            sender.sendMessage(Component.text("Respawned fake player: " + fakePlayer.name(), NamedTextColor.GREEN));
            return true;
        }

        FakePlayerSettings settings = settingsService.settings();
        Location spawnLoc = getSpawnLocation(sender);

        UUID uuid = UUID.randomUUID();
        FakePlayer fakePlayer = new FakePlayer(name, uuid, manager.snapSpawnLocation(spawnLoc), settings.skins().defaultSkin(), false);

        if (!trySpawnFakePlayer(sender, fakePlayer)) {
            return true;
        }

        store.add(key, fakePlayer);
        store.save();

        sender.sendMessage(Component.text("Spawned fake player: " + name, NamedTextColor.GREEN));
        return true;
    }

    private boolean trySpawnFakePlayer(CommandSender sender, FakePlayer fakePlayer) {
        try {
            if (!manager.spawnFakePlayer(fakePlayer)) {
                sender.sendMessage(Component.text("Failed to spawn fake player: " + fakePlayer.name(), NamedTextColor.RED));
                return false;
            }
            return true;
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to spawn fake player " + fakePlayer.name(), exception);
            sender.sendMessage(Component.text(
                "Failed to spawn fake player: " + fakePlayer.name() + " (see server log)",
                NamedTextColor.RED
            ));
            return false;
        }
    }

    private boolean handleDespawn(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /dfp despawn <name|all>", NamedTextColor.RED));
            return true;
        }

        String target = args[1].toLowerCase();

        if ("all".equals(target)) {
            int count = 0;
            for (String key : new ArrayList<>(store.getKeys())) {
                store.get(key).ifPresent(fp -> {
                    if (manager.isSpawned(fp.uuid())) {
                        manager.despawnFakePlayer(fp.uuid());
                    }
                    movementService.stop(fp.uuid());
                    store.remove(key);
                });
                count++;
            }
            store.save();
            sender.sendMessage(Component.text("Despawned " + count + " fake players", NamedTextColor.GREEN));
            return true;
        }

        Optional<FakePlayer> optPlayer = store.get(target);
        if (optPlayer.isEmpty()) {
            sender.sendMessage(Component.text("Fake player not found: " + target, NamedTextColor.RED));
            return true;
        }

        FakePlayer fp = optPlayer.get();
        if (manager.isSpawned(fp.uuid())) {
            manager.despawnFakePlayer(fp.uuid());
        }
        movementService.stop(fp.uuid());
        store.remove(target);
        store.save();

        sender.sendMessage(Component.text("Despawned fake player: " + fp.name(), NamedTextColor.GREEN));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        Collection<FakePlayer> players = store.getAll();

        if (players.isEmpty()) {
            sender.sendMessage(Component.text("No fake players configured.", NamedTextColor.YELLOW));
            return true;
        }

        sender.sendMessage(Component.text("=== Fake Players ===", NamedTextColor.GOLD));
        for (FakePlayer fp : players) {
            boolean spawned = manager.isSpawned(fp.uuid());
            String status = !spawned ? "DESPAWNED" : movementService.isMoving(fp.uuid()) ? "WALKING" : "IDLE";

            NamedTextColor color = switch (status) {
                case "WALKING" -> NamedTextColor.AQUA;
                case "IDLE" -> NamedTextColor.GREEN;
                default -> NamedTextColor.GRAY;
            };

            sender.sendMessage(Component.text(fp.name(), color)
                .append(Component.text(" - " + status, NamedTextColor.GRAY)));
        }
        return true;
    }

    private boolean handleMove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /dfp move <name> <wander|stop|x y z>", NamedTextColor.RED));
            return true;
        }

        String target = args[1].toLowerCase();
        Optional<FakePlayer> optPlayer = store.get(target);
        if (optPlayer.isEmpty()) {
            sender.sendMessage(Component.text("Fake player not found: " + target, NamedTextColor.RED));
            return true;
        }

        FakePlayer fp = optPlayer.get();
        if (!manager.isSpawned(fp.uuid())) {
            sender.sendMessage(Component.text("Fake player is not spawned: " + fp.name(), NamedTextColor.RED));
            return true;
        }

        String action = args[2].toLowerCase();
        if ("stop".equals(action)) {
            movementService.stop(fp.uuid());
            store.save();
            sender.sendMessage(Component.text("Stopped movement for " + fp.name(), NamedTextColor.GREEN));
            return true;
        }

        if ("wander".equals(action)) {
            double radius = settingsService.settings().movement().wanderRadius();
            if (args.length >= 4) {
                try {
                    radius = Double.parseDouble(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid wander radius", NamedTextColor.RED));
                    return true;
                }
            }

            movementService.startWandering(target, fp, radius);
            store.save();
            sender.sendMessage(Component.text("Started wandering for " + fp.name() + " (radius " + radius + ")", NamedTextColor.GREEN));
            return true;
        }

        if (args.length < 5) {
            sender.sendMessage(Component.text("Usage: /dfp move <name> <x> <y> <z>", NamedTextColor.RED));
            return true;
        }

        try {
            double x = Double.parseDouble(args[2]);
            double y = Double.parseDouble(args[3]);
            double z = Double.parseDouble(args[4]);
            Location destination = new Location(fp.location().getWorld(), x, y, z);
            movementService.startMoveTo(target, fp, destination);
            store.save();
            sender.sendMessage(Component.text(
                "Moving " + fp.name() + " to " + formatLocation(destination),
                NamedTextColor.GREEN
            ));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid coordinates", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        settingsService.reload();
        plugin.applyRuntimeSettings();
        sender.sendMessage(Component.text("Configuration reloaded!", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleConfig(CommandSender sender, String[] args) {
        if (args.length == 1 || "list".equalsIgnoreCase(args[1])) {
            sendConfigList(sender);
            return true;
        }

        String action = args[1].toLowerCase();
        if ("get".equals(action)) {
            if (args.length < 3) {
                sender.sendMessage(Component.text("Usage: /dfp config get <setting>", NamedTextColor.RED));
                return true;
            }
            sendSettingValue(sender, args[2]);
            return true;
        }

        if ("set".equals(action)) {
            if (args.length < 4) {
                sender.sendMessage(Component.text("Usage: /dfp config set <setting> <value>", NamedTextColor.RED));
                return true;
            }
            return updateConfigSetting(sender, args[2], args[3]);
        }

        sender.sendMessage(Component.text("Usage: /dfp config <list|get|set>", NamedTextColor.RED));
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /dfp set <name> <name|skin|immortal|gravity|immovable> <value>", NamedTextColor.RED));
            return true;
        }

        String key = args[1].toLowerCase();
        Optional<FakePlayer> optPlayer = store.get(key);
        if (optPlayer.isEmpty()) {
            sender.sendMessage(Component.text("Fake player not found: " + args[1], NamedTextColor.RED));
            return true;
        }

        FakePlayer fakePlayer = optPlayer.get();
        String property = args[2].toLowerCase();
        String value = args[3];

        switch (property) {
            case "name" -> {
                String newKey = value.toLowerCase();
                if (newKey.isBlank()) {
                    sender.sendMessage(Component.text("Name cannot be blank.", NamedTextColor.RED));
                    return true;
                }

                FakePlayer updated = fakePlayer.withName(value);
                if (!store.rename(key, newKey, updated)) {
                    sender.sendMessage(Component.text("Fake player already exists: " + value, NamedTextColor.RED));
                    return true;
                }

                movementService.updateStoreKey(updated.uuid(), newKey);
                manager.refreshAppearance(updated);
                store.save();
                sender.sendMessage(Component.text("Renamed fake player to: " + value, NamedTextColor.GREEN));
                return true;
            }
            case "skin" -> {
                FakePlayer updated = fakePlayer.withSkin(value);
                store.update(key, updated);
                manager.refreshAppearance(updated);
                store.save();
                sender.sendMessage(Component.text("Updated skin for " + updated.name() + " to: " + value, NamedTextColor.GREEN));
                return true;
            }
            case "immortal", "invulnerable" -> {
                return updateConfigSetting(sender, "behavior.invulnerable", value);
            }
            case "gravity" -> {
                return updateConfigSetting(sender, "behavior.gravity", value);
            }
            case "immovable" -> {
                return updateConfigSetting(sender, "behavior.immovable", value);
            }
            default -> {
                sender.sendMessage(Component.text("Unknown player setting: " + property, NamedTextColor.RED));
                return true;
            }
        }
    }

    private boolean updateConfigSetting(CommandSender sender, String rawPath, String value) {
        String path = normalizeConfigPath(rawPath);
        try {
            FakePlayerSettings updated = withSetting(settingsService.settings(), path, value);
            settingsService.update(updated);
            plugin.applyRuntimeSettings();
            sender.sendMessage(Component.text("Updated " + path + " to " + settingValue(updated, path), NamedTextColor.GREEN));
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
        }
        return true;
    }

    private void sendConfigList(CommandSender sender) {
        FakePlayerSettings settings = settingsService.settings();
        sender.sendMessage(Component.text("=== FakePlayer Settings ===", NamedTextColor.GOLD));
        for (String path : CONFIG_PATHS) {
            if ("behavior.immortal".equals(path)) {
                continue;
            }
            sender.sendMessage(Component.text(path + " = " + settingValue(settings, path), NamedTextColor.GRAY));
        }
        sender.sendMessage(Component.text("Alias: behavior.immortal -> behavior.invulnerable", NamedTextColor.DARK_GRAY));
    }

    private void sendSettingValue(CommandSender sender, String rawPath) {
        String path = normalizeConfigPath(rawPath);
        try {
            sender.sendMessage(Component.text(path + " = " + settingValue(settingsService.settings(), path), NamedTextColor.GREEN));
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
        }
    }

    private FakePlayerSettings withSetting(FakePlayerSettings settings, String path, String value) {
        FakePlayerSettings.MovementSettings movement = settings.movement();
        FakePlayerSettings.BehaviorSettings behavior = settings.behavior();
        FakePlayerSettings.SkinsSettings skins = settings.skins();

        return switch (path) {
            case "enabled" -> new FakePlayerSettings(parseBoolean(path, value), movement, behavior, skins);
            case "movement.speed" -> new FakePlayerSettings(
                settings.enabled(),
                new FakePlayerSettings.MovementSettings(parseDouble(path, value, 0.05, 1.0), movement.arrivalDistance(), movement.tickInterval(), movement.wanderRadius()),
                behavior,
                skins
            );
            case "movement.arrival-distance" -> new FakePlayerSettings(
                settings.enabled(),
                new FakePlayerSettings.MovementSettings(movement.speed(), parseDouble(path, value, 0.5, 5.0), movement.tickInterval(), movement.wanderRadius()),
                behavior,
                skins
            );
            case "movement.tick-interval" -> new FakePlayerSettings(
                settings.enabled(),
                new FakePlayerSettings.MovementSettings(movement.speed(), movement.arrivalDistance(), parseInt(path, value, 1, 20), movement.wanderRadius()),
                behavior,
                skins
            );
            case "movement.wander-radius" -> new FakePlayerSettings(
                settings.enabled(),
                new FakePlayerSettings.MovementSettings(movement.speed(), movement.arrivalDistance(), movement.tickInterval(), parseDouble(path, value, 1.0, 100.0)),
                behavior,
                skins
            );
            case "behavior.invulnerable" -> new FakePlayerSettings(
                settings.enabled(),
                movement,
                new FakePlayerSettings.BehaviorSettings(parseBoolean(path, value), behavior.gravity(), behavior.immovable()),
                skins
            );
            case "behavior.gravity" -> new FakePlayerSettings(
                settings.enabled(),
                movement,
                new FakePlayerSettings.BehaviorSettings(behavior.invulnerable(), parseBoolean(path, value), behavior.immovable()),
                skins
            );
            case "behavior.immovable" -> new FakePlayerSettings(
                settings.enabled(),
                movement,
                new FakePlayerSettings.BehaviorSettings(behavior.invulnerable(), behavior.gravity(), parseBoolean(path, value)),
                skins
            );
            case "skins.folder" -> new FakePlayerSettings(settings.enabled(), movement, behavior, new FakePlayerSettings.SkinsSettings(value, skins.defaultSkin()));
            case "skins.default" -> new FakePlayerSettings(settings.enabled(), movement, behavior, new FakePlayerSettings.SkinsSettings(skins.folder(), value));
            default -> throw new IllegalArgumentException("Unknown setting: " + path);
        };
    }

    private String settingValue(FakePlayerSettings settings, String rawPath) {
        String path = normalizeConfigPath(rawPath);
        return switch (path) {
            case "enabled" -> Boolean.toString(settings.enabled());
            case "movement.speed" -> Double.toString(settings.movement().speed());
            case "movement.arrival-distance" -> Double.toString(settings.movement().arrivalDistance());
            case "movement.tick-interval" -> Integer.toString(settings.movement().tickInterval());
            case "movement.wander-radius" -> Double.toString(settings.movement().wanderRadius());
            case "behavior.invulnerable" -> Boolean.toString(settings.behavior().invulnerable());
            case "behavior.gravity" -> Boolean.toString(settings.behavior().gravity());
            case "behavior.immovable" -> Boolean.toString(settings.behavior().immovable());
            case "skins.folder" -> settings.skins().folder();
            case "skins.default" -> settings.skins().defaultSkin();
            default -> throw new IllegalArgumentException("Unknown setting: " + path);
        };
    }

    private String normalizeConfigPath(String path) {
        String normalized = path.toLowerCase().replace('_', '-');
        if ("immortal".equals(normalized) || "behavior.immortal".equals(normalized)) {
            return "behavior.invulnerable";
        }
        return normalized;
    }

    private boolean parseBoolean(String path, String value) {
        if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value) || "off".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException(path + " must be true or false.");
    }

    private double parseDouble(String path, String value, double min, double max) {
        try {
            double parsed = Double.parseDouble(value);
            if (parsed < min || parsed > max) {
                throw new IllegalArgumentException(path + " must be between " + min + " and " + max + ".");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(path + " must be a number.");
        }
    }

    private int parseInt(String path, String value, int min, int max) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < min || parsed > max) {
                throw new IllegalArgumentException(path + " must be between " + min + " and " + max + ".");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(path + " must be an integer.");
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== /dfp Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/dfp spawn <name>", NamedTextColor.AQUA).append(Component.text(" - Spawn a fake player", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp despawn <name|all>", NamedTextColor.AQUA).append(Component.text(" - Despawn fake player(s)", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp move <name> wander [radius]", NamedTextColor.AQUA).append(Component.text(" - Wander nearby", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp move <name> <x> <y> <z>", NamedTextColor.AQUA).append(Component.text(" - Walk to coordinates", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp move <name> stop", NamedTextColor.AQUA).append(Component.text(" - Stop movement", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp list", NamedTextColor.AQUA).append(Component.text(" - List fake players", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp reload", NamedTextColor.AQUA).append(Component.text(" - Reload configuration", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp config <list|get|set> [setting] [value]", NamedTextColor.AQUA).append(Component.text(" - View or change plugin settings live", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp set <name> <name|skin|immortal|gravity|immovable> <value>", NamedTextColor.AQUA).append(Component.text(" - Change a fake player live", NamedTextColor.GRAY)));
    }

    private Location getSpawnLocation(CommandSender sender) {
        return sender instanceof Player player ? player.getLocation() : getDefaultSpawn();
    }

    private Location getDefaultSpawn() {
        return Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    private String formatLocation(Location loc) {
        return String.format("(%.1f, %.1f, %.1f) in %s",
            loc.getX(), loc.getY(), loc.getZ(),
            loc.getWorld().getName());
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("spawn", "despawn", "remove", "list", "move", "reload", "config", "settings", "set"), args[0]);
        }

        return switch (args[0].toLowerCase()) {
            case "despawn", "remove", "move" -> {
                if (args.length == 2) {
                    yield filter(store.getKeys().stream().toList(), args[1]);
                } else if (args.length == 3 && "move".equals(args[0].toLowerCase())) {
                    yield filter(Arrays.asList("wander", "stop"), args[2]);
                }
                yield Collections.emptyList();
            }
            case "config", "settings" -> {
                if (args.length == 2) {
                    yield filter(Arrays.asList("list", "get", "set"), args[1]);
                } else if (args.length == 3 && ("get".equalsIgnoreCase(args[1]) || "set".equalsIgnoreCase(args[1]))) {
                    yield filter(CONFIG_PATHS, args[2]);
                } else if (args.length == 4 && "set".equalsIgnoreCase(args[1])) {
                    yield filter(settingValueSuggestions(args[2]), args[3]);
                }
                yield Collections.emptyList();
            }
            case "set" -> {
                if (args.length == 2) {
                    yield filter(store.getKeys().stream().toList(), args[1]);
                } else if (args.length == 3) {
                    yield filter(PLAYER_PROPERTIES, args[2]);
                } else if (args.length == 4) {
                    yield filter(settingValueSuggestions(args[2]), args[3]);
                }
                yield Collections.emptyList();
            }
            default -> Collections.emptyList();
        };
    }

    private List<String> settingValueSuggestions(String path) {
        String normalized = normalizeConfigPath(path);
        if (normalized.equals("enabled")
            || normalized.startsWith("behavior.")
            || "immortal".equalsIgnoreCase(path)
            || "invulnerable".equalsIgnoreCase(path)
            || "gravity".equalsIgnoreCase(path)
            || "immovable".equalsIgnoreCase(path)) {
            return Arrays.asList("true", "false");
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream()
            .filter(s -> s.toLowerCase().startsWith(lower))
            .sorted()
            .collect(Collectors.toList());
    }
}
