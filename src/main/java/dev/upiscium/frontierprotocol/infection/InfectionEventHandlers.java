package dev.upiscium.frontierprotocol.infection;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.registry.ModAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = FrontierProtocolMod.MOD_ID)
public final class InfectionEventHandlers {
    private InfectionEventHandlers() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof Mob mob) || level != level.getServer().overworld()) return;
        if (!mob.hasData(ModAttachments.MOB_SCALING)) return;
        var state = mob.getData(ModAttachments.MOB_SCALING);
        InfectionRuntimeIndex index = InfectionRuntimeIndex.get(level);
        if (state.outbreakCarrier()) index.registerCarrier(mob);
        if (state.sourceNestId().isPresent()) index.registerNestMob(mob);
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Mob mob)) return;
        InfectionRuntimeIndex index = InfectionRuntimeIndex.get(level);
        index.removeCarrier(mob.getUUID());
        index.removeNestMob(mob.getUUID());
    }

    @SubscribeEvent
    public static void onEnteringSection(EntityEvent.EnteringSection event) {
        if (!event.didChunkChange() || !(event.getEntity() instanceof Mob mob)
                || !(mob.level() instanceof ServerLevel level)) return;
        InfectionRuntimeIndex.get(level).moveCarrier(mob.getUUID(), new ChunkPos(
                SectionPos.x(event.getPackedNewPos()), SectionPos.z(event.getPackedNewPos())));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCarrierDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Mob mob)
                || !(mob.level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof ServerPlayer)
                || !mob.getData(ModAttachments.MOB_SCALING).outbreakCarrier()) return;
        LevelChunk chunk = level.getChunkSource().getChunkNow(mob.chunkPosition().x, mob.chunkPosition().z);
        if (chunk != null) {
            ChunkInfectionState state = chunk.getData(ModAttachments.CHUNK_INFECTION)
                    .withPressureDelta(-FrontierProtocolServerConfig.INFECTION_CARRIER_KILL_REDUCTION.getAsInt(),
                            FrontierProtocolServerConfig.INFECTION_MAX_PRESSURE.getAsInt());
            chunk.setData(ModAttachments.CHUNK_INFECTION, state);
            InfectionRuntimeIndex.get(level).markPersistentChunk(chunk.getPos(), !state.equals(ChunkInfectionState.DEFAULT));
        }
        InfectionRuntimeIndex.get(level).removeCarrier(mob.getUUID());
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        chunk.getExistingData(ModAttachments.CHUNK_INFECTION).ifPresent(state ->
                InfectionRuntimeIndex.get(level).markPersistentChunk(chunk.getPos(), !state.equals(ChunkInfectionState.DEFAULT)));
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            InfectionRuntimeIndex.get(level).markPersistentChunk(event.getChunk().getPos(), false);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) InfectionRuntimeIndex.clear(level);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        int interval = FrontierProtocolServerConfig.INFECTION_SLOW_TICK_INTERVAL.getAsInt();
        if (level.getGameTime() % interval == 0) {
            level.getProfiler().push("frontier_protocol:infection_slow_tick");
            try {
                InfectionService.slowTick(level);
            } finally {
                level.getProfiler().pop();
            }
        }
    }
}
