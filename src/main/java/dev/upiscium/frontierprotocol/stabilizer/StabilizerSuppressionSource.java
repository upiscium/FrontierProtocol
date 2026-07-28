package dev.upiscium.frontierprotocol.stabilizer;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSource;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceId;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

final class StabilizerSuppressionSource {
    private StabilizerSuppressionSource() {}

    static SuppressionSource at(StabilizerTier tier, BlockPos pos) {
        String path = "stabilizer/" + tier.serializedName() + "/"
                + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
        return new SuppressionSource(
                new SuppressionSourceId(ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, path)),
                SuppressionSourceType.STABILIZER);
    }
}
