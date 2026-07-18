package dev.upiscium.frontierprotocol.infection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;

public record ChunkInfectionState(
        int pressure,
        Optional<BlockPos> infectionPos,
        int activeLoadedTicks,
        Optional<UUID> nestId) {
    public static final ChunkInfectionState DEFAULT = new ChunkInfectionState(0, Optional.empty(), 0, Optional.empty());
    public static final Codec<ChunkInfectionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("pressure", 0).forGetter(ChunkInfectionState::pressure),
            BlockPos.CODEC.optionalFieldOf("infection_pos").forGetter(ChunkInfectionState::infectionPos),
            Codec.INT.optionalFieldOf("active_loaded_ticks", 0).forGetter(ChunkInfectionState::activeLoadedTicks),
            UUIDUtil.CODEC.optionalFieldOf("nest_id").forGetter(ChunkInfectionState::nestId)
    ).apply(instance, ChunkInfectionState::new));

    public ChunkInfectionState withPressureDelta(int delta, int maximum) {
        long changed = (long) pressure + delta;
        int clamped = (int) Math.max(0L, Math.min(maximum, changed));
        return new ChunkInfectionState(clamped, infectionPos, activeLoadedTicks, nestId);
    }

    public ChunkInfectionState withCore(BlockPos pos) {
        return new ChunkInfectionState(pressure, Optional.of(pos.immutable()), 0, Optional.empty());
    }

    public ChunkInfectionState withMaturationProgress(int ticks) {
        return new ChunkInfectionState(pressure, infectionPos, Math.max(0, ticks), nestId);
    }

    public ChunkInfectionState withNest(UUID id) {
        return new ChunkInfectionState(pressure, infectionPos, activeLoadedTicks, Optional.of(id));
    }

    public ChunkInfectionState withoutInfectionBlock(int pressureReduction, int maximum) {
        return new ChunkInfectionState(withPressureDelta(-pressureReduction, maximum).pressure(),
                Optional.empty(), 0, Optional.empty());
    }

    public InfectionStage stage(int coreThreshold) {
        if (nestId.isPresent()) return InfectionStage.NEST;
        if (infectionPos.isPresent()) return InfectionStage.CORE;
        return pressure >= coreThreshold ? InfectionStage.PRESSURIZED : InfectionStage.DORMANT;
    }
}
