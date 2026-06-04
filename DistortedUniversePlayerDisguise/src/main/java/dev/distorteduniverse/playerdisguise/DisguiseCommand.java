package dev.distorteduniverse.playerdisguise;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class DisguiseCommand implements TabExecutor {
    private static final String PREFIX = "[DUDisguise] ";
    private static final List<String> SUBCOMMANDS = List.of(
        "help",
        "status",
        "set",
        "clear",
        "list",
        "refresh",
        "cache-refresh",
        "reload",
        "save",
        "get",
        "config"
    );

    private final Plugin plugin;
    private final DisguiseSettingsService settingsService;
    private final DisguiseStore disguiseStore;
    private final ProfileLookupService profileLookupService;
    private final DisguiseDisplayService displayService;

    public DisguiseCommand(
        Plugin plugin,
        DisguiseSettingsService settingsService,
        DisguiseStore disguiseStore,
        ProfileLookupService profileLookupService,
        DisguiseDisplayService displayService
    ) {
        this.plugin = plugin;
        this.settingsService = settingsService;
        this.disguiseStore = disguiseStore;
        this.profileLookupService = profileLookupService;
        this.displayService = displayService;
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
            case "set" -> setDisguise(sender, label, args);
            case "clear" -> clearDisguise(sender, label, args);
            case "list" -> listDisguises(sender);
            case "refresh" -> {
                displayService.refreshAll();
                sender.sendMessage(PREFIX + "Refreshed online disguises.");
            }
            case "cache-refresh" -> refreshCache(sender, label, args);
            case "reload" -> {
                settingsService.reload();
                disguiseStore.load();
                displayService.refreshAll();
                sender.sendMessage(PREFIX + "Reloaded config.yml and data.yml.");
            }
            case "save" -> {
                settingsService.save();
                disguiseStore.save();
                sender.sendMessage(PREFIX + "Saved config.yml and data.yml.");
            }
            case "get" -> getSetting(sender, label, args);
            case "config" -> setSetting(sender, label, args);
            default -> sender.sendMessage(PREFIX + "Unknown command. Use /" + label + " help.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return matching(SUBCOMMANDS, args[0]);
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && List.of("set", "clear", "status").contains(subcommand)) {
            return matching(playerNames(), args[1]);
        }
        if (args.length == 2 && (subcommand.equals("get") || subcommand.equals("config"))) {
            return matching(DisguiseSettingParser.PATHS, args[1]);
        }
        if (args.length == 3 && subcommand.equals("config")) {
            return suggestionsForValue(args[1], args[2]);
        }
        return List.of();
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(PREFIX + "/" + label + " status [player]");
        sender.sendMessage(PREFIX + "/" + label + " set <player> <minecraft-username>");
        sender.sendMessage(PREFIX + "/" + label + " clear <player>");
        sender.sendMessage(PREFIX + "/" + label + " list");
        sender.sendMessage(PREFIX + "/" + label + " refresh");
        sender.sendMessage(PREFIX + "/" + label + " cache-refresh <minecraft-username>");
        sender.sendMessage(PREFIX + "/" + label + " get <path>");
        sender.sendMessage(PREFIX + "/" + label + " config <path> <value...>");
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
            String status = disguiseStore.entry(target.playerId())
                .map(entry -> entry.profileName() + " (" + entry.sourceId() + ")")
                .orElse("none");
            sender.sendMessage(PREFIX + target.name() + " disguise: " + status);
            return;
        }

        DisguiseSettings settings = settingsService.settings();
        sender.sendMessage(PREFIX + "plugin: " + enabled(settings.enabled())
            + ", self-sees-disguise=" + settings.visibility().selfSeesDisguise()
            + ", protocol-profile=" + settings.apply().protocolProfile()
            + ", cache-days=" + settings.profileLookup().cacheDays());
        sender.sendMessage(PREFIX + "disguised players: " + disguiseStore.size());
    }

    private void setDisguise(CommandSender sender, String label, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(PREFIX + "Usage: /" + label + " set <player> <minecraft-username>");
            return;
        }

        TargetPlayer target = resolveTarget(args[1]);
        if (target == null) {
            sender.sendMessage(PREFIX + "Player must be online or known to this server: " + args[1]);
            return;
        }

        String sourceName;
        try {
            sourceName = DisguiseSettingParser.validateUsername(args[2]);
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(PREFIX + "Error: " + exception.getMessage());
            return;
        }

        Optional<ProfileLookupService.ResolvedProfile> cachedProfile = findUsableCachedProfile(sourceName);
        if (cachedProfile.isPresent()) {
            setResolvedDisguise(sender, target, cachedProfile.get());
            return;
        }

        sender.sendMessage(PREFIX + "Looking up Minecraft profile: " + sourceName + "...");
        profileLookupService.lookup(sourceName).thenAccept(result ->
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!result.success()) {
                    sender.sendMessage(PREFIX + "Error: " + result.message());
                    return;
                }
                setResolvedDisguise(sender, target, result.profile());
            })
        );
    }

    private Optional<ProfileLookupService.ResolvedProfile> findUsableCachedProfile(String sourceName) {
        long now = System.currentTimeMillis();
        boolean refreshExpired = settingsService.settings().profileLookup().refreshExpiredCache();
        String normalized = sourceName.toLowerCase(Locale.ROOT);
        return disguiseStore.entries().stream()
            .filter(entry -> entry.sourceName().equalsIgnoreCase(normalized) || entry.profileName().equalsIgnoreCase(normalized))
            .filter(entry -> !refreshExpired || !settingsService.settings().isCacheExpired(entry.cachedAtEpochMillis(), now))
            .findFirst()
            .map(entry -> new ProfileLookupService.ResolvedProfile(
                entry.sourceName(),
                entry.sourceId(),
                entry.profileName(),
                entry.textureValue(),
                entry.textureSignature(),
                entry.cachedAtEpochMillis()
            ));
    }

    private void setResolvedDisguise(CommandSender sender, TargetPlayer target, ProfileLookupService.ResolvedProfile profile) {
        DisguiseStore.DisguiseEntry entry = new DisguiseStore.DisguiseEntry(
            target.playerId(),
            target.name(),
            profile.sourceName(),
            profile.sourceId(),
            profile.profileName(),
            profile.textureValue(),
            profile.textureSignature(),
            profile.cachedAtEpochMillis()
        );
        boolean changed = disguiseStore.setDisguise(entry);
        disguiseStore.save();
        Player onlinePlayer = Bukkit.getPlayer(target.playerId());
        if (onlinePlayer != null) {
            displayService.apply(onlinePlayer);
        }
        sender.sendMessage(PREFIX + target.name()
            + (changed ? " is now disguised as " : " was already disguised as ")
            + profile.profileName() + ".");
    }

    private void clearDisguise(CommandSender sender, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(PREFIX + "Usage: /" + label + " clear <player>");
            return;
        }

        TargetPlayer target = resolveTarget(args[1]);
        if (target == null) {
            sender.sendMessage(PREFIX + "Player must be online or known to this server: " + args[1]);
            return;
        }

        boolean changed = disguiseStore.clearDisguise(target.playerId());
        disguiseStore.save();
        Player onlinePlayer = Bukkit.getPlayer(target.playerId());
        if (onlinePlayer != null) {
            displayService.clear(onlinePlayer);
        }
        sender.sendMessage(PREFIX + target.name() + (changed ? " disguise cleared." : " had no disguise."));
    }

    private void listDisguises(CommandSender sender) {
        List<DisguiseStore.DisguiseEntry> entries = disguiseStore.entries();
        if (entries.isEmpty()) {
            sender.sendMessage(PREFIX + "No disguises are set.");
            return;
        }

        String summary = String.join(", ", entries.stream()
            .map(entry -> entry.name() + "->" + entry.profileName())
            .toList());
        sender.sendMessage(PREFIX + "Disguises: " + summary);
    }

    private void refreshCache(CommandSender sender, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(PREFIX + "Usage: /" + label + " cache-refresh <minecraft-username>");
            return;
        }

        String sourceName;
        try {
            sourceName = DisguiseSettingParser.validateUsername(args[1]);
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(PREFIX + "Error: " + exception.getMessage());
            return;
        }

        sender.sendMessage(PREFIX + "Refreshing Minecraft profile: " + sourceName + "...");
        profileLookupService.lookup(sourceName).thenAccept(result ->
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!result.success()) {
                    sender.sendMessage(PREFIX + "Error: " + result.message());
                    return;
                }

                int updated = 0;
                for (DisguiseStore.DisguiseEntry entry : disguiseStore.entries()) {
                    if (entry.sourceName().equalsIgnoreCase(sourceName) || entry.profileName().equalsIgnoreCase(sourceName)) {
                        disguiseStore.setDisguise(entry.withProfile(result.profile()));
                        Player onlinePlayer = Bukkit.getPlayer(entry.playerId());
                        if (onlinePlayer != null) {
                            displayService.apply(onlinePlayer);
                        }
                        updated++;
                    }
                }
                disguiseStore.save();
                sender.sendMessage(PREFIX + "Refreshed " + result.profile().profileName()
                    + " profile cache for " + updated + " disguise(s).");
            })
        );
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
            sender.sendMessage(PREFIX + "Usage: /" + label + " config <path> <value...>");
            return;
        }
        String path = args[1];
        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        sendResult(sender, settingsService.set(path, value));
        displayService.refreshAll();
    }

    private TargetPlayer resolveTarget(String rawName) {
        Player onlinePlayer = Bukkit.getPlayerExact(rawName);
        if (onlinePlayer != null) {
            return new TargetPlayer(onlinePlayer.getUniqueId(), onlinePlayer.getName());
        }

        return disguiseStore.findByName(rawName)
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
        names.addAll(disguiseStore.knownNames());
        return new ArrayList<>(names);
    }

    private static void sendResult(CommandSender sender, DisguiseSettingsService.SettingResult result) {
        sender.sendMessage(PREFIX + (result.success() ? result.message() : "Error: " + result.message()));
    }

    private static String enabled(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }

    private static List<String> suggestionsForValue(String rawPath, String prefix) {
        String path = DisguiseSettingParser.normalizePath(rawPath);
        if (!DisguiseSettingParser.PATHS.contains(path)) {
            return List.of();
        }
        if (path.equals("enabled")
            || path.equals("visibility.self-sees-disguise")
            || path.equals("profile-lookup.refresh-expired-cache")
            || path.startsWith("apply.")) {
            return matching(List.of("true", "false"), prefix);
        }
        if (path.equals("profile-lookup.cache-days")) {
            return matching(List.of("0", "1", "7", "30"), prefix);
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

