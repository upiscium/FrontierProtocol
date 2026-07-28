package dev.upiscium.frontierprotocol.datagen;

import dev.upiscium.frontierprotocol.registry.ModBlocks;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public final class FrontierProtocolBlockLootProvider extends BlockLootSubProvider {
    private FrontierProtocolBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    public static LootTableProvider create(
            PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        return new LootTableProvider(
                output,
                Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(
                        FrontierProtocolBlockLootProvider::new, LootContextParamSets.BLOCK)),
                registries);
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.TIER_1_STABILIZER.get());
        dropSelf(ModBlocks.TIER_2_STABILIZER.get());
        dropSelf(ModBlocks.TIER_3_STABILIZER.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return List.of(
                ModBlocks.TIER_1_STABILIZER.get(),
                ModBlocks.TIER_2_STABILIZER.get(),
                ModBlocks.TIER_3_STABILIZER.get());
    }
}
