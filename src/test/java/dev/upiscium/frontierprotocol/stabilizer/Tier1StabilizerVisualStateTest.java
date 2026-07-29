package dev.upiscium.frontierprotocol.stabilizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.stabilizer.display.StabilizerDisplaySnapshot;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

class Tier1StabilizerVisualStateTest {
    @Test
    void blockStateSuppliesStatusWithoutSnapshot() {
        for (StabilizerStatus status : StabilizerStatus.values()) {
            assertEquals(status, Tier1StabilizerVisualState.resolveStatus(null, state(status)));
        }
    }

    @Test
    void snapshotStatusTakesPriorityOverBlockState() {
        assertEquals(
                StabilizerStatus.OFFLINE,
                Tier1StabilizerVisualState.resolveStatus(
                        snapshot(StabilizerStatus.OFFLINE, 64), state(StabilizerStatus.ACTIVE)));
        assertEquals(
                StabilizerStatus.ACTIVE,
                Tier1StabilizerVisualState.resolveStatus(
                        snapshot(StabilizerStatus.ACTIVE, 64), state(StabilizerStatus.OFFLINE)));
        assertEquals(
                StabilizerStatus.GRACE_PERIOD,
                Tier1StabilizerVisualState.resolveStatus(
                        snapshot(StabilizerStatus.GRACE_PERIOD, 64), state(StabilizerStatus.ACTIVE)));
    }

    @Test
    void configuredMinimumRpmIsUsedOnlyWithoutSnapshot() {
        assertEquals(32, Tier1StabilizerVisualState.resolveMinimumRpm(null, 32));
        assertEquals(64, Tier1StabilizerVisualState.resolveMinimumRpm(snapshot(StabilizerStatus.ACTIVE, 64), 32));
    }

    @Test
    void stateWithoutStatusFallsBackOffline() {
        assertEquals(
                StabilizerStatus.OFFLINE,
                Tier1StabilizerVisualState.resolveStatus(null, Blocks.STONE.defaultBlockState()));
    }

    @Test
    void snapshotlessGraceUsesLowFrequencyBlinkBand() {
        for (double time : new double[] {0.0, 0.25, 0.5, 0.75}) {
            assertEquals(
                    Tier1StabilizerAnimation.graceLightAlpha(time, 3, 4),
                    Tier1StabilizerVisualState.resolveGraceLightAlpha(null, time));
        }
    }

    private static BlockState state(StabilizerStatus status) {
        return ModBlocks.TIER_1_STABILIZER.get().defaultBlockState().setValue(StabilizerBlock.STATUS, status);
    }

    private static StabilizerDisplaySnapshot snapshot(StabilizerStatus status, int minimumRpm) {
        return new StabilizerDisplaySnapshot(
                StabilizerTier.TIER_1,
                status,
                minimumRpm,
                16.0,
                1,
                8,
                6000,
                6000,
                900,
                1200,
                0);
    }
}
