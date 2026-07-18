package dev.upiscium.frontierprotocol.sector;

import dev.upiscium.frontierprotocol.data.TraitReloadListener;

public final class SectorServices {
    public static final SectorPlacementService PLACEMENT = new SectorPlacementService(
            TraitReloadListener::definitions, TraitReloadListener::revision);

    private SectorServices() {}
}
