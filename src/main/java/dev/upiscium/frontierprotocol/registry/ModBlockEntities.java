package dev.upiscium.frontierprotocol.registry;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.protection.StabilizationBeaconBlockEntity;
import dev.upiscium.frontierprotocol.infection.InfectionNestBlockEntity;
import dev.upiscium.frontierprotocol.resource.ResourceNodeBlockEntity;
import dev.upiscium.frontierprotocol.oil.OilWellBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(
            Registries.BLOCK_ENTITY_TYPE, FrontierProtocolMod.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StabilizationBeaconBlockEntity>> STABILIZATION_BEACON =
            TYPES.register("stabilization_beacon", () -> BlockEntityType.Builder.of(
                     StabilizationBeaconBlockEntity::new, ModBlocks.STABILIZATION_BEACON.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfectionNestBlockEntity>> INFECTION_NEST =
            TYPES.register("infection_nest", () -> BlockEntityType.Builder.of(
                    InfectionNestBlockEntity::new, ModBlocks.INFECTION_NEST.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResourceNodeBlockEntity>> RESOURCE_NODE =
            TYPES.register("resource_node", () -> BlockEntityType.Builder.of(
                    ResourceNodeBlockEntity::new, ModBlocks.RESOURCE_NODE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OilWellBlockEntity>> OIL_WELL =
            TYPES.register("oil_well", () -> BlockEntityType.Builder.of(
                    OilWellBlockEntity::new, ModBlocks.OIL_WELL.get()).build(null));

    private ModBlockEntities() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, STABILIZATION_BEACON.get(),
                (beacon, side) -> beacon.inventory());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RESOURCE_NODE.get(),
                (node, side) -> node.output());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, OIL_WELL.get(),
                (well, side) -> well.output());
    }
}
