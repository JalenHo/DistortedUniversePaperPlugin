package dev.distorteduniverse.fakeplayer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class MovementCollision {
    private static final double MAX_STEP_UP = 1.0D;
    private static final double MAX_STEP_DOWN = 3.0D;

    private MovementCollision() {
    }

    public static Location resolveStep(Location from, double toX, double toZ, float yaw) {
        World world = from.getWorld();
        if (world == null) {
            return null;
        }

        double targetY = findStandableY(world, toX, toZ, from.getY());
        if (Double.isNaN(targetY)) {
            return null;
        }

        double deltaY = targetY - from.getY();
        if (deltaY > MAX_STEP_UP + 0.05D || deltaY < -MAX_STEP_DOWN) {
            return null;
        }

        if (!hasHeadroom(world, toX, targetY, toZ)) {
            return null;
        }

        if (!isHorizontalPathClear(world, from.getX(), from.getY(), from.getZ(), toX, toZ)) {
            return null;
        }

        return new Location(world, toX, targetY, toZ, yaw, from.getPitch());
    }

    public static double findStandableY(World world, double x, double z, double currentY) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int currentFeet = (int) Math.floor(currentY);

        for (int feetY = currentFeet + 1; feetY >= currentFeet - 1; feetY--) {
            if (canStandAt(world, blockX, feetY, blockZ)) {
                return feetY;
            }
        }

        int groundY = world.getHighestBlockYAt(blockX, blockZ);
        int feetY = groundY + 1;
        if (canStandAt(world, blockX, feetY, blockZ)) {
            double standY = feetY;
            if (Math.abs(standY - currentY) <= MAX_STEP_UP + 0.05D) {
                return standY;
            }
        }

        return Double.NaN;
    }

    private static boolean canStandAt(World world, int blockX, int feetY, int blockZ) {
        if (!isSolidGround(world.getBlockAt(blockX, feetY - 1, blockZ))) {
            return false;
        }
        return isPassable(world.getBlockAt(blockX, feetY, blockZ))
            && isPassable(world.getBlockAt(blockX, feetY + 1, blockZ));
    }

    private static boolean hasHeadroom(World world, double x, double y, double z) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int feetY = (int) Math.floor(y);
        return isPassable(world.getBlockAt(blockX, feetY, blockZ))
            && isPassable(world.getBlockAt(blockX, feetY + 1, blockZ));
    }

    private static boolean isHorizontalPathClear(
        World world,
        double fromX,
        double fromY,
        double fromZ,
        double toX,
        double toZ
    ) {
        int fromBlockX = (int) Math.floor(fromX);
        int fromBlockZ = (int) Math.floor(fromZ);
        int toBlockX = (int) Math.floor(toX);
        int toBlockZ = (int) Math.floor(toZ);

        if (fromBlockX == toBlockX && fromBlockZ == toBlockZ) {
            return true;
        }

        int feetY = (int) Math.floor(fromY);
        int headY = feetY + 1;

        int stepX = Integer.compare(toBlockX, fromBlockX);
        int stepZ = Integer.compare(toBlockZ, fromBlockZ);
        int x = fromBlockX;
        int z = fromBlockZ;

        while (x != toBlockX || z != toBlockZ) {
            if (x != toBlockX) {
                x += stepX;
            } else if (z != toBlockZ) {
                z += stepZ;
            }

            if (!isPassable(world.getBlockAt(x, feetY, z)) || !isPassable(world.getBlockAt(x, headY, z))) {
                return false;
            }
        }

        return true;
    }

    private static boolean isPassable(Block block) {
        Material material = block.getType();
        return !material.isSolid() && material != Material.LAVA && material != Material.FIRE;
    }

    private static boolean isSolidGround(Block block) {
        Material material = block.getType();
        return material.isSolid() && material != Material.LAVA;
    }
}
