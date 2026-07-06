package dev.distorteduniverse.team;

import net.kyori.adventure.text.format.TextColor;

import java.util.List;
import java.util.UUID;

public record Team(
    String id,
    String displayName,
    String color,
    int maxSize,
    boolean glowEnabled,
    String glowColor,
    String autoKit,
    List<UUID> members
) {
    public Team withDisplayName(String newDisplayName) {
        return new Team(id, newDisplayName, color, maxSize, glowEnabled, glowColor, autoKit, members);
    }

    public Team withColor(String newColor) {
        return new Team(id, displayName, newColor, maxSize, glowEnabled, glowColor, autoKit, members);
    }

    public Team withMaxSize(int newMaxSize) {
        return new Team(id, displayName, color, newMaxSize, glowEnabled, glowColor, autoKit, members);
    }

    public Team withGlowEnabled(boolean enabled) {
        return new Team(id, displayName, color, maxSize, enabled, glowColor, autoKit, members);
    }

    public Team withGlowColor(String newGlowColor) {
        return new Team(id, displayName, color, maxSize, glowEnabled, newGlowColor, autoKit, members);
    }

    public Team withAutoKit(String newAutoKit) {
        return new Team(id, displayName, color, maxSize, glowEnabled, glowColor, newAutoKit, members);
    }

    public Team withMembers(List<UUID> newMembers) {
        return new Team(id, displayName, color, maxSize, glowEnabled, glowColor, autoKit, newMembers);
    }

    public Team addMember(UUID member) {
        if (members.contains(member)) {
            return this;
        }
        return withMembers(new java.util.ArrayList<>(members));
    }

    public Team removeMember(UUID member) {
        List<UUID> newMembers = new java.util.ArrayList<>(members);
        newMembers.remove(member);
        return withMembers(newMembers);
    }

    public boolean hasMember(UUID member) {
        return members.contains(member);
    }

    public boolean isFull() {
        return maxSize > 0 && members.size() >= maxSize;
    }

    public int availableSlots() {
        if (maxSize < 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, maxSize - members.size());
    }

    public TextColor getTextColor() {
        try {
            return TextColor.fromHexString("#" + getHexColor());
        } catch (Exception e) {
            return TextColor.fromCSSHexString("WHITE");
        }
    }

    private String getHexColor() {
        return switch (color.toLowerCase()) {
            case "black" -> "000000";
            case "dark_blue" -> "0000AA";
            case "dark_green" -> "00AA00";
            case "dark_aqua" -> "00AAAA";
            case "dark_red" -> "AA0000";
            case "dark_purple" -> "AA00AA";
            case "gold" -> "FFAA00";
            case "gray" -> "555555";
            case "dark_gray" -> "333333";
            case "blue" -> "5555FF";
            case "green" -> "55FF55";
            case "aqua" -> "55FFFF";
            case "red" -> "FF5555";
            case "light_purple" -> "FF55FF";
            case "yellow" -> "FFFF55";
            case "white" -> "FFFFFF";
            default -> "FFFFFF";
        };
    }
}
