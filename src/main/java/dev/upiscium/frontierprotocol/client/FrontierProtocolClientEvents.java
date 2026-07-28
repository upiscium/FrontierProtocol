package dev.upiscium.frontierprotocol.client;

import dev.upiscium.frontierprotocol.client.tooltip.FrontierProtocolItemTooltips;
import dev.upiscium.frontierprotocol.client.render.StabilizerRangeRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class FrontierProtocolClientEvents {
    private FrontierProtocolClientEvents() {}

    @SubscribeEvent
    public static void itemTooltip(ItemTooltipEvent event) {
        FrontierProtocolItemTooltips.append(event.getItemStack(), event.getToolTip(), Screen.hasShiftDown());
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent event) {
        StabilizerRangeRenderer.render(event);
    }
}
