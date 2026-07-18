package dev.upiscium.frontierprotocol.suppression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.upiscium.frontierprotocol.api.suppression.SuppressionSource;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceId;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceType;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class DimensionSuppressionIndexTest {
    private static final SuppressionSource SOURCE_A = source("a");
    private static final SuppressionSource SOURCE_B = source("b");

    @Test
    void indexesNegativeChunksAndReturnsTheirSource() {
        DimensionSuppressionIndex index = new DimensionSuppressionIndex();
        ChunkPos target = new ChunkPos(-7, -11);

        index.registerOrUpdate(SOURCE_A, Set.of(target));

        assertTrue(index.isSuppressed(target));
        assertEquals(Set.of(SOURCE_A), index.getSources(target));
        assertFalse(index.isSuppressed(new ChunkPos(-6, -11)));
    }

    @Test
    void removingOneOverlappingSourceKeepsTheOtherActive() {
        DimensionSuppressionIndex index = new DimensionSuppressionIndex();
        ChunkPos target = new ChunkPos(4, -3);
        index.registerOrUpdate(SOURCE_A, Set.of(target));
        index.registerOrUpdate(SOURCE_B, Set.of(target));

        index.unregister(SOURCE_A.id());

        assertTrue(index.isSuppressed(target));
        assertEquals(Set.of(SOURCE_B), index.getSources(target));
        index.unregister(SOURCE_B.id());
        assertFalse(index.isSuppressed(target));
    }

    @Test
    void updatingSourceReplacesItsCoveredChunkSet() {
        DimensionSuppressionIndex index = new DimensionSuppressionIndex();
        ChunkPos oldChunk = new ChunkPos(1, 2);
        ChunkPos retainedChunk = new ChunkPos(2, 2);
        ChunkPos newChunk = new ChunkPos(3, 2);
        index.registerOrUpdate(SOURCE_A, Set.of(oldChunk, retainedChunk));

        index.registerOrUpdate(SOURCE_A, Set.of(retainedChunk, newChunk));

        assertFalse(index.isSuppressed(oldChunk));
        assertTrue(index.isSuppressed(retainedChunk));
        assertTrue(index.isSuppressed(newChunk));
    }

    private static SuppressionSource source(String path) {
        return new SuppressionSource(
                new SuppressionSourceId(ResourceLocation.fromNamespaceAndPath("frontier_protocol_test", path)),
                SuppressionSourceType.EXTERNAL);
    }
}
