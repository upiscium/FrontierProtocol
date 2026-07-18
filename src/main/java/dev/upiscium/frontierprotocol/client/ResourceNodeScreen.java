package dev.upiscium.frontierprotocol.client;

import dev.upiscium.frontierprotocol.resource.ResourceNodeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ResourceNodeScreen extends AbstractContainerScreen<ResourceNodeMenu> {
    public ResourceNodeScreen(ResourceNodeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 73;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF20262B);
        graphics.fill(leftPos + 79, topPos + 34, leftPos + 97, topPos + 52, 0xFF59636B);
        int progress = menu.scaledProgress(120);
        graphics.fill(leftPos + 28, topPos + 58, leftPos + 148, topPos + 64, 0xFF111619);
        graphics.fill(leftPos + 28, topPos + 58, leftPos + 28 + progress, topPos + 64, 0xFFB86B3D);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xE8ECEF, false);
        graphics.drawString(font, Component.translatable("gui.frontier_protocol.resource_node.trait", menu.requiredTrait()),
                8, 20, 0xC9D1D6, false);
        graphics.drawString(font, Component.translatable(
                "gui.frontier_protocol.resource_node.status." + menu.status().name().toLowerCase(java.util.Locale.ROOT)),
                8, 32, 0xC9D1D6, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xC9D1D6, false);
    }
}
