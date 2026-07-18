package dev.upiscium.frontierprotocol.breach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

class BreachRulesTest {
    @Test
    void hardnessBoundaryIsInclusiveAndRejectsUnbreakableBlocks() {
        assertTrue(BreachRules.hardnessAllowed(2.0F, 2.0));
        assertFalse(BreachRules.hardnessAllowed(-1.0F, 2.0));
        assertFalse(BreachRules.hardnessAllowed(2.01F, 2.0));
    }

    @Test
    void durationScalesWithHardnessAndMultiplier() {
        assertEquals(20, BreachRules.breakDurationTicks(0.0F, 1.0));
        assertEquals(100, BreachRules.breakDurationTicks(2.0F, 1.0));
        assertEquals(200, BreachRules.breakDurationTicks(2.0F, 2.0));
        assertThrows(IllegalArgumentException.class, () -> BreachRules.breakDurationTicks(-1.0F, 1.0));
    }

    @Test
    void progressNeverShowsCompletionBeforeDestruction() {
        assertEquals(0, BreachRules.progressStage(0, 100));
        assertEquals(5, BreachRules.progressStage(50, 100));
        assertEquals(9, BreachRules.progressStage(100, 100));
    }

    @Test
    void blockEntityStatesAreRejected() {
        assertTrue(BreachRules.hasNoBlockEntity(Blocks.OAK_PLANKS.defaultBlockState()));
        assertFalse(BreachRules.hasNoBlockEntity(Blocks.CHEST.defaultBlockState()));
    }
}
