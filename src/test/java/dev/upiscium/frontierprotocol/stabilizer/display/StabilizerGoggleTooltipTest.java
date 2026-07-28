package dev.upiscium.frontierprotocol.stabilizer.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.upiscium.frontierprotocol.stabilizer.StabilizerStatus;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerTier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class StabilizerGoggleTooltipTest {
    @Test
    void unsynchronizedSnapshotUsesOnlyFallbackMessage() {
        List<Component> tooltip = new ArrayList<>();
        StabilizerGoggleTooltip.addSynchronizing(tooltip);
        assertEquals("frontier_protocol.goggles.title", key(tooltip.get(1)));
        assertEquals("frontier_protocol.goggles.synchronizing", key(tooltip.get(2)));
    }

    @Test
    void normalAndShiftViewsContainOperationalAndBoundedDetails() {
        StabilizerDisplaySnapshot snapshot = new StabilizerDisplaySnapshot(
                StabilizerTier.TIER_2,
                StabilizerStatus.GRACE_PERIOD,
                64,
                64.0,
                12,
                32,
                800,
                3000,
                400,
                1);
        List<Component> normal = new ArrayList<>();
        StabilizerGoggleTooltip.add(normal, false, snapshot, new ChunkPos(-2, 4), 64.0F, false);
        assertTrue(keys(normal).contains("frontier_protocol.goggles.grace_time"));
        assertTrue(keys(normal).contains("frontier_protocol.goggles.diagnostic"));
        assertTrue(!keys(normal).contains("frontier_protocol.goggles.center_chunk"));

        List<Component> detailed = new ArrayList<>();
        StabilizerGoggleTooltip.add(detailed, true, snapshot, new ChunkPos(-2, 4), 64.0F, false);
        assertTrue(keys(detailed).contains("frontier_protocol.goggles.center_chunk"));
        assertTrue(keys(detailed).contains("frontier_protocol.goggles.chunk_x_range"));
        assertTrue(keys(detailed).contains("frontier_protocol.goggles.chunk_z_range"));
        assertTrue(keys(detailed).contains("frontier_protocol.goggles.stress_impact"));
        assertTrue(keys(detailed).contains("frontier_protocol.goggles.cell_duration"));
    }

    @Test
    void activeViewExposesThePreservedPerCellGraceBudget() {
        StabilizerDisplaySnapshot snapshot = new StabilizerDisplaySnapshot(
                StabilizerTier.TIER_1,
                StabilizerStatus.ACTIVE,
                32,
                16.0,
                1,
                8,
                4000,
                6000,
                800,
                0);
        List<Component> tooltip = new ArrayList<>();

        StabilizerGoggleTooltip.add(tooltip, false, snapshot, new ChunkPos(0, 0), 32.0F, false);

        assertTrue(keys(tooltip).contains("frontier_protocol.goggles.cell_time"));
        assertTrue(keys(tooltip).contains("frontier_protocol.goggles.grace_time"));
    }

    private static List<String> keys(List<Component> tooltip) {
        return tooltip.stream()
                .filter(component -> component.getContents() instanceof TranslatableContents)
                .map(StabilizerGoggleTooltipTest::key)
                .toList();
    }

    private static String key(Component component) {
        return ((TranslatableContents) component.getContents()).getKey();
    }
}
