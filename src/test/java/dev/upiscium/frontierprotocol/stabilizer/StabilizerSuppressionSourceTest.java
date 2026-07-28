package dev.upiscium.frontierprotocol.stabilizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class StabilizerSuppressionSourceTest {
    @Test
    void sourceIdIsStableAndIncludesNegativeCoordinates() {
        var first = StabilizerSuppressionSource.at(StabilizerTier.TIER_1, new BlockPos(-17, 64, -33));
        var second = StabilizerSuppressionSource.at(StabilizerTier.TIER_1, new BlockPos(-17, 64, -33));

        assertEquals(first.id(), second.id());
        assertEquals("frontier_protocol:stabilizer/tier_1/-17_64_-33", first.id().value().toString());
    }

    @Test
    void rpmRequirementUsesAbsoluteSpeed() {
        assertTrue(StabilizerBlockEntity.isRpmSufficient(32.0F, 32));
        assertTrue(StabilizerBlockEntity.isRpmSufficient(-32.0F, 32));
        assertFalse(StabilizerBlockEntity.isRpmSufficient(31.99F, 32));
        assertFalse(StabilizerBlockEntity.isRpmSufficient(-31.99F, 32));
    }
}
