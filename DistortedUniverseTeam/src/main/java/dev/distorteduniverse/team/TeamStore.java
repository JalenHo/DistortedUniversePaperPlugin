package dev.distorteduniverse.team;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TeamStore {
    private final File dataFile;
    private final Map<String, Team> teams = new HashMap<>();

    public TeamStore(File dataFolder) {
        this.dataFile = new File(dataFolder, "teams.yml");
    }

    public void load() {
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

        if (config.getInt("data-version", 0) < 1) {
            migrateV0(config);
        }

        ConfigurationSection teamsSection = config.getConfigurationSection("teams");
        if (teamsSection == null) {
            return;
        }

        for (String id : teamsSection.getKeys(false)) {
            ConfigurationSection teamSection = teamsSection.getConfigurationSection(id);
            if (teamSection == null) {
                continue;
            }

            List<String> memberStrings = teamSection.getStringList("members");
            List<UUID> members = new ArrayList<>();
            for (String uuidStr : memberStrings) {
                try {
                    members.add(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException ignored) {
                }
            }

            Team team = new Team(
                id,
                teamSection.getString("display-name", id),
                teamSection.getString("color", "white"),
                teamSection.getInt("max-size", -1),
                teamSection.getBoolean("glow-enabled", true),
                teamSection.getString("glow-color", "white"),
                teamSection.getString("auto-kit", null),
                members
            );
            teams.put(id, team);
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("data-version", 1);

        ConfigurationSection teamsSection = config.createSection("teams");
        for (Map.Entry<String, Team> entry : teams.entrySet()) {
            Team team = entry.getValue();
            ConfigurationSection teamSection = teamsSection.createSection(entry.getKey());

            teamSection.set("display-name", team.displayName());
            teamSection.set("color", team.color());
            teamSection.set("max-size", team.maxSize());
            teamSection.set("glow-enabled", team.glowEnabled());
            teamSection.set("glow-color", team.glowColor());
            teamSection.set("auto-kit", team.autoKit());

            List<String> memberStrings = new ArrayList<>();
            for (UUID member : team.members()) {
                memberStrings.add(member.toString());
            }
            teamSection.set("members", memberStrings);
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save teams data", e);
        }
    }

    private void migrateV0(YamlConfiguration config) {
        // Simple migration from old format
        config.set("data-version", 1);
    }

    public void add(String id, Team team) {
        teams.put(id, team);
    }

    public void remove(String id) {
        teams.remove(id);
    }

    public Optional<Team> get(String id) {
        return Optional.ofNullable(teams.get(id));
    }

    public Collection<Team> getAll() {
        return teams.values();
    }

    public Set<String> getIds() {
        return teams.keySet();
    }

    public void update(String id, Team team) {
        teams.put(id, team);
    }

    public boolean contains(String id) {
        return teams.containsKey(id);
    }

    public Optional<Team> getTeamByMember(UUID member) {
        for (Team team : teams.values()) {
            if (team.hasMember(member)) {
                return Optional.of(team);
            }
        }
        return Optional.empty();
    }
}
