package dev.distorteduniverse.team;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class DistortedUniverseTeamPlugin extends JavaPlugin {
    private TeamSettingsService settingsService;
    private TeamStore teamStore;
    private TeamPlayerStore playerStore;
    private TeamManager teamManager;
    private TeamGuiManager guiManager;
    private AutoKitIntegration autoKitIntegration;
    private TeamListener listener;
    private TeamCommand command;

    @Override
    public void onEnable() {
        settingsService = new TeamSettingsService(this);
        settingsService.load();

        teamStore = new TeamStore(getDataFolder());
        teamStore.load();

        playerStore = new TeamPlayerStore(getDataFolder());
        playerStore.load();

        autoKitIntegration = new AutoKitIntegration(this);

        teamManager = new TeamManager(this, teamStore, playerStore);
        teamManager.initialize();

        guiManager = new TeamGuiManager(this, teamManager);

        listener = new TeamListener(teamManager);
        getServer().getPluginManager().registerEvents(listener, this);
        getServer().getPluginManager().registerEvents(guiManager, this);

        command = new TeamCommand(this);
        getCommand("duteam").setExecutor(command);
        getCommand("duteam").setTabCompleter(command);

        getLogger().info("DistortedUniverseTeam enabled!");
    }

    @Override
    public void onDisable() {
        if (teamStore != null) {
            teamStore.save();
        }
        if (playerStore != null) {
            playerStore.save();
        }

        getLogger().info("DistortedUniverseTeam disabled!");
    }

    public void saveAll() {
        if (teamStore != null) {
            teamStore.save();
        }
        if (playerStore != null) {
            playerStore.save();
        }
        if (settingsService != null) {
            settingsService.save();
        }
    }

    public TeamSettingsService getSettingsService() {
        return settingsService;
    }

    public TeamStore getTeamStore() {
        return teamStore;
    }

    public TeamPlayerStore getPlayerStore() {
        return playerStore;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public TeamGuiManager getGuiManager() {
        return guiManager;
    }

    public AutoKitIntegration getAutoKitIntegration() {
        return autoKitIntegration;
    }
}
