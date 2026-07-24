package dev.upiscium.frontierprotocol.datagen;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class FrontierProtocolBlockTagProvider extends BlockTagsProvider {
    private static final TagKey<Block> CREATE_NON_MOVABLE = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("create", "non_movable"));

    public FrontierProtocolBlockTagProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> registries,
            ExistingFileHelper existingFileHelper) {
        super(output, registries, FrontierProtocolMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(
                        ModBlocks.TIER_1_STABILIZER.get(),
                        ModBlocks.TIER_2_STABILIZER.get(),
                        ModBlocks.TIER_3_STABILIZER.get());
        tag(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.TIER_1_STABILIZER.get(), ModBlocks.TIER_2_STABILIZER.get());
        tag(BlockTags.NEEDS_DIAMOND_TOOL).add(ModBlocks.TIER_3_STABILIZER.get());
        tag(CREATE_NON_MOVABLE)
                .add(
                        ModBlocks.TIER_1_STABILIZER.get(),
                        ModBlocks.TIER_2_STABILIZER.get(),
                        ModBlocks.TIER_3_STABILIZER.get());
    }
}
