package dev.upiscium.frontierprotocol.tier1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class Tier1SuppressionSourceTest {
    @Test
    void sourceIdIsStableAndIncludesNegativeCoordinates() {
        var first = Tier1SuppressionSource.at(new BlockPos(-17, 64, -33));
        var second = Tier1SuppressionSource.at(new BlockPos(-17, 64, -33));

        assertEquals(first.id(), second.id());
        assertEquals("frontier_protocol:tier_1/-17_64_-33", first.id().value().toString());
    }

    @Test
    void rpmRequirementUsesAbsoluteSpeed() {
        assertTrue(Tier1StabilizerBlockEntity.isRpmSufficient(32.0F, 32));
        assertTrue(Tier1StabilizerBlockEntity.isRpmSufficient(-32.0F, 32));
        assertFalse(Tier1StabilizerBlockEntity.isRpmSufficient(31.99F, 32));
        assertFalse(Tier1StabilizerBlockEntity.isRpmSufficient(-31.99F, 32));
    }
}
