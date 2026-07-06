package dev.distorteduniverse.team;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class TeamGuiManager {
    private final DistortedUniverseTeamPlugin plugin;
    private final TeamManager teamManager;
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<UUID, String> playerViewingTeam = new HashMap<>();

    private static final int TEAMS_PER_PAGE = 6;
    private static final int[] TEAM_SLOTS = {10, 12, 14, 16, 28, 30};
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int CLOSE_SLOT = 49;

    public TeamGuiManager(DistortedUniverseTeamPlugin plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }

    public void openMainGui(Player player) {
        playerPages.put(player.getUniqueId(), 0);
        playerViewingTeam.remove(player.getUniqueId());
        updateMainGui(player);
    }

    public void updateMainGui(Player player) {
        int page = playerPages.getOrDefault(player.getUniqueId(), 0);
        int totalPages = Math.max(1, (int) Math.ceil((double) teamManager.getAllTeams().size() / TEAMS_PER_PAGE));
        page = Math.min(page, totalPages - 1);

        String title = "Team Manager - Page " + (page + 1) + "/" + totalPages;
        Inventory gui = Bukkit.createInventory(null, 54, Component.text(title, NamedTextColor.GOLD));

        Collection<Team> teams = teamManager.getAllTeams();
        List<Team> teamList = new ArrayList<>(teams);
        int startIndex = page * TEAMS_PER_PAGE;

        for (int i = 0; i < TEAM_SLOTS.length && startIndex + i < teamList.size(); i++) {
            Team team = teamList.get(startIndex + i);
            gui.setItem(TEAM_SLOTS[i], createTeamItem(team));
        }

        gui.setItem(PREV_SLOT, createNavItem("Previous", Material.ARROW, page > 0));
        gui.setItem(NEXT_SLOT, createNavItem("Next", Material.ARROW, page < totalPages - 1));
        gui.setItem(CLOSE_SLOT, createCloseItem());

        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, createFillerItem());
            }
        }

        player.openInventory(gui);
    }

    public void openTeamDetailGui(Player player, String teamId) {
        Team team = teamManager.getTeamById(teamId).orElse(null);
        if (team == null) {
            player.sendMessage(Component.text("Team not found!", NamedTextColor.RED));
            return;
        }

        playerViewingTeam.put(player.getUniqueId(), teamId);
        updateTeamDetailGui(player, team);
    }

    public void updateTeamDetailGui(Player player, Team team) {
        String title = "Team: " + team.displayName();
        Inventory gui = Bukkit.createInventory(null, 54, Component.text(title, team.getTextColor()));

        gui.setItem(4, createTeamInfoItem(team));
        gui.setItem(20, createActionItem("Set Color", Material.ENDER_EYE, "click to change color"));
        gui.setItem(22, createActionItem("Toggle Glow", Material.GLOWSTONE_DUST, "click to toggle glow"));
        gui.setItem(24, createActionItem("Set Max Size", Material.PAPER, "click to set max size"));
        gui.setItem(38, createActionItem("Set AutoKit", Material.NAME_TAG, "click to set kit"));
        gui.setItem(42, createDeleteItem(team.id()));

        int memberSlot = 10;
        for (UUID memberId : team.members()) {
            if (memberSlot > 43) break;
            if (memberSlot % 9 == 8) memberSlot += 2;
            String memberName = Bukkit.getOfflinePlayer(memberId).getName();
            gui.setItem(memberSlot++, createMemberItem(memberId, memberName));
        }

        gui.setItem(49, createBackItem());
        gui.setItem(53, createCloseItem());

        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, createFillerItem());
            }
        }

        player.openInventory(gui);
    }

    public void handleClick(Player player, int slot, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;

        String viewingTeamId = playerViewingTeam.get(player.getUniqueId());

        if (viewingTeamId != null) {
            handleTeamDetailClick(player, slot, item, viewingTeamId);
        } else {
            handleMainGuiClick(player, slot, item);
        }
    }

    private void handleMainGuiClick(Player player, int slot, ItemStack item) {
        if (isTeamSlot(slot)) {
            String teamId = getTeamIdFromItem(item);
            if (teamId != null) {
                openTeamDetailGui(player, teamId);
            }
        } else if (slot == PREV_SLOT && isNavEnabled(item)) {
            int page = playerPages.getOrDefault(player.getUniqueId(), 0);
            playerPages.put(player.getUniqueId(), page - 1);
            updateMainGui(player);
        } else if (slot == NEXT_SLOT && isNavEnabled(item)) {
            int page = playerPages.getOrDefault(player.getUniqueId(), 0);
            playerPages.put(player.getUniqueId(), page + 1);
            updateMainGui(player);
        } else if (slot == CLOSE_SLOT) {
            player.closeInventory();
        }
    }

    private void handleTeamDetailClick(Player player, int slot, ItemStack item, String teamId) {
        String lore = item.getItemMeta().lore().toString();

        if (slot == 49) {
            playerViewingTeam.remove(player.getUniqueId());
            openMainGui(player);
        } else if (slot == CLOSE_SLOT) {
            player.closeInventory();
        } else if (slot == 42 && lore.contains("delete")) {
            if (teamManager.deleteTeam(teamId)) {
                player.sendMessage(Component.text("Team deleted!", NamedTextColor.GREEN));
                playerViewingTeam.remove(player.getUniqueId());
                openMainGui(player);
            }
        } else if (slot == 53 && lore.contains("close")) {
            player.closeInventory();
        }
    }

    private boolean isTeamSlot(int slot) {
        for (int teamSlot : TEAM_SLOTS) {
            if (teamSlot == slot) return true;
        }
        return false;
    }

    private String getTeamIdFromItem(ItemStack item) {
        if (!item.hasItemMeta()) return null;
        List<Component> lore = item.getItemMeta().lore();
        if (lore == null || lore.isEmpty()) return null;

        String loreStr = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(lore.get(lore.size() - 1));
        return loreStr.replace("ID: ", "");
    }

    private boolean isNavEnabled(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        List<Component> lore = item.getItemMeta().lore();
        if (lore == null || lore.isEmpty()) return false;

        String loreStr = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(lore.get(0));
        return !loreStr.contains("DISABLED");
    }

    private ItemStack createTeamItem(Team team) {
        ItemStack item = new ItemStack(getTeamColorMaterial(team.color()));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(team.displayName(), team.getTextColor()));
        meta.lore(List.of(
            Component.text("Members: " + team.members().size() +
                (team.maxSize() > 0 ? "/" + team.maxSize() : ""), NamedTextColor.GRAY),
            Component.text("Color: " + team.color(), NamedTextColor.GRAY),
            Component.text("Glow: " + (team.glowEnabled() ? "ON" : "OFF"), NamedTextColor.GRAY),
            Component.text("ID: " + team.id(), NamedTextColor.DARK_GRAY)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTeamInfoItem(Team team) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Team Info", NamedTextColor.GOLD));
        meta.lore(List.of(
            Component.text("Name: " + team.displayName(), NamedTextColor.WHITE),
            Component.text("Members: " + team.members().size(), NamedTextColor.GRAY),
            Component.text("Color: " + team.color(), NamedTextColor.GRAY),
            Component.text("Glow: " + (team.glowEnabled() ? "ON" : "OFF"), NamedTextColor.GRAY),
            Component.text("Max Size: " + (team.maxSize() < 0 ? "Unlimited" : team.maxSize()), NamedTextColor.GRAY),
            Component.text("AutoKit: " + (team.autoKit() != null ? team.autoKit() : "None"), NamedTextColor.GRAY)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createActionItem(String name, Material material, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.AQUA));
        meta.lore(List.of(Component.text("Click to " + action, NamedTextColor.GRAY)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMemberItem(UUID uuid, String name) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.WHITE));
        meta.lore(List.of(
            Component.text("UUID: " + uuid.toString(), NamedTextColor.DARK_GRAY),
            Component.text("Click to kick", NamedTextColor.RED)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDeleteItem(String teamId) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Delete Team", NamedTextColor.RED));
        meta.lore(List.of(
            Component.text("Click to delete", NamedTextColor.GRAY),
            Component.text("WARNING: Cannot be undone!", NamedTextColor.DARK_RED)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Back to List", NamedTextColor.YELLOW));
        meta.lore(List.of(Component.text("Click to go back", NamedTextColor.GRAY)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Close", NamedTextColor.RED));
        meta.lore(List.of(Component.text("Click to close", NamedTextColor.GRAY)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNavItem(String name, Material material, boolean enabled) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        meta.lore(List.of(Component.text(enabled ? "Click to go" : "DISABLED", NamedTextColor.DARK_GRAY)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFillerItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }

    private Material getTeamColorMaterial(String color) {
        return switch (color.toLowerCase()) {
            case "red" -> Material.RED_WOOL;
            case "blue" -> Material.BLUE_WOOL;
            case "green" -> Material.GREEN_WOOL;
            case "yellow" -> Material.YELLOW_WOOL;
            case "purple" -> Material.PURPLE_WOOL;
            case "orange" -> Material.ORANGE_WOOL;
            case "pink" -> Material.PINK_WOOL;
            case "cyan" -> Material.CYAN_WOOL;
            default -> Material.WHITE_WOOL;
        };
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!playerPages.containsKey(player.getUniqueId()) && !playerViewingTeam.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        handleClick(player, event.getSlot(), item);
    }
}
