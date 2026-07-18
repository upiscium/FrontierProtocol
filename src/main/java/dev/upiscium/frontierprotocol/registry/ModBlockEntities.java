package dev.upiscium.frontierprotocol.registry;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(
            Registries.BLOCK_ENTITY_TYPE, FrontierProtocolMod.MOD_ID);
    private ModBlockEntities() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
