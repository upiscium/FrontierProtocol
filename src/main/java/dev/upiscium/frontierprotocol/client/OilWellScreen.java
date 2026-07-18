package dev.upiscium.frontierprotocol.client;

import dev.upiscium.frontierprotocol.oil.OilWellMenu;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class OilWellScreen extends AbstractContainerScreen<OilWellMenu> {
    public OilWellScreen(OilWellMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 196;
        imageHeight = 108;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF20262B);
        int progress = menu.scaledProgress(156);
        graphics.fill(leftPos + 20, topPos + 78, leftPos + 176, topPos + 84, 0xFF111619);
        graphics.fill(leftPos + 20, topPos + 78, leftPos + 20 + progress, topPos + 84, 0xFFB86B3D);
        int fluid = menu.scaledFluid(48);
        graphics.fill(leftPos + 166, topPos + 20, leftPos + 178, topPos + 68, 0xFF111619);
        graphics.fill(leftPos + 166, topPos + 68 - fluid, leftPos + 178, topPos + 68, 0xFF332A20);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xE8ECEF, false);
        graphics.drawString(font, Component.translatable("gui.frontier_protocol.oil_well.trait", menu.requiredTrait()),
                8, 20, 0xC9D1D6, false);
        graphics.drawString(font, Component.translatable("gui.frontier_protocol.oil_well.mod", menu.requiredMod()),
                8, 32, 0xC9D1D6, false);
        graphics.drawString(font, Component.translatable("gui.frontier_protocol.oil_well.fluid",
                menu.outputFluid(), menu.fluidAmount(), menu.capacity()), 8, 44, 0xC9D1D6, false);
        graphics.drawString(font, Component.translatable(
                "gui.frontier_protocol.oil_well.status." + menu.status().name().toLowerCase(Locale.ROOT)),
                8, 58, 0xC9D1D6, false);
    }
}
