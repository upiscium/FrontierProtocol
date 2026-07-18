package dev.upiscium.frontierprotocol.infection;

import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.registry.ModBlockEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import dev.upiscium.frontierprotocol.registry.ModAttachments;

public final class InfectionNestBlockEntity extends BlockEntity {
    private static final String NEST_ID_TAG = "NestId";
    private static final String NEXT_SPAWN_TAG = "NextSpawnGameTime";

    private UUID nestId = UUID.randomUUID();
    private long nextSpawnGameTime;

    public InfectionNestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFECTION_NEST.get(), pos, state);
    }

    public UUID nestId() {
        return nestId;
    }

    public long nextSpawnGameTime() {
        return nextSpawnGameTime;
    }

    void replaceNestId(UUID id) {
        nestId = id;
        setChanged();
    }

    public boolean isSpawnDue(long gameTime) {
        return gameTime >= nextSpawnGameTime;
    }

    public void scheduleNextSpawn(ServerLevel level) {
        int minimum = FrontierProtocolServerConfig.INFECTION_NEST_MIN_SPAWN_INTERVAL.getAsInt();
        int maximum = Math.max(minimum, FrontierProtocolServerConfig.INFECTION_NEST_MAX_SPAWN_INTERVAL.getAsInt());
        nextSpawnGameTime = level.getGameTime() + level.getRandom().nextIntBetweenInclusive(minimum, maximum);
        setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            if (nextSpawnGameTime <= 0) scheduleNextSpawn(serverLevel);
            InfectionRuntimeIndex.get(serverLevel).registerNest(this);
            LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(
                    getBlockPos().getX() >> 4, getBlockPos().getZ() >> 4);
            if (chunk != null) {
                chunk.getExistingData(ModAttachments.CHUNK_INFECTION)
                        .filter(state -> state.infectionPos().filter(getBlockPos()::equals).isPresent())
                        .filter(state -> state.nestId().filter(nestId::equals).isEmpty())
                        .ifPresent(state -> chunk.setData(ModAttachments.CHUNK_INFECTION, state.withNest(nestId)));
            }
        }
    }

    @Override
    public void onChunkUnloaded() {
        if (level instanceof ServerLevel serverLevel) InfectionRuntimeIndex.get(serverLevel).unregisterNest(this);
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) InfectionRuntimeIndex.get(serverLevel).unregisterNest(this);
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID(NEST_ID_TAG, nestId);
        tag.putLong(NEXT_SPAWN_TAG, nextSpawnGameTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID(NEST_ID_TAG)) nestId = tag.getUUID(NEST_ID_TAG);
        if (tag.contains(NEXT_SPAWN_TAG, Tag.TAG_LONG)) nextSpawnGameTime = tag.getLong(NEXT_SPAWN_TAG);
    }
}
