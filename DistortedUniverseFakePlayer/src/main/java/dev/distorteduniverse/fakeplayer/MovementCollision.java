package dev.distorteduniverse.fakeplayer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;

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

        // Prefer floating on the water surface when currently in fluid.
        if (isInFluid(from)) {
            Location waterStep = resolveWaterSurfaceStep(from, toX, toZ, yaw);
            if (waterStep != null) {
                return waterStep;
            }
        }

        double targetY = findStandableY(world, toX, toZ, from.getY());
        if (Double.isNaN(targetY)) {
            // Fall back to water-surface walking when no solid ground is nearby.
            Location waterStep = resolveWaterSurfaceStep(from, toX, toZ, yaw);
            if (waterStep != null) {
                return waterStep;
            }
            return null;
        }

        double deltaY = targetY - from.getY();
        if (deltaY > MAX_STEP_UP + 0.05D || deltaY < -MAX_STEP_DOWN) {
            return null;
        }

        if (!hasHeadroom(world, toX, targetY, toZ)) {
            return null;
        }

        if (!isHorizontalPathClear(world, from.getX(), from.getY(), from.getZ(), toX, targetY, toZ)) {
            return null;
        }

        return new Location(world, toX, targetY, toZ, yaw, from.getPitch());
    }

    public static Location resolveWaterSurfaceStep(Location from, double toX, double toZ, float yaw) {
        World world = from.getWorld();
        if (world == null) {
            return null;
        }

        Double surfaceY = findWaterSurfaceY(world, toX, toZ, from.getY());
        if (surfaceY == null) {
            return null;
        }

        double deltaY = surfaceY - from.getY();
        if (deltaY > MAX_STEP_UP + 0.5D || deltaY < -MAX_STEP_DOWN) {
            return null;
        }

        if (!hasHeadroom(world, toX, surfaceY, toZ)) {
            return null;
        }

        return new Location(world, toX, surfaceY, toZ, yaw, from.getPitch());
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

    /**
     * Returns the Y where an entity should float on the water surface
     * (feet roughly at the top of the water column), or null if no water.
     */
    public static Double findWaterSurfaceY(World world, double x, double z, double preferNearY) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int preferY = (int) Math.floor(preferNearY);

        Integer surfaceBlockY = null;
        for (int y = preferY + 4; y >= preferY - 8; y--) {
            Block block = world.getBlockAt(blockX, y, blockZ);
            if (!isFluid(block)) {
                continue;
            }
            Block above = world.getBlockAt(blockX, y + 1, blockZ);
            if (!isFluid(above) && isPassable(above)) {
                surfaceBlockY = y;
                break;
            }
        }

        if (surfaceBlockY == null) {
            // Scan from high to low around highest block for open water.
            int high = world.getHighestBlockYAt(blockX, blockZ) + 2;
            for (int y = high; y >= high - 16; y--) {
                Block block = world.getBlockAt(blockX, y, blockZ);
                if (!isFluid(block)) {
                    continue;
                }
                Block above = world.getBlockAt(blockX, y + 1, blockZ);
                if (!isFluid(above) && isPassable(above)) {
                    surfaceBlockY = y;
                    break;
                }
            }
        }

        if (surfaceBlockY == null) {
            return null;
        }

        // Float near the top of the surface water block, similar to villagers/pigs.
        return surfaceBlockY + 0.85D;
    }

    public static boolean isInFluid(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        Block feet = world.getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        Block below = world.getBlockAt(location.getBlockX(), location.getBlockY() - 1, location.getBlockZ());
        return isFluid(feet) || isFluid(below);
    }

    public static boolean isFluid(Block block) {
        Material type = block.getType();
        if (type == Material.WATER || type == Material.BUBBLE_COLUMN) {
            return true;
        }
        return block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged();
    }

    private static boolean canStandAt(World world, int blockX, int feetY, int blockZ) {
        Block ground = world.getBlockAt(blockX, feetY - 1, blockZ);
        if (!isSolidGround(ground)) {
            return false;
        }
        // Do not treat underwater seafloor as preferred standable ground for wandering.
        Block feet = world.getBlockAt(blockX, feetY, blockZ);
        if (isFluid(feet)) {
            return false;
        }
        return isPassable(feet) && isPassable(world.getBlockAt(blockX, feetY + 1, blockZ));
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
        double toY,
        double toZ
    ) {
        int fromBlockX = (int) Math.floor(fromX);
        int fromBlockZ = (int) Math.floor(fromZ);
        int toBlockX = (int) Math.floor(toX);
        int toBlockZ = (int) Math.floor(toZ);

        if (fromBlockX == toBlockX && fromBlockZ == toBlockZ) {
            return true;
        }

        int feetY = (int) Math.floor(Math.max(fromY, toY));
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
