package dev.upiscium.frontierprotocol.registry;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.tier1.Tier1StabilizerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(
            Registries.BLOCK_ENTITY_TYPE, FrontierProtocolMod.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Tier1StabilizerBlockEntity>>
            TIER_1_STABILIZER = TYPES.register("tier_1_stabilizer", () -> BlockEntityType.Builder.of(
                    Tier1StabilizerBlockEntity::new, ModBlocks.TIER_1_STABILIZER.get()).build(null));

    private ModBlockEntities() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                TIER_1_STABILIZER.get(),
                (blockEntity, direction) -> blockEntity.externalInventory());
    }
}
