package dev.upiscium.frontierprotocol.registry;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModBlockTags {
    public static final TagKey<Block> INFECTION_CORE_GROUND = create("infection_core_ground");
    public static final TagKey<Block> INFECTION_CORE_REPLACEABLE = create("infection_core_replaceable");
    public static final TagKey<Block> MOB_BREAKABLE = create("mob_breakable");

    private ModBlockTags() {}

    private static TagKey<Block> create(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, path));
    }
}
