package dev.upiscium.frontierprotocol.client.tooltip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

class FrontierProtocolItemTooltipTest {
    @Test
    void stabilizerSummariesContainNoMutableServerNumbers() {
        for (FrontierProtocolItemTooltips.TooltipKind kind : List.of(
                FrontierProtocolItemTooltips.TooltipKind.TIER_1,
                FrontierProtocolItemTooltips.TooltipKind.TIER_2,
                FrontierProtocolItemTooltips.TooltipKind.TIER_3)) {
            List<Component> tooltip = new ArrayList<>();
            FrontierProtocolItemTooltips.append(kind, tooltip, false);
            assertEquals("frontier_protocol.tooltip.stabilizer.summary", key(tooltip.get(0)));
            assertEquals(kind.name().toLowerCase().replace("tier_", "frontier_protocol.tooltip.tier_") + ".summary", key(tooltip.get(1)));
            assertEquals("frontier_protocol.tooltip.hold_shift", key(tooltip.get(2)));
        }
    }

    @Test
    void expandedConsumablesUseTheirStaticProductionDetails() {
        List<Component> cell = new ArrayList<>();
        FrontierProtocolItemTooltips.append(FrontierProtocolItemTooltips.TooltipKind.CELL, cell, true);
        assertEquals("frontier_protocol.tooltip.cell.summary", key(cell.get(0)));
        assertEquals("frontier_protocol.tooltip.cell.details", key(cell.get(1)));

        List<Component> compound = new ArrayList<>();
        FrontierProtocolItemTooltips.append(FrontierProtocolItemTooltips.TooltipKind.COMPOUND, compound, true);
        assertEquals("frontier_protocol.tooltip.compound.summary", key(compound.get(0)));
        assertEquals("frontier_protocol.tooltip.compound.details", key(compound.get(1)));
    }

    private static String key(Component component) {
        return ((TranslatableContents) component.getContents()).getKey();
    }
}
