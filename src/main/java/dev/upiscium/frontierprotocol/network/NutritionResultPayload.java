package dev.upiscium.frontierprotocol.network;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NutritionResultPayload(int protocolVersion, int efficiencyPercent, boolean repeated)
        implements CustomPacketPayload {
    public static final Type<NutritionResultPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, "nutrition_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NutritionResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, NutritionResultPayload::protocolVersion,
            ByteBufCodecs.INT, NutritionResultPayload::efficiencyPercent,
            ByteBufCodecs.BOOL, NutritionResultPayload::repeated,
            NutritionResultPayload::new);

    @Override
    public Type<NutritionResultPayload> type() {
        return TYPE;
    }
}
