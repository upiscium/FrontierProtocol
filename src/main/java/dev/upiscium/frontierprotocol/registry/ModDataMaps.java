package dev.upiscium.frontierprotocol.registry;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.data.FuelDefinition;
import dev.upiscium.frontierprotocol.data.MobScalingDefinition;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

public final class ModDataMaps {
    public static final DataMapType<Item, FuelDefinition> STABILIZATION_FUELS = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, "stabilization_fuels"),
            Registries.ITEM,
            FuelDefinition.CODEC).build();
    public static final DataMapType<EntityType<?>, MobScalingDefinition> MOB_SCALING = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, "mob_scaling"),
            Registries.ENTITY_TYPE,
            MobScalingDefinition.CODEC).build();

    private ModDataMaps() {}

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(STABILIZATION_FUELS);
        event.register(MOB_SCALING);
    }
}
