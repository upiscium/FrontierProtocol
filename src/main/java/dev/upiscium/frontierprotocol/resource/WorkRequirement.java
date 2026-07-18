package dev.upiscium.frontierprotocol.resource;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

@FunctionalInterface
public interface WorkRequirement {
    WorkResult evaluate(ServerLevel level, BlockPos pos);
}
