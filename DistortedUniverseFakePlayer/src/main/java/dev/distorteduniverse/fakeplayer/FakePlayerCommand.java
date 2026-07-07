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
        if (store.get(key).isPresent()) {
            sender.sendMessage(Component.text("Fake player already exists: " + name, NamedTextColor.RED));
            return true;
        }

        FakePlayerSettings settings = settingsService.settings();
        Location spawnLoc = sender instanceof Player player ? player.getLocation() : getDefaultSpawn();

        UUID uuid = UUID.randomUUID();
        FakePlayer fakePlayer = new FakePlayer(name, uuid, spawnLoc, settings.skins().defaultSkin(), false);

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
            String status;
            if (!manager.isSpawned(fp.uuid())) {
                status = "DESPAWNED";
            } else if (movementService.isMoving(fp.uuid())) {
                status = "WALKING";
            } else {
                status = "IDLE";
            }

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
        FakePlayerSettings settings = settingsService.settings();
        plugin.getSkinLoader().load(settings.skins());
        manager.updateBehavior(settings.behavior());
        movementService.updateSettings(settings.movement());
        sender.sendMessage(Component.text("Configuration reloaded!", NamedTextColor.GREEN));
        return true;
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
            return filter(Arrays.asList("spawn", "despawn", "remove", "list", "move", "reload"), args[0]);
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
            default -> Collections.emptyList();
        };
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream()
            .filter(s -> s.toLowerCase().startsWith(lower))
            .sorted()
            .collect(Collectors.toList());
    }
}
