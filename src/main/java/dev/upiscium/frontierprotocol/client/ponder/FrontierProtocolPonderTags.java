package dev.upiscium.frontierprotocol.client.ponder;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.registry.ModItems;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public final class FrontierProtocolPonderTags {
    public static final ResourceLocation CONTAINMENT = id("containment");

    private FrontierProtocolPonderTags() {}

    public static void register(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(CONTAINMENT)
                .title("Frontier Protocol")
                .description("Create-powered infrastructure for suppressing Spore infection.")
                .item(ModItems.TIER_2_STABILIZER.get(), true, true)
                .addToIndex()
                .register();
        helper.addTagToComponent(ModItems.TIER_1_STABILIZER.getId(), CONTAINMENT);
        helper.addTagToComponent(ModItems.TIER_2_STABILIZER.getId(), CONTAINMENT);
        helper.addTagToComponent(ModItems.TIER_3_STABILIZER.getId(), CONTAINMENT);
        helper.addTagToComponent(ModItems.STABILIZATION_CELL.getId(), CONTAINMENT);
        helper.addTagToComponent(ModItems.STABILIZATION_COMPOUND.getId(), CONTAINMENT);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, path);
    }
}
