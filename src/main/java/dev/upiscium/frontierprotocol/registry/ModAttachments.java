package dev.upiscium.frontierprotocol.registry;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.mob.MobScalingState;
import dev.upiscium.frontierprotocol.infection.ChunkInfectionState;
import dev.upiscium.frontierprotocol.nutrition.FoodHistoryState;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(
            NeoForgeRegistries.ATTACHMENT_TYPES, FrontierProtocolMod.MOD_ID);
    public static final Supplier<AttachmentType<MobScalingState>> MOB_SCALING = ATTACHMENTS.register(
            "mob_scaling", () -> AttachmentType.builder(() -> MobScalingState.DEFAULT)
                    .serialize(MobScalingState.CODEC).build());
    public static final Supplier<AttachmentType<ChunkInfectionState>> CHUNK_INFECTION = ATTACHMENTS.register(
            "chunk_infection", () -> AttachmentType.builder(() -> ChunkInfectionState.DEFAULT)
                    .serialize(ChunkInfectionState.CODEC, state -> !state.equals(ChunkInfectionState.DEFAULT)).build());
    public static final Supplier<AttachmentType<FoodHistoryState>> FOOD_HISTORY = ATTACHMENTS.register(
            "food_history", () -> AttachmentType.builder(() -> FoodHistoryState.EMPTY)
                    .serialize(FoodHistoryState.CODEC, state -> !state.equals(FoodHistoryState.EMPTY))
                    .copyOnDeath().build());

    private ModAttachments() {}

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }
}
