package dev.upiscium.frontierprotocol.sector;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.world.FrontierProtocolWorldData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

public final class SectorGuaranteeService {
    private SectorGuaranteeService() {}

    public static void initialize(ServerLevel level, FrontierProtocolWorldData data) {
        if (data.guaranteesInitialized()) return;
        SectorPos origin = data.originSector();
        for (SectorTraitDefinition definition : SectorServices.PLACEMENT.orderedDefinitions()) {
            SectorTraitDefinition.Guarantee guarantee = definition.guarantee();
            if (!guarantee.enabled() || guarantee.count() == 0) continue;
            List<SectorPos> ring = ring(origin, guarantee.ringMin(), guarantee.ringMax());
            long existing = ring.stream().filter(pos -> !overlapsInitialProtection(pos, data)).filter(pos -> SectorServices.PLACEMENT
                    .resolve(level.getSeed(), pos, origin, data.forcedTraitOverrides()).filter(definition.id()::equals).isPresent()).count();
            int missing = Math.max(0, guarantee.count() - (int) existing);
            if (missing == 0) continue;
            List<SectorPos> candidates = ring.stream()
                    .filter(pos -> !data.forcedTraitOverrides().containsKey(pos))
                    .filter(pos -> SectorServices.PLACEMENT.resolve(level.getSeed(), pos, origin, data.forcedTraitOverrides()).isEmpty())
                    .filter(pos -> !overlapsInitialProtection(pos, data))
                    .sorted((first, second) -> {
                        long firstHash = SectorServices.PLACEMENT.stableOrderHash(
                                level.getSeed(), definition.id(), definition.placement().salt(), first);
                        long secondHash = SectorServices.PLACEMENT.stableOrderHash(
                                level.getSeed(), definition.id(), definition.placement().salt(), second);
                        int hashResult = Long.compareUnsigned(firstHash, secondHash);
                        return hashResult != 0 ? hashResult : first.compareTo(second);
                    })
                    .toList();
            if (candidates.size() < missing) {
                throw new IllegalStateException("Unable to guarantee " + definition.id() + ": need " + missing
                        + " normal sectors in ring but found " + candidates.size());
            }
            for (int index = 0; index < missing; index++) data.setOverride(candidates.get(index), definition.id());
            FrontierProtocolMod.LOGGER.info("Created {} guaranteed sector override(s) for {}", missing, definition.id());
        }
        data.markGuaranteesInitialized();
    }

    private static List<SectorPos> ring(SectorPos origin, int min, int max) {
        List<SectorPos> result = new ArrayList<>();
        for (long dx = -max; dx <= max; dx++) {
            for (long dz = -max; dz <= max; dz++) {
                long distance = Math.max(Math.abs(dx), Math.abs(dz));
                long x = (long) origin.x() + dx;
                long z = (long) origin.z() + dz;
                if (distance >= min && x >= Integer.MIN_VALUE && x <= Integer.MAX_VALUE
                        && z >= Integer.MIN_VALUE && z <= Integer.MAX_VALUE) {
                    result.add(new SectorPos((int) x, (int) z));
                }
            }
        }
        return result;
    }

    private static boolean overlapsInitialProtection(SectorPos sector, FrontierProtocolWorldData data) {
        long minX = (long) sector.x() * data.sectorSizeChunks();
        long maxX = minX + data.sectorSizeChunks() - 1L;
        long minZ = (long) sector.z() * data.sectorSizeChunks();
        long maxZ = minZ + data.sectorSizeChunks() - 1L;
        return minX <= (long) data.originChunkX() + 2 && maxX >= (long) data.originChunkX() - 2
                && minZ <= (long) data.originChunkZ() + 2 && maxZ >= (long) data.originChunkZ() - 2;
    }
}
