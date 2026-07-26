package dev.upiscium.frontierprotocol.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.ChunkPos;

public final class StabilizerRangeGeometry {
    private static final int CHUNK_SIZE = 16;

    private StabilizerRangeGeometry() {}

    public static RangeBounds from(
            ChunkPos center, int radius, int minBuildHeight, int maxBuildHeight) {
        if (center == null) throw new IllegalArgumentException("center must not be null");
        if (radius < 0) throw new IllegalArgumentException("radius must not be negative");
        if (maxBuildHeight <= minBuildHeight) {
            throw new IllegalArgumentException("maxBuildHeight must be above minBuildHeight");
        }
        long minChunkX = (long) center.x - radius;
        long maxChunkX = (long) center.x + radius;
        long minChunkZ = (long) center.z - radius;
        long maxChunkZ = (long) center.z + radius;
        return new RangeBounds(
                minChunkX,
                maxChunkX,
                minChunkZ,
                maxChunkZ,
                minChunkX * CHUNK_SIZE,
                (maxChunkX + 1L) * CHUNK_SIZE,
                minChunkZ * CHUNK_SIZE,
                (maxChunkZ + 1L) * CHUNK_SIZE,
                minBuildHeight,
                maxBuildHeight);
    }

    public record RangeBounds(
            long minChunkX,
            long maxChunkX,
            long minChunkZ,
            long maxChunkZ,
            long minBlockX,
            long maxBlockXExclusive,
            long minBlockZ,
            long maxBlockZExclusive,
            int minBuildHeight,
            int maxBuildHeight) {
        public int width() {
            return Math.toIntExact(maxChunkX - minChunkX + 1L);
        }

        public List<LineSegment> horizontalLines(double y) {
            List<LineSegment> lines = new ArrayList<>(4 + Math.max(0, width() - 1) * 2);
            lines.add(new LineSegment(minBlockX, y, minBlockZ, maxBlockXExclusive, y, minBlockZ, LineKind.OUTER));
            lines.add(new LineSegment(
                    minBlockX, y, maxBlockZExclusive, maxBlockXExclusive, y, maxBlockZExclusive, LineKind.OUTER));
            lines.add(new LineSegment(minBlockX, y, minBlockZ, minBlockX, y, maxBlockZExclusive, LineKind.OUTER));
            lines.add(new LineSegment(
                    maxBlockXExclusive, y, minBlockZ, maxBlockXExclusive, y, maxBlockZExclusive, LineKind.OUTER));
            for (int offset = 1; offset < width(); offset++) {
                long x = minBlockX + (long) offset * CHUNK_SIZE;
                long z = minBlockZ + (long) offset * CHUNK_SIZE;
                lines.add(new LineSegment(x, y, minBlockZ, x, y, maxBlockZExclusive, LineKind.INTERNAL));
                lines.add(new LineSegment(minBlockX, y, z, maxBlockXExclusive, y, z, LineKind.INTERNAL));
            }
            return List.copyOf(lines);
        }

        public List<LineSegment> verticalCorners(double minY, double maxY) {
            if (maxY <= minY) throw new IllegalArgumentException("maxY must be above minY");
            return List.of(
                    new LineSegment(minBlockX, minY, minBlockZ, minBlockX, maxY, minBlockZ, LineKind.VERTICAL),
                    new LineSegment(
                            maxBlockXExclusive,
                            minY,
                            minBlockZ,
                            maxBlockXExclusive,
                            maxY,
                            minBlockZ,
                            LineKind.VERTICAL),
                    new LineSegment(
                            minBlockX,
                            minY,
                            maxBlockZExclusive,
                            minBlockX,
                            maxY,
                            maxBlockZExclusive,
                            LineKind.VERTICAL),
                    new LineSegment(
                            maxBlockXExclusive,
                            minY,
                            maxBlockZExclusive,
                            maxBlockXExclusive,
                            maxY,
                            maxBlockZExclusive,
                            LineKind.VERTICAL));
        }
    }

    public record LineSegment(
            double startX,
            double startY,
            double startZ,
            double endX,
            double endY,
            double endZ,
            LineKind kind) {}

    public enum LineKind {
        OUTER,
        INTERNAL,
        VERTICAL
    }
}
