package dev.upiscium.frontierprotocol.datagen;

import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class FrontierProtocolDataGenerators {
    private FrontierProtocolDataGenerators() {}

    public static void gatherData(GatherDataEvent event) {
        if (!event.includeServer()) return;
        event.addProvider(new FrontierProtocolRecipeProvider.Mixing(
                event.getGenerator().getPackOutput(), event.getLookupProvider()));
        event.addProvider(new FrontierProtocolRecipeProvider.Deploying(
                event.getGenerator().getPackOutput(), event.getLookupProvider()));
        event.addProvider(new FrontierProtocolRecipeProvider.MechanicalCrafting(
                event.getGenerator().getPackOutput(), event.getLookupProvider()));
        event.addProvider(new FrontierProtocolItemTagProvider(
                event.getGenerator().getPackOutput(), event.getLookupProvider(), event.getExistingFileHelper()));
        event.addProvider(new FrontierProtocolBlockTagProvider(
                event.getGenerator().getPackOutput(), event.getLookupProvider(), event.getExistingFileHelper()));
        event.addProvider(FrontierProtocolBlockLootProvider.create(
                event.getGenerator().getPackOutput(), event.getLookupProvider()));
    }
}
