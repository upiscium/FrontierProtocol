package dev.upiscium.frontierprotocol.resource;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.protection.ServerProtectionService;
import dev.upiscium.frontierprotocol.sector.SectorPos;
import dev.upiscium.frontierprotocol.sector.SectorServices;
import dev.upiscium.frontierprotocol.world.FrontierProtocolWorldData;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public final class ResourceNodeWorkService {
    public static final ResourceLocation WRONG_TRAIT = id("wrong_trait");
    public static final ResourceLocation PROTECTION_REQUIRED = id("protection_required");

    private ResourceNodeWorkService() {}

    public static Optional<ResourceLocation> currentTrait(ServerLevel level, BlockPos pos) {
        if (level != level.getServer().overworld()) return Optional.empty();
        FrontierProtocolWorldData data = FrontierProtocolWorldData.get(level);
        SectorPos sector = SectorPos.fromChunk(new ChunkPos(pos), data.sectorSizeChunks());
        return SectorServices.PLACEMENT.resolve(level.getSeed(), sector, data.originSector(), data.forcedTraitOverrides());
    }

    public static WorkResult evaluate(ServerLevel level, BlockPos pos, ResourceNodeDefinition definition) {
        if (currentTrait(level, pos).filter(definition.requiredTrait()::equals).isEmpty()) {
            return WorkResult.blocked(WRONG_TRAIT);
        }
        if (definition.requiresProtection() && !ServerProtectionService.INSTANCE.isBlockProtected(level, pos)) {
            return WorkResult.blocked(PROTECTION_REQUIRED);
        }
        return WorkResult.allowed();
    }

    public static WorkRequirement requirement(ResourceNodeDefinition definition) {
        return (level, pos) -> evaluate(level, pos, definition);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, path);
    }
}
