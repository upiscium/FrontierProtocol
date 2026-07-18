package dev.upiscium.frontierprotocol.api.suppression;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Server-side queries for chunk-based infection suppression.
 *
 * <p>All methods must be called from the Minecraft server thread. Block-position queries ignore the Y coordinate
 * because suppression covers complete chunks.</p>
 */
public interface InfectionSuppressionService {
    /** Returns whether the chunk containing {@code pos} is suppressed, regardless of its Y coordinate. */
    boolean isSuppressed(ServerLevel level, BlockPos pos);

    /** Returns whether {@code chunkPos} is suppressed in the supplied dimension. */
    boolean isSuppressed(ServerLevel level, ChunkPos chunkPos);

    /** Returns an immutable snapshot of the sources suppressing {@code chunkPos}. */
    Set<SuppressionSource> getSources(ServerLevel level, ChunkPos chunkPos);
}
