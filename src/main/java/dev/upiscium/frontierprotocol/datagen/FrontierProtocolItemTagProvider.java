package dev.upiscium.frontierprotocol.datagen;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.registry.ModItemTags;
import dev.upiscium.frontierprotocol.registry.ModItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class FrontierProtocolItemTagProvider extends ItemTagsProvider {
    public FrontierProtocolItemTagProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> registries,
            ExistingFileHelper existingFileHelper) {
        super(
                output,
                registries,
                CompletableFuture.completedFuture(TagsProvider.TagLookup.<Block>empty()),
                FrontierProtocolMod.MOD_ID,
                existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(ModItemTags.STABILIZER_CONSUMABLES)
                .add(ModItems.STABILIZATION_CELL.get())
                .getInternalBuilder()
                .replace(false);
    }
}
