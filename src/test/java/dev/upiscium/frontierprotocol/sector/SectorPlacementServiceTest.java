package dev.upiscium.frontierprotocol.sector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class SectorPlacementServiceTest {
    private static final ResourceLocation LOW = ResourceLocation.fromNamespaceAndPath("frontier_protocol", "low");
    private static final ResourceLocation HIGH = ResourceLocation.fromNamespaceAndPath("frontier_protocol", "high");

    @Test
    void resolutionIsDeterministicForSeedAndCoordinate() {
        List<SectorTraitDefinition> definitions = List.of(definition(LOW, 10, 7, 0.6, 3));
        SectorPlacementService first = service(definitions);
        SectorPlacementService second = service(definitions);
        for (int x = -50; x <= 50; x++) {
            SectorPos pos = new SectorPos(x, x / 2);
            assertEquals(first.resolve(1234L, pos, Map.of()), second.resolve(1234L, pos, Map.of()));
        }
    }

    @Test
    void priorityAndIdResolveConflictsAndOverridesWin() {
        SectorTraitDefinition low = definition(LOW, 10, 1, 1.0, 1);
        SectorTraitDefinition high = definition(HIGH, 20, 1, 1.0, 1);
        SectorPlacementService service = service(List.of(low, high));
        SectorPos pos = new SectorPos(20, 20);

        assertEquals(Optional.of(HIGH), service.resolve(1L, pos, Map.of()));
        assertEquals(Optional.of(LOW), service.resolve(1L, pos, Map.of(pos, LOW)));

        SectorTraitDefinition lexicalHigh = definition(HIGH, 10, 1, 1.0, 1);
        assertEquals(Optional.of(HIGH), service(List.of(low, lexicalHigh)).resolve(1L, pos, Map.of()));
    }

    @Test
    void differentSeedsChangeAStatisticalSample() {
        SectorPlacementService service = service(List.of(definition(LOW, 10, 7, 0.5, 2)));
        long first = countMatches(service, 11L);
        long second = countMatches(service, 12L);
        assertNotEquals(first, second);
    }

    @Test
    void shippedStylePlacementLeavesMostSectorsNormal() {
        SectorPlacementService service = service(List.of(
                definition(LOW, 100, 10, 0.35, 2),
                definition(HIGH, 90, 8, 0.45, 2)));
        long occupied = 0;
        int total = 201 * 201;
        for (int x = -100; x <= 100; x++) {
            for (int z = -100; z <= 100; z++) {
                if (service.resolve(987654321L, new SectorPos(x, z), Map.of()).isPresent()) {
                    occupied++;
                }
            }
        }
        assertTrue(occupied > total * 0.005 && occupied < total * 0.3, "occupied=" + occupied + "/" + total);
    }

    private static long countMatches(SectorPlacementService service, long seed) {
        long signature = 0;
        for (int x = -30; x <= 30; x++) {
            for (int z = -30; z <= 30; z++) {
                if (service.resolve(seed, new SectorPos(x, z), Map.of()).isPresent()) {
                    signature = signature * 31 + x * 17L + z;
                }
            }
        }
        return signature;
    }

    private static SectorPlacementService service(List<SectorTraitDefinition> definitions) {
        return new SectorPlacementService(() -> definitions);
    }

    private static SectorTraitDefinition definition(
            ResourceLocation id, int priority, int spacing, double chance, int clusterMax) {
        return new SectorTraitDefinition(id, priority,
                new SectorTraitDefinition.Placement(spacing, 0, chance, 0, Optional.empty(), 1, clusterMax, 918273L),
                new SectorTraitDefinition.Guarantee(false, 0, 0, 0), List.of());
    }
}
