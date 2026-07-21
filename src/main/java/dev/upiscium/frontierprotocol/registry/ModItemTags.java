package dev.upiscium.frontierprotocol.registry;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModItemTags {
    public static final TagKey<Item> TIER_1_STABILIZER_CONSUMABLES = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, "tier_1_stabilizer_consumables"));

    private ModItemTags() {}
}
