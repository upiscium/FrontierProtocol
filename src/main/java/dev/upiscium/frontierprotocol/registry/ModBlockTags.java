package dev.upiscium.frontierprotocol.registry;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModBlockTags {
    public static final TagKey<Block> CLEANUP_REMOVABLE = create("cleanup/removable");
    public static final TagKey<Block> CLEANUP_NEVER = create("cleanup/never");

    private ModBlockTags() {}

    private static TagKey<Block> create(String path) {
        return TagKey.create(
                Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, path));
    }
}
