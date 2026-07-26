package dev.upiscium.frontierprotocol.client.tooltip;

import dev.upiscium.frontierprotocol.registry.ModItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class FrontierProtocolItemTooltips {
    private FrontierProtocolItemTooltips() {}

    public static boolean append(ItemStack stack, List<Component> tooltip, boolean expanded) {
        TooltipKind kind;
        if (stack.is(ModItems.TIER_1_STABILIZER.get())) {
            kind = TooltipKind.TIER_1;
        } else if (stack.is(ModItems.TIER_2_STABILIZER.get())) {
            kind = TooltipKind.TIER_2;
        } else if (stack.is(ModItems.TIER_3_STABILIZER.get())) {
            kind = TooltipKind.TIER_3;
        } else if (stack.is(ModItems.STABILIZATION_CELL.get())) {
            kind = TooltipKind.CELL;
        } else if (stack.is(ModItems.STABILIZATION_COMPOUND.get())) {
            kind = TooltipKind.COMPOUND;
        } else {
            return false;
        }
        append(kind, tooltip, expanded);
        return true;
    }

    static void append(TooltipKind kind, List<Component> tooltip, boolean expanded) {
        if (kind == TooltipKind.CELL || kind == TooltipKind.COMPOUND) {
            appendConsumable(
                    tooltip,
                    expanded,
                    kind == TooltipKind.CELL
                            ? "frontier_protocol.tooltip.cell"
                            : "frontier_protocol.tooltip.compound");
            return;
        }

        tooltip.add(Component.translatable("frontier_protocol.tooltip.stabilizer.summary")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(kind.translationPrefix + ".summary").withStyle(ChatFormatting.AQUA));
        if (expanded) {
            tooltip.add(Component.translatable("frontier_protocol.tooltip.stabilizer.rotation")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("frontier_protocol.tooltip.stabilizer.cleanup")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("frontier_protocol.tooltip.stabilizer.defense_warning")
                    .withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("frontier_protocol.tooltip.hold_shift")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void appendConsumable(
            List<Component> tooltip, boolean expanded, String translationPrefix) {
        tooltip.add(Component.translatable(translationPrefix + ".summary").withStyle(ChatFormatting.GRAY));
        if (expanded) {
            tooltip.add(Component.translatable(translationPrefix + ".details").withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("frontier_protocol.tooltip.hold_shift")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    enum TooltipKind {
        TIER_1("frontier_protocol.tooltip.tier_1"),
        TIER_2("frontier_protocol.tooltip.tier_2"),
        TIER_3("frontier_protocol.tooltip.tier_3"),
        CELL("frontier_protocol.tooltip.cell"),
        COMPOUND("frontier_protocol.tooltip.compound");

        private final String translationPrefix;

        TooltipKind(String translationPrefix) {
            this.translationPrefix = translationPrefix;
        }
    }
}
