package dev.upiscium.frontierprotocol.client.nixie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.simibubi.create.content.trains.signal.SignalBlockEntity.SignalState;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerBlock;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerStatus;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

class StabilizerNixieSignalResolverTest {
    private static final StabilizerBlock STABILIZER = ModBlocks.TIER_1_STABILIZER.get();

    @Test
    void offlineMapsToRed() {
        assertEquals(SignalState.RED, resolve(StabilizerStatus.OFFLINE));
    }

    @Test
    void activeMapsToGreen() {
        assertEquals(SignalState.GREEN, resolve(StabilizerStatus.ACTIVE));
    }

    @Test
    void gracePeriodMapsToGreenWhileSuppressionContinues() {
        assertEquals(SignalState.GREEN, resolve(StabilizerStatus.GRACE_PERIOD));
    }

    @Test
    void nonStabilizerHasNoOverride() {
        assertTrue(StabilizerNixieSignalResolver.resolve(Blocks.STONE.defaultBlockState()).isEmpty());
    }

    private static SignalState resolve(StabilizerStatus status) {
        BlockState state = STABILIZER.defaultBlockState().setValue(StabilizerBlock.STATUS, status);
        return StabilizerNixieSignalResolver.resolve(state).orElseThrow();
    }
}
