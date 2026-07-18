package dev.upiscium.frontierprotocol.infection;

import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.mob.MobEventHandlers;
import dev.upiscium.frontierprotocol.protection.StabilizationBeaconBlockEntity;
import dev.upiscium.frontierprotocol.protection.ServerProtectionService;
import dev.upiscium.frontierprotocol.registry.ModAttachments;
import dev.upiscium.frontierprotocol.registry.ModBlockTags;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.registry.ModEntityTypeTags;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.EventHooks;

public final class InfectionService {
    private static int nextChunk;

    private InfectionService() {}

    public static void slowTick(ServerLevel level) {
        if (level != level.getServer().overworld()) return;
        InfectionRuntimeIndex index = InfectionRuntimeIndex.get(level);
        List<Long> candidates = new ArrayList<>(index.candidateChunks());
        if (candidates.isEmpty()) return;
        int budget = Math.min(candidates.size(), FrontierProtocolServerConfig.INFECTION_CHUNK_BUDGET.getAsInt());
        int start = Math.floorMod(nextChunk, candidates.size());
        for (int processed = 0; processed < budget; processed++) {
            processLoadedChunk(level, new ChunkPos(candidates.get((start + processed) % candidates.size())));
        }
        nextChunk = (start + budget) % candidates.size();
    }

    public static void processLoadedChunk(ServerLevel level, ChunkPos pos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk == null || ServerProtectionService.INSTANCE.isChunkProtected(level, pos)) return;
        InfectionRuntimeIndex index = InfectionRuntimeIndex.get(level);
        ChunkInfectionState state = chunk.getData(ModAttachments.CHUNK_INFECTION);

        int contributingCarriers = 0;
        double activityRadius = FrontierProtocolServerConfig.INFECTION_PLAYER_ACTIVITY_RADIUS.getAsInt();
        for (Mob carrier : index.carriersIn(pos)) {
            if (level.hasNearbyAlivePlayer(carrier.getX(), carrier.getY(), carrier.getZ(), activityRadius)) {
                contributingCarriers++;
            }
        }
        if (contributingCarriers > 0) {
            long increase = (long) contributingCarriers
                    * FrontierProtocolServerConfig.INFECTION_PRESSURE_PER_CARRIER.getAsInt();
            state = state.withPressureDelta((int) Math.min(Integer.MAX_VALUE, increase), maximumPressure());
            chunk.setData(ModAttachments.CHUNK_INFECTION, state);
        }

        if (state.infectionPos().isEmpty()
                && state.pressure() >= FrontierProtocolServerConfig.INFECTION_CORE_THRESHOLD.getAsInt()) {
            findCorePosition(level, chunk).ifPresent(corePos -> {
                if (level.setBlock(corePos, ModBlocks.INFECTION_CORE.get().defaultBlockState(), 3)) {
                    chunk.setData(ModAttachments.CHUNK_INFECTION,
                            chunk.getData(ModAttachments.CHUNK_INFECTION).withCore(corePos));
                    index.markPersistentChunk(pos, true);
                }
            });
            state = chunk.getData(ModAttachments.CHUNK_INFECTION);
        }

        if (state.infectionPos().isPresent() && state.nestId().isEmpty()) {
            BlockPos corePos = state.infectionPos().get();
            if (!level.getBlockState(corePos).is(ModBlocks.INFECTION_CORE.get())) {
                clearStaleInfection(level, chunk, pos);
                return;
            }
            int progress = Math.min(Integer.MAX_VALUE,
                    state.activeLoadedTicks() + FrontierProtocolServerConfig.INFECTION_SLOW_TICK_INTERVAL.getAsInt());
            state = state.withMaturationProgress(progress);
            chunk.setData(ModAttachments.CHUNK_INFECTION, state);
            if (progress >= FrontierProtocolServerConfig.INFECTION_CORE_MATURATION_TICKS.getAsInt()) {
                matureCore(level, chunk, corePos);
            }
        } else if (state.nestId().isPresent()) {
            InfectionNestBlockEntity nest = index.nestIn(pos);
            if (nest != null && nest.isSpawnDue(level.getGameTime())) {
                nest.scheduleNextSpawn(level);
                trySpawnNestMob(level, nest, index);
            }
        }
    }

    public static void onInfectionBlockDestroyed(ServerLevel level, BlockPos pos, boolean nest) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return;
        ChunkInfectionState state = chunk.getData(ModAttachments.CHUNK_INFECTION);
        if (state.infectionPos().filter(pos::equals).isEmpty()) return;
        int reduction = nest ? FrontierProtocolServerConfig.INFECTION_NEST_BREAK_REDUCTION.getAsInt()
                : FrontierProtocolServerConfig.INFECTION_CORE_BREAK_REDUCTION.getAsInt();
        chunk.setData(ModAttachments.CHUNK_INFECTION, state.withoutInfectionBlock(reduction, maximumPressure()));
        InfectionRuntimeIndex.get(level).markPersistentChunk(new ChunkPos(pos), false);
    }

    public static ChunkInfectionState getState(ServerLevel level, ChunkPos pos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        return chunk == null ? ChunkInfectionState.DEFAULT : chunk.getData(ModAttachments.CHUNK_INFECTION);
    }

    public static boolean setPressure(ServerLevel level, ChunkPos pos, int pressure) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk == null) return false;
        ChunkInfectionState old = chunk.getData(ModAttachments.CHUNK_INFECTION);
        ChunkInfectionState changed = new ChunkInfectionState(Math.max(0, Math.min(maximumPressure(), pressure)),
                old.infectionPos(), old.activeLoadedTicks(), old.nestId());
        chunk.setData(ModAttachments.CHUNK_INFECTION, changed);
        InfectionRuntimeIndex.get(level).markPersistentChunk(pos, !changed.equals(ChunkInfectionState.DEFAULT));
        return true;
    }

    public static void clear(ServerLevel level, ChunkPos pos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk == null) return;
        ChunkInfectionState state = chunk.getData(ModAttachments.CHUNK_INFECTION);
        state.infectionPos().ifPresent(blockPos -> {
            if (level.getBlockState(blockPos).is(ModBlocks.INFECTION_CORE.get())
                    || level.getBlockState(blockPos).is(ModBlocks.INFECTION_NEST.get())) {
                level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3);
            }
        });
        chunk.setData(ModAttachments.CHUNK_INFECTION, ChunkInfectionState.DEFAULT);
        InfectionRuntimeIndex.get(level).markPersistentChunk(pos, false);
    }

    private static java.util.Optional<BlockPos> findCorePosition(ServerLevel level, LevelChunk chunk) {
        int attempts = FrontierProtocolServerConfig.INFECTION_CORE_CANDIDATES.getAsInt();
        for (int attempt = 0; attempt < attempts; attempt++) {
            int localX = level.getRandom().nextInt(16);
            int localZ = level.getRandom().nextInt(16);
            int x = chunk.getPos().getMinBlockX() + localX;
            int z = chunk.getPos().getMinBlockZ() + localZ;
            int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, localX, localZ);
            BlockPos target = new BlockPos(x, y, z);
            BlockPos ground = target.below();
            if (!chunk.getBlockState(ground).is(ModBlockTags.INFECTION_CORE_GROUND)) continue;
            if (!(chunk.getBlockState(target).isAir()
                    || chunk.getBlockState(target).is(ModBlockTags.INFECTION_CORE_REPLACEABLE))) continue;
            if (level.hasNearbyAlivePlayer(x + 0.5, y + 0.5, z + 0.5, 24.0)) continue;
            if (hasNearbyBedOrAnchor(level, target) || hasNearbyBeacon(level, target, 16)
                    || hasNearbyBlockEntity(level, target, 8)) continue;
            return java.util.Optional.of(target);
        }
        return java.util.Optional.empty();
    }

    private static boolean hasNearbyBedOrAnchor(ServerLevel level, BlockPos center) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = center.getX() - 16; x <= center.getX() + 16; x++) {
            for (int z = center.getZ() - 16; z <= center.getZ() + 16; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
                if (chunk == null) continue;
                for (int y = Math.max(level.getMinBuildHeight(), center.getY() - 16);
                        y <= Math.min(level.getMaxBuildHeight() - 1, center.getY() + 16); y++) {
                    cursor.set(x, y, z);
                    var state = chunk.getBlockState(cursor);
                    if (state.is(BlockTags.BEDS) || state.is(Blocks.RESPAWN_ANCHOR)) return true;
                }
            }
        }
        return false;
    }

    private static boolean hasNearbyBeacon(ServerLevel level, BlockPos center, int radius) {
        int minChunkX = (center.getX() - radius) >> 4;
        int maxChunkX = (center.getX() + radius) >> 4;
        int minChunkZ = (center.getZ() - radius) >> 4;
        int maxChunkZ = (center.getZ() + radius) >> 4;
        long radiusSquared = (long) radius * radius;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof StabilizationBeaconBlockEntity
                            && blockEntity.getBlockPos().distSqr(center) <= radiusSquared) return true;
                }
            }
        }
        return false;
    }

    private static boolean hasNearbyBlockEntity(ServerLevel level, BlockPos center, int radius) {
        int minChunkX = (center.getX() - radius) >> 4;
        int maxChunkX = (center.getX() + radius) >> 4;
        int minChunkZ = (center.getZ() - radius) >> 4;
        int maxChunkZ = (center.getZ() + radius) >> 4;
        long radiusSquared = (long) radius * radius;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity.getBlockPos().distSqr(center) <= radiusSquared) return true;
                }
            }
        }
        return false;
    }

    private static void matureCore(ServerLevel level, LevelChunk chunk, BlockPos pos) {
        if (!level.setBlock(pos, ModBlocks.INFECTION_NEST.get().defaultBlockState(), 3)) return;
        if (level.getBlockEntity(pos) instanceof InfectionNestBlockEntity nest) {
            ChunkInfectionState state = chunk.getData(ModAttachments.CHUNK_INFECTION).withNest(nest.nestId());
            chunk.setData(ModAttachments.CHUNK_INFECTION, state);
            InfectionRuntimeIndex.get(level).registerNest(nest);
        }
    }

    private static void trySpawnNestMob(ServerLevel level, InfectionNestBlockEntity nest, InfectionRuntimeIndex index) {
        BlockPos nestPos = nest.getBlockPos();
        if (!level.hasNearbyAlivePlayer(nestPos.getX() + 0.5, nestPos.getY() + 0.5, nestPos.getZ() + 0.5,
                FrontierProtocolServerConfig.INFECTION_PLAYER_ACTIVITY_RADIUS.getAsInt())) return;
        if (index.nestMobCapReached(nestPos,
                FrontierProtocolServerConfig.INFECTION_NEST_LOCAL_RADIUS.getAsInt(),
                FrontierProtocolServerConfig.INFECTION_NEST_LOCAL_CAP.getAsInt(),
                FrontierProtocolServerConfig.INFECTION_NEST_GLOBAL_CAP.getAsInt())) return;
        var registry = level.registryAccess().registryOrThrow(Registries.ENTITY_TYPE);
        java.util.Optional<Holder<EntityType<?>>> selected = registry.getRandomElementOf(
                ModEntityTypeTags.NEST_SPAWNS, level.getRandom());
        if (selected.isEmpty()) return;
        for (int attempt = 0; attempt < 8; attempt++) {
            BlockPos spawnPos = nestPos.offset(level.getRandom().nextIntBetweenInclusive(-4, 4), 1,
                    level.getRandom().nextIntBetweenInclusive(-4, 4));
            EntityType<?> type = selected.get().value();
            if (!type.canSummon() || !SpawnPlacements.isSpawnPositionOk(type, level, spawnPos)
                    || !SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.EVENT, spawnPos, level.getRandom())
                    || !level.noCollision(type.getSpawnAABB(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5))) {
                continue;
            }
            Entity entity = type.create(level);
            if (!(entity instanceof Mob mob)) return;
            mob.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    level.getRandom().nextFloat() * 360.0F, 0.0F);
            if (!EventHooks.checkSpawnPosition(mob, level, MobSpawnType.EVENT)) {
                mob.discard();
                continue;
            }
            EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);
            if (mob.isSpawnCancelled()) {
                mob.discard();
                return;
            }
            MobEventHandlers.prepareNestScaling(mob, level);
            MobEventHandlers.markNestSource(mob, nest.nestId());
            if (level.addFreshEntity(mob)) index.registerNestMob(mob);
            else mob.discard();
            return;
        }
    }

    private static void clearStaleInfection(ServerLevel level, LevelChunk chunk, ChunkPos pos) {
        chunk.setData(ModAttachments.CHUNK_INFECTION,
                chunk.getData(ModAttachments.CHUNK_INFECTION).withoutInfectionBlock(0, maximumPressure()));
        InfectionRuntimeIndex.get(level).markPersistentChunk(pos, false);
    }

    private static int maximumPressure() {
        return FrontierProtocolServerConfig.INFECTION_MAX_PRESSURE.getAsInt();
    }
}
