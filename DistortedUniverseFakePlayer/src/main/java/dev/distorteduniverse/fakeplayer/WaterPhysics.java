package dev.distorteduniverse.fakeplayer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;

import java.util.OptionalDouble;

final class WaterPhysics {
    private static final double FLOAT_SURFACE_OFFSET = 0.10;

    private WaterPhysics() {
    }

    static OptionalDouble floatingY(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return OptionalDouble.empty();
        }
        return floatingY(world, location.getX(), location.getZ(), location.getY());
    }

    static OptionalDouble floatingY(World world, double x, double z, double referenceY) {
        OptionalDouble surfaceY = waterSurfaceY(world, x, z, referenceY);
        if (surfaceY.isEmpty()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(surfaceY.getAsDouble() - FLOAT_SURFACE_OFFSET);
    }

    static double walkingY(World world, double x, double z, double currentY) {
        OptionalDouble waterY = floatingY(world, x, z, currentY);
        if (waterY.isPresent()) {
            return waterY.getAsDouble();
        }

        int groundY = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z));
        double snapped = groundY + 1.0;
        if (snapped < world.getMinHeight()) {
            return currentY;
        }
        return snapped;
    }

    private static OptionalDouble waterSurfaceY(World world, double x, double z, double referenceY) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int centerY = (int) Math.floor(referenceY);
        int minY = Math.max(world.getMinHeight(), centerY - 6);
        int maxY = Math.min(world.getMaxHeight() - 1, centerY + 3);

        for (int y = maxY; y >= minY; y--) {
            if (!isWater(world.getBlockAt(blockX, y, blockZ))) {
                continue;
            }

            int surfaceBlockY = y;
            while (surfaceBlockY + 1 < world.getMaxHeight()
                && isWater(world.getBlockAt(blockX, surfaceBlockY + 1, blockZ))) {
                surfaceBlockY++;
            }
            if (!hasClearColumnToReference(world, blockX, blockZ, surfaceBlockY, centerY)) {
                return OptionalDouble.empty();
            }
            return OptionalDouble.of(surfaceBlockY + 1.0);
        }

        return OptionalDouble.empty();
    }

    private static boolean hasClearColumnToReference(World world, int x, int z, int surfaceBlockY, int referenceY) {
        int fromY = Math.max(world.getMinHeight(), surfaceBlockY + 1);
        int toY = Math.min(world.getMaxHeight() - 1, referenceY);
        for (int y = fromY; y <= toY; y++) {
            Block block = world.getBlockAt(x, y, z);
            if (!isWater(block) && !block.isPassable()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWater(Block block) {
        if (block.getType() == Material.WATER) {
            return true;
        }
        return block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged();
    }
}
