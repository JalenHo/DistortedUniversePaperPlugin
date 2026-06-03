package dev.distorteduniverse.playerevent;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class RecipientSelector {
    public List<Player> nearbyPlayers(Location origin, double radius, Player subject, boolean includeSubject) {
        if (origin.getWorld() == null) {
            return List.of();
        }

        double radiusSquared = radius * radius;
        List<Player> recipients = new ArrayList<>();
        for (Player candidate : origin.getWorld().getPlayers()) {
            if (!includeSubject && subject != null && candidate.getUniqueId().equals(subject.getUniqueId())) {
                continue;
            }
            if (subject != null && !candidate.getUniqueId().equals(subject.getUniqueId()) && !candidate.canSee(subject)) {
                continue;
            }
            if (candidate.getLocation().distanceSquared(origin) <= radiusSquared) {
                recipients.add(candidate);
            }
        }
        return recipients;
    }
}
