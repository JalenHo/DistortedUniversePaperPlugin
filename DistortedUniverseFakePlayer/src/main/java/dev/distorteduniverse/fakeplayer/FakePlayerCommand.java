package dev.distorteduniverse.fakeplayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
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
    private final WanderingService wanderingService;
    private final FakePlayerSkinLoader skinLoader;

    public FakePlayerCommand(DistortedUniverseFakePlayerPlugin plugin) {
        this.plugin = plugin;
        this.settingsService = plugin.getSettingsService();
        this.store = plugin.getStore();
        this.manager = plugin.getManager();
        this.wanderingService = plugin.getWanderingService();
        this.skinLoader = plugin.getSkinLoader();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "spawn" -> handleSpawn(sender, args);
            case "spawn-random" -> handleSpawnRandom(sender);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "skin" -> handleSkin(sender, args);
            case "skin-list" -> handleSkinList(sender);
            case "move" -> handleMove(sender, args);
            case "togglespam" -> handleToggleSpam(sender, args);
            case "chat" -> handleChat(sender, args);
            case "status" -> handleStatus(sender);
            case "reload" -> handleReload(sender);
            case "save" -> handleSave(sender);
            case "get" -> handleGet(sender, args);
            case "config" -> handleConfig(sender);
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

    private boolean handleSpawnRandom(CommandSender sender) {
        FakePlayerSettings settings = settingsService.settings();
        List<String> names = settings.names();

        if (names.isEmpty()) {
            sender.sendMessage(Component.text("No preset names configured!", NamedTextColor.RED));
            return true;
        }

        String name = names.get((int) (Math.random() * names.size()));
        Location spawnLoc = sender instanceof Player player ? player.getLocation() : getDefaultSpawn();

        UUID uuid = UUID.randomUUID();
        FakePlayer fakePlayer = new FakePlayer(name, uuid, spawnLoc, settings.skins().defaultSkin(), false);

        String key = name.toLowerCase() + "_" + System.currentTimeMillis();
        if (!trySpawnFakePlayer(sender, fakePlayer)) {
            return true;
        }

        store.add(key, fakePlayer);
        store.save();

        sender.sendMessage(Component.text("Spawned random fake player: " + name, NamedTextColor.GREEN));
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

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /dfp remove <name|all>", NamedTextColor.RED));
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
                    wanderingService.stopWandering(fp.uuid());
                    store.remove(key);
                });
                count++;
            }
            store.save();
            sender.sendMessage(Component.text("Removed " + count + " fake players", NamedTextColor.GREEN));
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
        wanderingService.stopWandering(fp.uuid());
        store.remove(target);
        store.save();

        sender.sendMessage(Component.text("Removed fake player: " + fp.name(), NamedTextColor.GREEN));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        Collection<FakePlayer> players = store.getAll();

        if (players.isEmpty()) {
            sender.sendMessage(Component.text("No fake players spawned.", NamedTextColor.YELLOW));
            return true;
        }

        sender.sendMessage(Component.text("=== Fake Players ===", NamedTextColor.GOLD));
        for (FakePlayer fp : players) {
            String status = manager.isSpawned(fp.uuid()) ? (wanderingService.isWandering(fp.uuid()) ? "WANDERING" : "IDLE") : "DESPAWNED";
            NamedTextColor color = "WANDERING".equals(status) ? NamedTextColor.AQUA :
                                   "IDLE".equals(status) ? NamedTextColor.GREEN : NamedTextColor.GRAY;

            sender.sendMessage(Component.text(fp.name(), color)
                .append(Component.text(" - " + status + " - Skin: " + fp.skin(), NamedTextColor.GRAY)));
        }
        return true;
    }

    private boolean handleSkin(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /dfp skin <player> <skin>", NamedTextColor.RED));
            return true;
        }

        String playerName = args[1].toLowerCase();
        String skinName = args[2];

        Optional<FakePlayer> optPlayer = store.get(playerName);
        if (optPlayer.isEmpty()) {
            sender.sendMessage(Component.text("Fake player not found: " + playerName, NamedTextColor.RED));
            return true;
        }

        if (!skinLoader.getAvailableSkins().contains(skinName)) {
            sender.sendMessage(Component.text("Skin not found: " + skinName, NamedTextColor.RED));
            sender.sendMessage(Component.text("Available skins: " + String.join(", ", skinLoader.getAvailableSkins()), NamedTextColor.GRAY));
            return true;
        }

        manager.updateSkin(optPlayer.get().uuid(), skinName);
        sender.sendMessage(Component.text("Updated skin for " + optPlayer.get().name() + " to " + skinName, NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSkinList(CommandSender sender) {
        Collection<String> skins = skinLoader.getAvailableSkins();

        sender.sendMessage(Component.text("=== Available Skins ===", NamedTextColor.GOLD));
        for (String skin : skins) {
            sender.sendMessage(Component.text("- " + skin, NamedTextColor.AQUA));
        }
        return true;
    }

    private boolean handleMove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /dfp move <name|all> [x y z|radius]", NamedTextColor.RED));
            return true;
        }

        String target = args[1].toLowerCase();

        if (args.length >= 5 && sender instanceof Player) {
            try {
                double x = Double.parseDouble(args[2]);
                double y = Double.parseDouble(args[3]);
                double z = Double.parseDouble(args[4]);
                Location loc = new Location(((Player) sender).getWorld(), x, y, z);

                Optional<FakePlayer> optPlayer = store.get(target);
                if (optPlayer.isPresent()) {
                    manager.teleportFakePlayer(optPlayer.get().uuid(), loc);
                    sender.sendMessage(Component.text("Moved " + optPlayer.get().name() + " to " + formatLocation(loc), NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Fake player not found: " + target, NamedTextColor.RED));
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid coordinates", NamedTextColor.RED));
            }
            return true;
        }

        if ("all".equals(target)) {
            if (args.length >= 3) {
                try {
                    double radius = Double.parseDouble(args[2]);
                    wanderingService.startWanderingAll(radius);
                    store.save();
                    sender.sendMessage(Component.text("Started wandering all fake players with radius " + radius, NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid radius", NamedTextColor.RED));
                }
            } else {
                wanderingService.startWanderingAll(settingsService.settings().wandering().defaultRadius());
                store.save();
                sender.sendMessage(Component.text("Started wandering all fake players", NamedTextColor.GREEN));
            }
            return true;
        }

        Optional<FakePlayer> optPlayer = store.get(target);
        if (optPlayer.isEmpty()) {
            sender.sendMessage(Component.text("Fake player not found: " + target, NamedTextColor.RED));
            return true;
        }

        FakePlayer fp = optPlayer.get();
        wanderingService.startWandering(target, fp);
        store.save();
        sender.sendMessage(Component.text("Started wandering for " + fp.name(), NamedTextColor.GREEN));
        return true;
    }

    private boolean handleToggleSpam(CommandSender sender, String[] args) {
        sender.sendMessage(Component.text("Message toggles are configured in config.yml", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Use /dfp reload to apply changes.", NamedTextColor.GRAY));
        return true;
    }

    private boolean handleChat(CommandSender sender, String[] args) {
        sender.sendMessage(Component.text("Chat responses are configured in config.yml", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Use /dfp reload to apply changes.", NamedTextColor.GRAY));
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        FakePlayerSettings settings = settingsService.settings();

        sender.sendMessage(Component.text("=== FakePlayer Status ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Enabled: " + settings.enabled(), settings.enabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
        sender.sendMessage(Component.text("Spawned: " + store.getAll().stream().filter(fp -> manager.isSpawned(fp.uuid())).count(), NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Total Preset Names: " + settings.names().size(), NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Available Skins: " + skinLoader.getAvailableSkins().size(), NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Wandering: " + store.getAll().stream().filter(fp -> wanderingService.isWandering(fp.uuid())).count(), NamedTextColor.AQUA));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        settingsService.reload();
        FakePlayerSettings settings = settingsService.settings();
        skinLoader.load(settings.skins());
        manager.updateBehavior(settings.behavior());
        wanderingService.updateSettings(settings.wandering());
        sender.sendMessage(Component.text("Configuration reloaded!", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSave(CommandSender sender) {
        store.save();
        settingsService.save();
        sender.sendMessage(Component.text("Data saved!", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleGet(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /dfp get <name>", NamedTextColor.RED));
            return true;
        }

        String target = args[1].toLowerCase();
        Optional<FakePlayer> optPlayer = store.get(target);

        if (optPlayer.isEmpty()) {
            sender.sendMessage(Component.text("Fake player not found: " + target, NamedTextColor.RED));
            return true;
        }

        FakePlayer fp = optPlayer.get();
        sender.sendMessage(Component.text("=== FakePlayer Info ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Name: " + fp.name(), NamedTextColor.AQUA));
        sender.sendMessage(Component.text("UUID: " + fp.uuid(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Location: " + formatLocation(fp.location()), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Skin: " + fp.skin(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Status: " + (manager.isSpawned(fp.uuid()) ? "SPAWNED" : "DESPAWNED"), NamedTextColor.GRAY));
        return true;
    }

    private boolean handleConfig(CommandSender sender) {
        FakePlayerSettings settings = settingsService.settings();
        sender.sendMessage(Component.text("=== Configuration ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Enabled: " + settings.enabled(), NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Names: " + String.join(", ", settings.names()), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Wandering Radius: " + settings.wandering().defaultRadius(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Tick Interval: " + settings.wandering().tickInterval(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Use Pathfinding: " + settings.wandering().usePathfinding(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Invulnerable: " + settings.behavior().invulnerable(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Gravity: " + settings.behavior().gravity(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Immovable: " + settings.behavior().immovable(), NamedTextColor.GRAY));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== /dfp Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/dfp spawn <name>", NamedTextColor.AQUA).append(Component.text(" - Spawn a fake player", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp spawn-random", NamedTextColor.AQUA).append(Component.text(" - Spawn with random preset name", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp remove <name|all>", NamedTextColor.AQUA).append(Component.text(" - Remove fake player(s)", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp list", NamedTextColor.AQUA).append(Component.text(" - List all fake players", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp skin <player> <skin>", NamedTextColor.AQUA).append(Component.text(" - Set player skin", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp skin-list", NamedTextColor.AQUA).append(Component.text(" - List available skins", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp move <name|all> [x y z]", NamedTextColor.AQUA).append(Component.text(" - Move or start wandering", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp status", NamedTextColor.AQUA).append(Component.text(" - Show plugin status", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp reload", NamedTextColor.AQUA).append(Component.text(" - Reload configuration", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/dfp save", NamedTextColor.AQUA).append(Component.text(" - Save all data", NamedTextColor.GRAY)));
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
            return filter(Arrays.asList("spawn", "spawn-random", "remove", "list", "skin", "skin-list", "move", "togglespam", "chat", "status", "reload", "save", "get", "config"), args[0]);
        }

        return switch (args[0].toLowerCase()) {
            case "remove", "get", "move", "skin" -> {
                if (args.length == 2) {
                    yield filter(store.getKeys().stream().toList(), args[1]);
                } else if (args.length == 3 && "skin".equals(args[0].toLowerCase())) {
                    yield filter(skinLoader.getAvailableSkins().stream().toList(), args[2]);
                }
                yield Collections.emptyList();
            }
            case "togglespam" -> filter(Arrays.asList("join", "leave", "death", "on", "off"), args[args.length - 1]);
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
