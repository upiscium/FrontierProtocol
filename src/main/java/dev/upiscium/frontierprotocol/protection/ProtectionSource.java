package dev.upiscium.frontierprotocol.protection;

import java.util.Optional;
import net.minecraft.core.BlockPos;

public record ProtectionSource(Type type, Optional<BlockPos> blockPos) {
    public enum Type {
        INITIAL_SPAWN,
        STABILIZATION_BEACON
    }

    public static ProtectionSource initialSpawn() {
        return new ProtectionSource(Type.INITIAL_SPAWN, Optional.empty());
    }

    public static ProtectionSource beacon(BlockPos pos) {
        return new ProtectionSource(Type.STABILIZATION_BEACON, Optional.of(pos.immutable()));
    }
}
