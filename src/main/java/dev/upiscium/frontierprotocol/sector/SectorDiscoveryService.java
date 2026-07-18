package dev.upiscium.frontierprotocol.sector;

import dev.upiscium.frontierprotocol.world.FrontierProtocolWorldData;
import dev.upiscium.frontierprotocol.network.NetworkRegistration;
import dev.upiscium.frontierprotocol.network.SectorInfoPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SectorDiscoveryService {
    private SectorDiscoveryService() {}

    public static void discoverCurrentSector(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || level != level.getServer().overworld()) return;
        FrontierProtocolWorldData data = FrontierProtocolWorldData.get(level);
        SectorPos sector = SectorPos.fromChunk(new ChunkPos(player.blockPosition()), data.sectorSizeChunks());
        SectorServices.PLACEMENT.resolve(level.getSeed(), sector, data.originSector(), data.forcedTraitOverrides()).ifPresent(trait -> {
            if (!data.discover(sector)) return;
            ResourceLocation id = trait;
            PacketDistributor.sendToPlayer(player, new SectorInfoPayload(
                    NetworkRegistration.PROTOCOL_VERSION, sector.x(), sector.z(), id));
            NeoForge.EVENT_BUS.post(new SectorDiscoveredEvent(player, sector, id));
        });
    }
}
