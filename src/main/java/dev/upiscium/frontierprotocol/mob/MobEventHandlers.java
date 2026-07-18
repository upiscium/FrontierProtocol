package dev.upiscium.frontierprotocol.mob;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.registry.ModAttachments;
import dev.upiscium.frontierprotocol.registry.ModEntityTypeTags;
import dev.upiscium.frontierprotocol.registry.ModDataMaps;
import dev.upiscium.frontierprotocol.data.MobScalingDefinition;
import dev.upiscium.frontierprotocol.protection.ServerProtectionService;
import dev.upiscium.frontierprotocol.world.FrontierProtocolWorldData;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

@EventBusSubscriber(modid = FrontierProtocolMod.MOD_ID)
public final class MobEventHandlers {
    private MobEventHandlers() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void prepareScaling(FinalizeSpawnEvent event) {
        if (event.isCanceled() || event.isSpawnCancelled() || !(event.getLevel() instanceof ServerLevel level)) return;
        if (level != level.getServer().overworld()) return;
        Mob mob = event.getEntity();
        boolean carrier = event.getSpawnType() == MobSpawnType.NATURAL
                && mob.getType().is(ModEntityTypeTags.OUTBREAK_CARRIERS)
                && !ServerProtectionService.INSTANCE.isBlockProtected(level, mob.blockPosition());

        if (isEnabledSpawnType(event.getSpawnType()) && mob.getType().is(ModEntityTypeTags.DISTANCE_SCALED_MOBS)) {
            prepareScaling(mob, level, carrier);
        } else if (carrier) {
            MobScalingState state = mob.getData(ModAttachments.MOB_SCALING);
            mob.setData(ModAttachments.MOB_SCALING,
                    new MobScalingState(state.applied(), state.distanceTier(), true, state.sourceNestId()));
        }
    }

    public static void prepareNestScaling(Mob mob, ServerLevel level) {
        if (FrontierProtocolServerConfig.SCALE_NEST_SPAWNS.get()
                && level == level.getServer().overworld()
                && mob.getType().is(ModEntityTypeTags.DISTANCE_SCALED_MOBS)) {
            prepareScaling(mob, level, true);
        }
    }

    public static void markNestSource(Mob mob, UUID nestId) {
        MobScalingState state = mob.getData(ModAttachments.MOB_SCALING);
        mob.setData(ModAttachments.MOB_SCALING,
                new MobScalingState(state.applied(), state.distanceTier(), true, Optional.of(nestId)));
    }

    private static void prepareScaling(Mob mob, ServerLevel level, boolean carrier) {
        MobScalingDefinition definition = mob.getType().builtInRegistryHolder().getData(ModDataMaps.MOB_SCALING);
        if (definition != null && !definition.enabled()) return;
        FrontierProtocolWorldData data = FrontierProtocolWorldData.get(level);
        ChunkPos chunk = new ChunkPos(mob.blockPosition());
        long distance = Math.max(
                Math.abs((long) chunk.x - data.originChunkX()),
                Math.abs((long) chunk.z - data.originChunkZ()));
        int tier = MobScalingService.configuredTierForDistance(distance);
        if (definition != null && definition.maxTier().isPresent()) {
            tier = Math.min(tier, definition.maxTier().get());
        }
        mob.setData(ModAttachments.MOB_SCALING,
                new MobScalingState(false, tier, carrier, Optional.empty()));
    }

    @SubscribeEvent
    public static void applyScaling(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Mob mob)) return;
        level.getProfiler().push("frontier_protocol:mob_scaling");
        try {
            MobScalingService.applyIfPrepared(mob);
        } finally {
            level.getProfiler().pop();
        }
    }

    private static boolean isEnabledSpawnType(MobSpawnType type) {
        if (type == MobSpawnType.NATURAL) return FrontierProtocolServerConfig.SCALE_NATURAL_SPAWNS.get();
        if (type == MobSpawnType.CHUNK_GENERATION) return FrontierProtocolServerConfig.SCALE_CHUNK_GENERATION_SPAWNS.get();
        return MobSpawnType.isSpawner(type) && FrontierProtocolServerConfig.SCALE_SPAWNER_SPAWNS.get();
    }
}
