package dev.distorteduniverse.fakeplayer.nms;

import com.mojang.authlib.GameProfile;
import dev.distorteduniverse.fakeplayer.SkinProperty;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class NmsFakePlayerSpawner {
    private final JavaPlugin plugin;
    private final Logger logger;
    private volatile boolean available = true;

    public NmsFakePlayerSpawner(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public boolean isAvailable() {
        return available;
    }

    public Player spawn(
        java.util.UUID uuid,
        String name,
        SkinProperty skin,
        Location location
    ) {
        if (!available) {
            return null;
        }

        World world = location.getWorld();
        if (world == null) {
            return null;
        }

        try {
            MinecraftServer minecraftServer = ((CraftServer) Bukkit.getServer()).getServer();
            ServerLevel serverLevel = ((CraftWorld) world).getHandle();
            GameProfile profile = GameProfileFactory.create(uuid, name, skin);
            ClientInformation clientInformation = ClientInformation.createDefault();
            ServerPlayer serverPlayer = new ServerPlayer(minecraftServer, serverLevel, profile, clientInformation);
            serverPlayer.setPos(location.getX(), location.getY(), location.getZ());
            serverPlayer.setYRot(location.getYaw());
            serverPlayer.setXRot(location.getPitch());

            FakeConnection connection = new FakeConnection(InetAddress.getLoopbackAddress());
            CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
            PlayerList playerList = minecraftServer.getPlayerList();
            playerList.placeNewPlayer(connection, serverPlayer, cookie);

            Player player = serverPlayer.getBukkitEntity();
            player.teleport(location);
            player.setGameMode(GameMode.SURVIVAL);
            return player;
        } catch (Throwable throwable) {
            logger.log(Level.WARNING, "Failed to spawn NMS fake player " + name, throwable);
            available = false;
            return null;
        }
    }

    public void remove(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        try {
            ServerPlayer serverPlayer = ((org.bukkit.craftbukkit.entity.CraftPlayer) player).getHandle();
            MinecraftServer minecraftServer = ((CraftServer) Bukkit.getServer()).getServer();
            minecraftServer.getPlayerList().remove(serverPlayer);
        } catch (Throwable throwable) {
            logger.log(Level.WARNING, "Failed to remove NMS fake player " + player.getName() + ", using kick fallback", throwable);
            if (player.isOnline()) {
                player.kick();
            }
        }
    }
}
