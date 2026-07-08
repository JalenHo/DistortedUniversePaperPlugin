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
    private static final List<String> CONFIG_KEYS = List.of(
        "enabled",
        "behavior.invulnerable",
        "behavior.immortal",
        "behavior.knockback-when-invulnerable",
        "behavior.gravity",
        "behavior.immovable",
        "movement.speed",
        "movement.arrival-distance",
        "movement.tick-interval",
        "movement.wander-radius",
        "display.tab-list",
        "skins.default"
    );

    private static final List<String> SET_KEYS = List.of(
        "name",
        "skin",
        "invulnerable",
        "immortal",
        "wandering"
    );

    private final DistortedUniverseFakePlayerPlugin plugin;
    private final FakePlayerSettingsService settingsService;
    private final FakePlayerStore store;
    private final FakePlayerManager manager;
    private final BotMovementService movementService;
    private final FakePlayerLifecycleService lifecycleService;

    public FakePlayerCommand(DistortedUniverseFakePlayerPlugin plugin) {
        this.plugin = plugin;
        this.settingsService = plugin.getSettingsService();
        this.store = plugin.getStore();
        this.manager = plugin.getManager();
        this.movementService = plugin.getMovementService();
        this.lifecycleService = plugin.getLifecycleService();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "spawn" -> handleSpawn(sender, args);
            case "despawn", "remove" -> handleDespawn(sender, args);
            case "list" -> handleList(sender);
            case "move" -> handleMove(sender, args);
            case "reload" -> handleReload(sender);
            case "config" -> handleConfig(sender, args);
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
        String key = name.toLowerCase(Locale.ROOT);
        if (store.get(key).isPresent() || store.findKey(name).isPresent()) {
            sender.sendMessage(Component.text("Fake player already exists: " + name, NamedTextColor.RED));
            return true;
        }

        FakePlayerSettings settings = settingsService.settings();
        Location spawnLoc = sender instanceof Player player ? player.getLocation() : getDefaultSpawn();

        UUID uuid = UUID.randomUUID();
        FakePlayer fakePlayer = new FakePlayer(name, uuid, manager.snapSpawnLocation(spawnLoc), settings.skins().defaultSkin(), false);

        if (!trySpawnFakePlayer(sender, key, fakePlayer)) {
            return true;
        }

        store.add(key, fakePlayer);
        store.save();

        sender.sendMessage(Component.text("Spawned fake player: " + name, NamedTextColor.GREEN));
        return true;
    }

    private boolean trySpawnFakePlayer(CommandSender sender, String key, FakePlayer fakePlayer) {
        try {
            if (!lifecycleService.spawn(key, fakePlayer)) {
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

        String target = args[1];

        if ("all".equalsIgnoreCase(target)) {
            int count = 0;
            for (String key : new ArrayList<>(store.getKeys())) {
                if (lifecycleService.remove(key, FakePlayerLifecycleService.RemovalReason.DESPAWN).isPresent()) {
                    count++;
                }
            }
            sender.sendMessage(Component.text("Despawned " + count + " fake players", NamedTextColor.GREEN));
            return true;
        }

        Optional<FakePlayer> removed = lifecycleService.remove(target, FakePlayerLifecycleService.RemovalReason.DESPAWN);
        if (removed.isEmpty()) {
            sender.sendMessage(Component.text("Fake player not found: " + target, NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("Despawned fake player: " + removed.get().name(), NamedTextColor.GREEN));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        int orphaned = manager.cleanupOrphanedFakePlayers();
        if (orphaned > 0) {
            sender.sendMessage(Component.text(
                "Cleaned up " + orphaned + " orphaned fake player entr" + (orphaned == 1 ? "y" : "ies"),
                NamedTextColor.YELLOW
            ));
        }

        Collection<FakePlayer> players = store.getAll();

        if (players.isEmpty()) {
            sender.sendMessage(Component.text("No fake players configured.", NamedTextColor.YELLOW));
            return true;
        }

        sender.sendMessage(Component.text("=== Fake Players ===", NamedTextColor.GOLD));
        for (FakePlayer fp : players) {
            boolean spawned = manager.isSpawned(fp.uuid());
            String status = !spawned ? "DESPAWNED" : movementService.isMoving(fp.uuid()) ? "WALKING" : "IDLE";
            boolean immortal = fp.resolvesInvulnerable(settingsService.settings().behavior().invulnerable());

            NamedTextColor color = switch (status) {
                case "WALKING" -> NamedTextColor.AQUA;
                case "IDLE" -> NamedTextColor.GREEN;
                default -> NamedTextColor.GRAY;
            };

            sender.sendMessage(Component.text(fp.name(), color)
                .append(Component.text(" - " + status, NamedTextColor.GRAY))
                .append(Component.text(immortal ? " [immortal]" : "", NamedTextColor.LIGHT_PURPLE)));
        }
        return true;
    }

    private boolean handleMove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /dfp move <name> <wander|stop|x y z>", NamedTextColor.RED));
            return true;
        }

        Optional<String> key = store.findKey(args[1]);
        if (key.isEmpty()) {
            sender.sendMessage(Component.text("Fake player not found: " + args[1], NamedTextColor.RED));
            return true;
        }

        Optional<FakePlayer> optPlayer = store.get(key.get());
        if (optPlayer.isEmpty()) {
            sender.sendMessage(Component.text("Fake player not found: " + args[1], NamedTextColor.RED));
            return true;
        }

        FakePlayer fp = optPlayer.get();
        if (!manager.isSpawned(fp.uuid())) {
            sender.sendMessage(Component.text("Fake player is not spawned: " + fp.name(), NamedTextColor.RED));
            return true;
        }

        String action = args[2].toLowerCase(Locale.ROOT);
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

            movementService.startWandering(key.get(), fp, radius);
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
            movementService.startMoveTo(key.get(), fp, destination);
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
        try {
            settingsService.reload();
            FakePlayerSettings settings = settingsService.settings();
            plugin.getSkinLoader().load(settings.skins());
            manager.updateBehavior(settings.behavior());
            manager.updateDisplay(settings.display());
            manager.updateMovementSpeed(settings.movement().speed());
            movementService.updateSettings(settings.movement());
            movementService.reloadAndResume();

            for (FakePlayer fp : store.getAll()) {
                manager.refreshAppearance(fp);
            }

            sender.sendMessage(Component.text("Configuration reloaded!", NamedTextColor.GREEN));
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to reload fake player configuration", exception);
            sender.sendMessage(Component.text("Reload failed (see server log).", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleConfig(CommandSender sender, String[] args) {
        if (args.length == 1 || "list".equalsIgnoreCase(args[1])) {
            sendConfigList(sender);
            return true;
        }

        if ("get".equalsIgnoreCase(args[1])) {
            if (args.length < 3) {
                sender.sendMessage(Component.text("Usage: /dfp config get <key>", NamedTextColor.RED));
                return true;
            }
            String key = normalizeConfigKey(args[2]);
            Optional<String> value = readConfigValue(key);
            if (value.isEmpty()) {
                sender.sendMessage(Component.text("Unknown config key: " + args[2], NamedTextColor.RED));
                sendConfigKeys(sender);
                return true;
            }
            sender.sendMessage(Component.text(key + " = ", NamedTextColor.AQUA)
                .append(Component.text(value.get(), NamedTextColor.WHITE)));
            return true;
        }

        if ("set".equalsIgnoreCase(args[1])) {
            if (args.length < 4) {
                sender.sendMessage(Component.text("Usage: /dfp config set <key> <value>", NamedTextColor.RED));
                return true;
            }
            String key = normalizeConfigKey(args[2]);
            String rawValue = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
            Optional<String> error = applyConfigValue(key, rawValue);
            if (error.isPresent()) {
                sender.sendMessage(Component.text(error.get(), NamedTextColor.RED));
                return true;
            }
            applyLiveSettings();
            sender.sendMessage(Component.text("Set " + key + " = " + readConfigValue(key).orElse(rawValue), NamedTextColor.GREEN));
            return true;
        }

        sender.sendMessage(Component.text("Usage: /dfp config [list|get <key>|set <key> <value>]", NamedTextColor.RED));
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /dfp set <name> <property> <value>", NamedTextColor.RED));
            sender.sendMessage(Component.text("Properties: name, skin, invulnerable|immortal, wandering", NamedTextColor.GRAY));
            return true;
        }

        Optional<String> keyOpt = store.findKey(args[1]);
        if (keyOpt.isEmpty()) {
            sender.sendMessage(Component.text("Fake player not found: " + args[1], NamedTextColor.RED));
            return true;
        }

        String key = keyOpt.get();
        Optional<FakePlayer> opt = store.get(key);
        if (opt.isEmpty()) {
            sender.sendMessage(Component.text("Fake player not found: " + args[1], NamedTextColor.RED));
            return true;
        }

        FakePlayer fp = opt.get();
        String property = args[2].toLowerCase(Locale.ROOT);
        String rawValue = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

        switch (property) {
            case "name" -> {
                String newName = rawValue.trim();
                if (newName.isEmpty() || newName.length() > 16) {
                    sender.sendMessage(Component.text("Name must be 1-16 characters.", NamedTextColor.RED));
                    return true;
                }
                String newKey = newName.toLowerCase(Locale.ROOT);
                if (!newKey.equals(key) && (store.contains(newKey) || store.findKey(newName).isPresent())) {
                    sender.sendMessage(Component.text("Another fake player already uses that name.", NamedTextColor.RED));
                    return true;
                }
                boolean wasWandering = fp.isWandering() || movementService.isMoving(fp.uuid());
                movementService.stop(fp.uuid(), false);
                FakePlayer renamed = fp.withName(newName);
                store.remove(key);
                store.add(newKey, renamed);
                store.save();
                manager.refreshAppearance(renamed);
                if (wasWandering && manager.isSpawned(renamed.uuid())) {
                    movementService.startWandering(newKey, renamed, settingsService.settings().movement().wanderRadius());
                    store.save();
                }
                sender.sendMessage(Component.text("Renamed fake player to " + newName, NamedTextColor.GREEN));
            }
            case "skin" -> {
                String skin = rawValue.trim();
                if (plugin.getSkinLoader().getSkin(skin).isEmpty() && !"default".equalsIgnoreCase(skin)) {
                    sender.sendMessage(Component.text(
                        "Skin not found: " + skin + ". Available: " + String.join(", ", plugin.getSkinLoader().getAvailableSkins()),
                        NamedTextColor.RED
                    ));
                    return true;
                }
                FakePlayer updated = fp.withSkin(skin);
                store.update(key, updated);
                store.save();
                manager.refreshAppearance(updated);
                sender.sendMessage(Component.text("Set skin of " + updated.name() + " to " + skin, NamedTextColor.GREEN));
            }
            case "invulnerable", "immortal" -> {
                Optional<Boolean> parsed = parseBoolean(rawValue);
                if (parsed.isEmpty()) {
                    if ("reset".equalsIgnoreCase(rawValue) || "default".equalsIgnoreCase(rawValue) || "null".equalsIgnoreCase(rawValue)) {
                        FakePlayer updated = fp.withInvulnerableOverride(null);
                        store.update(key, updated);
                        store.save();
                        manager.refreshAppearance(updated);
                        sender.sendMessage(Component.text(
                            "Reset immortal override for " + updated.name() + " (now uses global config).",
                            NamedTextColor.GREEN
                        ));
                        return true;
                    }
                    sender.sendMessage(Component.text("Value must be true/false (or reset).", NamedTextColor.RED));
                    return true;
                }
                FakePlayer updated = fp.withInvulnerableOverride(parsed.get());
                store.update(key, updated);
                store.save();
                manager.refreshAppearance(updated);
                sender.sendMessage(Component.text(
                    "Set immortal of " + updated.name() + " to " + parsed.get(),
                    NamedTextColor.GREEN
                ));
            }
            case "wandering" -> {
                Optional<Boolean> parsed = parseBoolean(rawValue);
                if (parsed.isEmpty()) {
                    sender.sendMessage(Component.text("Value must be true/false.", NamedTextColor.RED));
                    return true;
                }
                if (parsed.get()) {
                    if (!manager.isSpawned(fp.uuid())) {
                        sender.sendMessage(Component.text("Fake player is not spawned: " + fp.name(), NamedTextColor.RED));
                        return true;
                    }
                    movementService.startWandering(key, fp, settingsService.settings().movement().wanderRadius());
                    store.save();
                    sender.sendMessage(Component.text("Started wandering for " + fp.name(), NamedTextColor.GREEN));
                } else {
                    movementService.stop(fp.uuid());
                    store.save();
                    sender.sendMessage(Component.text("Stopped wandering for " + fp.name(), NamedTextColor.GREEN));
                }
            }
            default -> {
                sender.sendMessage(Component.text("Unknown property: " + property, NamedTextColor.RED));
                sender.sendMessage(Component.text("Properties: name, skin, invulnerable|immortal, wandering", NamedTextColor.GRAY));
            }
        }
        return true;
    }

    private void applyLiveSettings() {
        FakePlayerSettings settings = settingsService.settings();
        plugin.getSkinLoader().load(settings.skins());
        manager.updateBehavior(settings.behavior());
        manager.updateDisplay(settings.display());
        manager.updateMovementSpeed(settings.movement().speed());
        movementService.updateSettings(settings.movement());
        movementService.reloadAndResume();
        for (FakePlayer fp : store.getAll()) {
            manager.refreshAppearance(fp);
        }
    }

    private void sendConfigList(CommandSender sender) {
        sender.sendMessage(Component.text("=== Fake Player Config ===", NamedTextColor.GOLD));
        for (String key : CONFIG_KEYS) {
            if ("behavior.immortal".equals(key)) {
                continue; // alias of invulnerable
            }
            readConfigValue(key).ifPresent(value ->
                sender.sendMessage(Component.text(key + " = ", NamedTextColor.AQUA)
                    .append(Component.text(value, NamedTextColor.WHITE)))
            );
        }
        sender.sendMessage(Component.text("Use /dfp config set <key> <value> to change live.", NamedTextColor.GRAY));
    }

    private void sendConfigKeys(CommandSender sender) {
        sender.sendMessage(Component.text(
            "Keys: " + CONFIG_KEYS.stream().filter(k -> !k.equals("behavior.immortal")).collect(Collectors.joining(", ")),
            NamedTextColor.GRAY
        ));
    }

    private String normalizeConfigKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT).replace('_', '-');
        if ("immortal".equals(normalized) || "behavior.immortal".equals(normalized)) {
            return "behavior.invulnerable";
        }
        if ("invulnerable".equals(normalized)) {
            return "behavior.invulnerable";
        }
        if (!normalized.contains(".") && CONFIG_KEYS.stream().anyMatch(k -> k.endsWith("." + normalized))) {
            return CONFIG_KEYS.stream().filter(k -> k.endsWith("." + normalized)).findFirst().orElse(normalized);
        }
        return normalized;
    }

    private Optional<String> readConfigValue(String key) {
        FakePlayerSettings settings = settingsService.settings();
        return switch (key) {
            case "enabled" -> Optional.of(Boolean.toString(settings.enabled()));
            case "behavior.invulnerable" -> Optional.of(Boolean.toString(settings.behavior().invulnerable()));
            case "behavior.knockback-when-invulnerable" -> Optional.of(Boolean.toString(settings.behavior().knockbackWhenInvulnerable()));
            case "behavior.gravity" -> Optional.of(Boolean.toString(settings.behavior().gravity()));
            case "behavior.immovable" -> Optional.of(Boolean.toString(settings.behavior().immovable()));
            case "movement.speed" -> Optional.of(Double.toString(settings.movement().speed()));
            case "movement.arrival-distance" -> Optional.of(Double.toString(settings.movement().arrivalDistance()));
            case "movement.tick-interval" -> Optional.of(Integer.toString(settings.movement().tickInterval()));
            case "movement.wander-radius" -> Optional.of(Double.toString(settings.movement().wanderRadius()));
            case "display.tab-list" -> Optional.of(Boolean.toString(settings.display().tabList()));
            case "skins.default" -> Optional.of(settings.skins().defaultSkin());
            default -> Optional.empty();
        };
    }

    private Optional<String> applyConfigValue(String key, String rawValue) {
        FakePlayerSettings current = settingsService.settings();
        try {
            switch (key) {
                case "enabled" -> {
                    Optional<Boolean> value = parseBoolean(rawValue);
                    if (value.isEmpty()) {
                        return Optional.of("Value must be true/false.");
                    }
                    settingsService.updateEnabled(value.get());
                }
                case "behavior.invulnerable" -> {
                    Optional<Boolean> value = parseBoolean(rawValue);
                    if (value.isEmpty()) {
                        return Optional.of("Value must be true/false.");
                    }
                    FakePlayerSettings.BehaviorSettings behavior = current.behavior();
                    settingsService.updateBehavior(new FakePlayerSettings.BehaviorSettings(
                        value.get(),
                        behavior.knockbackWhenInvulnerable(),
                        behavior.gravity(),
                        behavior.immovable()
                    ));
                }
                case "behavior.knockback-when-invulnerable" -> {
                    Optional<Boolean> value = parseBoolean(rawValue);
                    if (value.isEmpty()) {
                        return Optional.of("Value must be true/false.");
                    }
                    FakePlayerSettings.BehaviorSettings behavior = current.behavior();
                    settingsService.updateBehavior(new FakePlayerSettings.BehaviorSettings(
                        behavior.invulnerable(),
                        value.get(),
                        behavior.gravity(),
                        behavior.immovable()
                    ));
                }
                case "behavior.gravity" -> {
                    Optional<Boolean> value = parseBoolean(rawValue);
                    if (value.isEmpty()) {
                        return Optional.of("Value must be true/false.");
                    }
                    FakePlayerSettings.BehaviorSettings behavior = current.behavior();
                    settingsService.updateBehavior(new FakePlayerSettings.BehaviorSettings(
                        behavior.invulnerable(),
                        behavior.knockbackWhenInvulnerable(),
                        value.get(),
                        behavior.immovable()
                    ));
                }
                case "behavior.immovable" -> {
                    Optional<Boolean> value = parseBoolean(rawValue);
                    if (value.isEmpty()) {
                        return Optional.of("Value must be true/false.");
                    }
                    FakePlayerSettings.BehaviorSettings behavior = current.behavior();
                    settingsService.updateBehavior(new FakePlayerSettings.BehaviorSettings(
                        behavior.invulnerable(),
                        behavior.knockbackWhenInvulnerable(),
                        behavior.gravity(),
                        value.get()
                    ));
                }
                case "movement.speed" -> {
                    double value = Double.parseDouble(rawValue);
                    FakePlayerSettings.MovementSettings movement = current.movement();
                    settingsService.updateMovement(new FakePlayerSettings.MovementSettings(
                        clamp(value, 0.05D, 1.0D),
                        movement.arrivalDistance(),
                        movement.tickInterval(),
                        movement.wanderRadius()
                    ));
                }
                case "movement.arrival-distance" -> {
                    double value = Double.parseDouble(rawValue);
                    FakePlayerSettings.MovementSettings movement = current.movement();
                    settingsService.updateMovement(new FakePlayerSettings.MovementSettings(
                        movement.speed(),
                        clamp(value, 0.5D, 5.0D),
                        movement.tickInterval(),
                        movement.wanderRadius()
                    ));
                }
                case "movement.tick-interval" -> {
                    int value = Integer.parseInt(rawValue);
                    FakePlayerSettings.MovementSettings movement = current.movement();
                    settingsService.updateMovement(new FakePlayerSettings.MovementSettings(
                        movement.speed(),
                        movement.arrivalDistance(),
                        Math.max(1, Math.min(20, value)),
                        movement.wanderRadius()
                    ));
                }
                case "movement.wander-radius" -> {
                    double value = Double.parseDouble(rawValue);
                    FakePlayerSettings.MovementSettings movement = current.movement();
                    settingsService.updateMovement(new FakePlayerSettings.MovementSettings(
                        movement.speed(),
                        movement.arrivalDistance(),
                        movement.tickInterval(),
                        clamp(value, 1.0D, 100.0D)
                    ));
                }
                case "display.tab-list" -> {
                    Optional<Boolean> value = parseBoolean(rawValue);
                    if (value.isEmpty()) {
                        return Optional.of("Value must be true/false.");
                    }
                    settingsService.updateDisplay(new FakePlayerSettings.DisplaySettings(value.get()));
                }
                case "skins.default" -> {
                    String skin = rawValue.trim();
                    FakePlayerSettings.SkinsSettings skins = current.skins();
                    settingsService.updateSkins(new FakePlayerSettings.SkinsSettings(skins.folder(), skin));
                }
                default -> {
                    return Optional.of("Unknown config key: " + key);
                }
            }
        } catch (NumberFormatException exception) {
            return Optional.of("Invalid number: " + rawValue);
        }
        return Optional.empty();
    }

    private Optional<Boolean> parseBoolean(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "true", "yes", "on", "1" -> Optional.of(true);
            case "false", "no", "off", "0" -> Optional.of(false);
            default -> Optional.empty();
        };
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== /dfp Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/dfp spawn <name>", NamedTextColor.AQUA).append(Component.text(" - Spawn a fake player", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp despawn <name|all>", NamedTextColor.AQUA).append(Component.text(" - Despawn fake player(s)", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp move <name> wander [radius]", NamedTextColor.AQUA).append(Component.text(" - Wander nearby", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp move <name> <x> <y> <z>", NamedTextColor.AQUA).append(Component.text(" - Walk to coordinates", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp move <name> stop", NamedTextColor.AQUA).append(Component.text(" - Stop movement", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp list", NamedTextColor.AQUA).append(Component.text(" - List fake players", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp config [list|get|set]", NamedTextColor.AQUA).append(Component.text(" - Live global settings", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp set <name> <prop> <value>", NamedTextColor.AQUA).append(Component.text(" - Per-bot settings", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp reload", NamedTextColor.AQUA).append(Component.text(" - Reload configuration", NamedTextColor.GRAY)));
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
            return filter(Arrays.asList("spawn", "despawn", "remove", "list", "move", "reload", "config", "set"), args[0]);
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "despawn", "remove", "move", "set" -> {
                if (args.length == 2) {
                    yield filter(store.getKeys().stream().toList(), args[1]);
                }
                if (args.length == 3 && "move".equalsIgnoreCase(args[0])) {
                    yield filter(Arrays.asList("wander", "stop"), args[2]);
                }
                if (args.length == 3 && "set".equalsIgnoreCase(args[0])) {
                    yield filter(SET_KEYS, args[2]);
                }
                if (args.length == 4 && "set".equalsIgnoreCase(args[0])) {
                    String prop = args[2].toLowerCase(Locale.ROOT);
                    if (prop.equals("invulnerable") || prop.equals("immortal") || prop.equals("wandering")) {
                        yield filter(Arrays.asList("true", "false", "reset"), args[3]);
                    }
                    if (prop.equals("skin")) {
                        yield filter(new ArrayList<>(plugin.getSkinLoader().getAvailableSkins()), args[3]);
                    }
                }
                yield Collections.emptyList();
            }
            case "config" -> {
                if (args.length == 2) {
                    yield filter(Arrays.asList("list", "get", "set"), args[1]);
                }
                if (args.length == 3 && ("get".equalsIgnoreCase(args[1]) || "set".equalsIgnoreCase(args[1]))) {
                    yield filter(CONFIG_KEYS.stream().filter(k -> !k.equals("behavior.immortal")).toList(), args[2]);
                }
                if (args.length == 4 && "set".equalsIgnoreCase(args[1])) {
                    String key = normalizeConfigKey(args[2]);
                    if (key.startsWith("behavior.") || key.equals("enabled") || key.equals("display.tab-list")) {
                        yield filter(Arrays.asList("true", "false"), args[3]);
                    }
                }
                yield Collections.emptyList();
            }
            default -> Collections.emptyList();
        };
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
            .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lower))
            .sorted()
            .collect(Collectors.toList());
    }
}
