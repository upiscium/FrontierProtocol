package dev.upiscium.frontierprotocol.network;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SectorInfoPayload(int protocolVersion, int sectorX, int sectorZ, ResourceLocation trait)
        implements CustomPacketPayload {
    public static final Type<SectorInfoPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, "sector_info"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SectorInfoPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SectorInfoPayload::protocolVersion,
            ByteBufCodecs.INT, SectorInfoPayload::sectorX,
            ByteBufCodecs.INT, SectorInfoPayload::sectorZ,
            ResourceLocation.STREAM_CODEC, SectorInfoPayload::trait,
            SectorInfoPayload::new);

    @Override
    public Type<SectorInfoPayload> type() {
        return TYPE;
    }
}
