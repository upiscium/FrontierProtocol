package dev.upiscium.frontierprotocol.infection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class ChunkInfectionStateTest {
    @Test
    void pressureIsClampedAtBothBounds() {
        assertEquals(100, ChunkInfectionState.DEFAULT.withPressureDelta(150, 100).pressure());
        assertEquals(0, new ChunkInfectionState(3, Optional.empty(), 0, Optional.empty())
                .withPressureDelta(-5, 100).pressure());
    }

    @Test
    void stagesFollowThresholdCoreAndNestOrder() {
        ChunkInfectionState pressure = ChunkInfectionState.DEFAULT.withPressureDelta(60, 100);
        assertEquals(InfectionStage.PRESSURIZED, pressure.stage(60));
        ChunkInfectionState core = pressure.withCore(new BlockPos(1, 2, 3));
        assertEquals(InfectionStage.CORE, core.stage(60));
        assertEquals(InfectionStage.NEST, core.withNest(UUID.randomUUID()).stage(60));
    }

    @Test
    void destroyingInfectionClearsPositionProgressAndNest() {
        ChunkInfectionState nest = new ChunkInfectionState(80, Optional.of(BlockPos.ZERO), 123,
                Optional.of(UUID.randomUUID()));
        assertEquals(new ChunkInfectionState(50, Optional.empty(), 0, Optional.empty()),
                nest.withoutInfectionBlock(30, 100));
    }
}
