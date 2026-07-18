package dev.upiscium.frontierprotocol.registry;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class ModEntityTypeTags {
    public static final TagKey<EntityType<?>> DISTANCE_SCALED_MOBS = create("distance_scaled_mobs");
    public static final TagKey<EntityType<?>> OUTBREAK_CARRIERS = create("outbreak_carriers");
    public static final TagKey<EntityType<?>> NEST_SPAWNS = create("nest_spawns");
    public static final TagKey<EntityType<?>> BREACHER_MOBS = create("breacher_mobs");

    private ModEntityTypeTags() {}

    private static TagKey<EntityType<?>> create(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, path));
    }
}
