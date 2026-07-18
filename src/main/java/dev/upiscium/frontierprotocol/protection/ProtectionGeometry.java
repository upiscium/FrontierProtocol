package dev.upiscium.frontierprotocol.protection;

import net.minecraft.world.level.ChunkPos;

public final class ProtectionGeometry {
    private ProtectionGeometry() {}

    public static boolean isWithinRadius(ChunkPos center, ChunkPos target, int radius) {
        return Math.abs((long) target.x - center.x) <= radius
                && Math.abs((long) target.z - center.z) <= radius;
    }
}
