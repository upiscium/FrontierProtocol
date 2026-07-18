package dev.upiscium.frontierprotocol.sector;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public final class SectorDiscoveredEvent extends Event {
    private final ServerPlayer player;
    private final SectorPos sector;
    private final ResourceLocation trait;

    public SectorDiscoveredEvent(ServerPlayer player, SectorPos sector, ResourceLocation trait) {
        this.player = player;
        this.sector = sector;
        this.trait = trait;
    }

    public ServerPlayer player() { return player; }
    public SectorPos sector() { return sector; }
    public ResourceLocation trait() { return trait; }
}
