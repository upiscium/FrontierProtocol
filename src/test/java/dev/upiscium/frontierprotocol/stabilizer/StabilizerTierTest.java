package dev.upiscium.frontierprotocol.stabilizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class StabilizerTierTest {
    @Test
    void hasStableSerializedNamesAndRegistryPrefixes() {
        assertEquals(
                List.of(StabilizerTier.TIER_1, StabilizerTier.TIER_2, StabilizerTier.TIER_3),
                List.of(StabilizerTier.values()));
        assertEquals("tier_1", StabilizerTier.TIER_1.serializedName());
        assertEquals("tier_2", StabilizerTier.TIER_2.serializedName());
        assertEquals("tier_3", StabilizerTier.TIER_3.serializedName());
        assertEquals("tier_1", StabilizerTier.TIER_1.registryPrefix());
        assertEquals("tier_2", StabilizerTier.TIER_2.registryPrefix());
        assertEquals("tier_3", StabilizerTier.TIER_3.registryPrefix());
    }

    @Test
    void resolvesCurrentAndPlannedRegistryPathsWithoutRegistrations() {
        assertEquals(StabilizerTier.TIER_1, StabilizerTier.fromRegistryPath("tier_1_stabilizer"));
        assertEquals(StabilizerTier.TIER_2, StabilizerTier.fromRegistryPath("tier_2_stabilizer"));
        assertEquals(StabilizerTier.TIER_3, StabilizerTier.fromRegistryPath("tier_3_stabilizer"));
        assertThrows(IllegalArgumentException.class, () -> StabilizerTier.fromRegistryPath("other_block"));
    }
}
