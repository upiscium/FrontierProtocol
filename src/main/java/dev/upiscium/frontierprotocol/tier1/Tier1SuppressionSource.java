package dev.upiscium.frontierprotocol.tier1;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSource;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceId;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

final class Tier1SuppressionSource {
    private Tier1SuppressionSource() {}

    static SuppressionSource at(BlockPos pos) {
        String path = "tier_1/" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
        return new SuppressionSource(
                new SuppressionSourceId(ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, path)),
                SuppressionSourceType.TIER_1_STABILIZER);
    }
}
