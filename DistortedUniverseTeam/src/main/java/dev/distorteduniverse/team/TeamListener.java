package dev.distorteduniverse.team;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Optional;

public class TeamListener implements Listener {
    private final TeamManager teamManager;
    private final TeamSettingsService settingsService;

    public TeamListener(TeamManager teamManager, TeamSettingsService settingsService) {
        this.teamManager = teamManager;
        this.settingsService = settingsService;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        teamManager.onPlayerJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        teamManager.onPlayerQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (settingsService.settings().friendlyFire()) {
            return;
        }

        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player damager = resolveDamager(event.getDamager());
        if (damager == null) {
            return;
        }

        Optional<Team> victimTeam = teamManager.getTeamByPlayer(victim.getUniqueId());
        Optional<Team> damagerTeam = teamManager.getTeamByPlayer(damager.getUniqueId());

        if (victimTeam.isPresent() && victimTeam.equals(damagerTeam)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        teamManager.getTeamByPlayer(player.getUniqueId()).ifPresent(team -> {
            Component deathMessage = Component.text(player.getName(), NamedTextColor.GRAY)
                .append(Component.text(" from team ", NamedTextColor.DARK_GRAY))
                .append(Component.text(team.displayName(), team.getTextColor()))
                .append(Component.text(" died", NamedTextColor.GRAY));
            event.deathMessage(deathMessage);
        });
    }

    private Player resolveDamager(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }

        if (damager instanceof org.bukkit.entity.Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }

        return null;
    }
}
