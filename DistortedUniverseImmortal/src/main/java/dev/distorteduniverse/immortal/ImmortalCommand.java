package dev.distorteduniverse.immortal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class ImmortalCommand implements TabExecutor {
    private static final String PREFIX = "[DUImmortal] ";
    private static final List<String> SUBCOMMANDS = List.of(
        "help",
        "status",
        "set",
        "unset",
        "toggle",
        "list",
        "reload",
        "save",
        "get",
        "config"
    );

    private final ImmortalSettingsService settingsService;
    private final ImmortalPlayerStore playerStore;

    public ImmortalCommand(ImmortalSettingsService settingsService, ImmortalPlayerStore playerStore) {
        this.settingsService = settingsService;
        this.playerStore = playerStore;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "status" -> sendStatus(sender, args);
            case "set" -> setPlayer(sender, label, args);
            case "unset" -> unsetPlayer(sender, label, args);
            case "toggle" -> togglePlayer(sender, label, args);
            case "list" -> listPlayers(sender);
            case "reload" -> {
                settingsService.reload();
                playerStore.load();
                sender.sendMessage(PREFIX + "Reloaded config.yml and data.yml.");
            }
            case "save" -> {
                settingsService.save();
                playerStore.save();
                sender.sendMessage(PREFIX + "Saved config.yml and data.yml.");
            }
            case "get" -> getSetting(sender, label, args);
            case "config" -> setSetting(sender, label, args);
            default -> {
                if (args.length == 1) {
                    togglePlayer(sender, label, new String[] {"toggle", args[0]});
                    return true;
                }
                sender.sendMessage(PREFIX + "Unknown command. Use /" + label + " help.");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            Set<String> values = new LinkedHashSet<>(SUBCOMMANDS);
            values.addAll(playerNames());
            return matching(new ArrayList<>(values), args[0]);
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && List.of("set", "unset", "toggle", "status").contains(subcommand)) {
            return matching(playerNames(), args[1]);
        }
        if (args.length == 2 && (subcommand.equals("get") || subcommand.equals("config"))) {
            return matching(ImmortalSettingParser.PATHS, args[1]);
        }
        if (args.length == 3 && subcommand.equals("config")) {
            return suggestionsForValue(args[1], args[2]);
        }
        return List.of();
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(PREFIX + "/" + label + " status [player]");
        sender.sendMessage(PREFIX + "/" + label + " set <player>");
        sender.sendMessage(PREFIX + "/" + label + " unset <player>");
        sender.sendMessage(PREFIX + "/" + label + " toggle <player>");
        sender.sendMessage(PREFIX + "/" + label + " list");
        sender.sendMessage(PREFIX + "/" + label + " get <enabled|minimum-health|totem-compatibility.enabled>");
        sender.sendMessage(PREFIX + "/" + label + " config <path> <value>");
        sender.sendMessage(PREFIX + "/" + label + " reload");
        sender.sendMessage(PREFIX + "/" + label + " save");
    }

    private void sendStatus(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            TargetPlayer target = resolveTarget(args[1]);
            if (target == null) {
                sender.sendMessage(PREFIX + "Player must be online or known to this server: " + args[1]);
                return;
            }
            sender.sendMessage(PREFIX + target.name() + " is "
                + (playerStore.isImmortal(target.playerId()) ? "immortal" : "not immortal") + ".");
            return;
        }

        ImmortalSettings settings = settingsService.settings();
        sender.sendMessage(PREFIX + "plugin: " + enabled(settings.enabled())
            + ", minimum-health=" + settings.minimumHealth()
            + ", totem-compatibility=" + enabled(settings.totemCompatibilityEnabled()));
        sender.sendMessage(PREFIX + "immortal players: " + playerStore.size());
    }

    private void setPlayer(CommandSender sender, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(PREFIX + "Usage: /" + label + " set <player>");
            return;
        }

        TargetPlayer target = resolveTarget(args[1]);
        if (target == null) {
            sender.sendMessage(PREFIX + "Player must be online or known to this server: " + args[1]);
            return;
        }

        boolean changed = playerStore.setImmortal(target.playerId(), target.name());
        playerStore.save();
        sender.sendMessage(PREFIX + target.name() + (changed ? " is now immortal." : " was already immortal."));
    }

    private void unsetPlayer(CommandSender sender, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(PREFIX + "Usage: /" + label + " unset <player>");
            return;
        }

        TargetPlayer target = resolveTarget(args[1]);
        if (target == null) {
            sender.sendMessage(PREFIX + "Player must be online or known to this server: " + args[1]);
            return;
        }

        boolean changed = playerStore.unsetImmortal(target.playerId());
        playerStore.save();
        sender.sendMessage(PREFIX + target.name() + (changed ? " is no longer immortal." : " was not immortal."));
    }

    private void togglePlayer(CommandSender sender, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(PREFIX + "Usage: /" + label + " toggle <player>");
            return;
        }

        TargetPlayer target = resolveTarget(args[1]);
        if (target == null) {
            sender.sendMessage(PREFIX + "Player must be online or known to this server: " + args[1]);
            return;
        }

        if (playerStore.isImmortal(target.playerId())) {
            playerStore.unsetImmortal(target.playerId());
            playerStore.save();
            sender.sendMessage(PREFIX + target.name() + " is no longer immortal.");
            return;
        }

        playerStore.setImmortal(target.playerId(), target.name());
        playerStore.save();
        sender.sendMessage(PREFIX + target.name() + " is now immortal.");
    }

    private void listPlayers(CommandSender sender) {
        List<ImmortalPlayerStore.ImmortalPlayerEntry> entries = playerStore.entries();
        if (entries.isEmpty()) {
            sender.sendMessage(PREFIX + "No immortal players are set.");
            return;
        }

        String names = String.join(", ", entries.stream().map(ImmortalPlayerStore.ImmortalPlayerEntry::name).toList());
        sender.sendMessage(PREFIX + "Immortal players: " + names);
    }

    private void getSetting(CommandSender sender, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(PREFIX + "Usage: /" + label + " get <path>");
            return;
        }
        sendResult(sender, settingsService.get(args[1]));
    }

    private void setSetting(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(PREFIX + "Usage: /" + label + " config <path> <value>");
            return;
        }
        String path = args[1];
        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        sendResult(sender, settingsService.set(path, value));
    }

    private TargetPlayer resolveTarget(String rawName) {
        Player onlinePlayer = Bukkit.getPlayerExact(rawName);
        if (onlinePlayer != null) {
            return new TargetPlayer(onlinePlayer.getUniqueId(), onlinePlayer.getName());
        }

        return playerStore.findByName(rawName)
            .map(entry -> new TargetPlayer(entry.playerId(), entry.name()))
            .orElseGet(() -> resolveCachedOfflinePlayer(rawName));
    }

    private static TargetPlayer resolveCachedOfflinePlayer(String rawName) {
        String normalizedName = rawName.toLowerCase(Locale.ROOT);
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            String name = offlinePlayer.getName();
            if (name != null && name.toLowerCase(Locale.ROOT).equals(normalizedName)) {
                return new TargetPlayer(offlinePlayer.getUniqueId(), name);
            }
        }
        return null;
    }

    private List<String> playerNames() {
        Set<String> names = new LinkedHashSet<>();
        names.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        names.addAll(playerStore.knownNames());
        return new ArrayList<>(names);
    }

    private static void sendResult(CommandSender sender, ImmortalSettingsService.SettingResult result) {
        sender.sendMessage(PREFIX + (result.success() ? result.message() : "Error: " + result.message()));
    }

    private static String enabled(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }

    private static List<String> suggestionsForValue(String rawPath, String prefix) {
        String path = ImmortalSettingParser.normalizePath(rawPath);
        if (!ImmortalSettingParser.PATHS.contains(path)) {
            return List.of();
        }
        if (path.equals("enabled") || path.equals("totem-compatibility.enabled")) {
            return matching(List.of("true", "false"), prefix);
        }
        if (path.equals("minimum-health")) {
            return matching(List.of("0.5", "1.0", "2.0", "10.0"), prefix);
        }
        return List.of();
    }

    private static List<String> matching(List<String> values, String prefix) {
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix)) {
                matches.add(value);
            }
        }
        return matches;
    }

    private record TargetPlayer(UUID playerId, String name) {
    }
}
