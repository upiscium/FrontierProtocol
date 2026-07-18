package dev.upiscium.frontierprotocol.mob;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

public record MobScalingState(boolean applied, int distanceTier, boolean outbreakCarrier, Optional<UUID> sourceNestId) {
    public static final MobScalingState DEFAULT = new MobScalingState(false, 0, false, Optional.empty());
    public static final Codec<MobScalingState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("applied", false).forGetter(MobScalingState::applied),
            Codec.INT.optionalFieldOf("distance_tier", 0).forGetter(MobScalingState::distanceTier),
            Codec.BOOL.optionalFieldOf("outbreak_carrier", false).forGetter(MobScalingState::outbreakCarrier),
            UUIDUtil.CODEC.optionalFieldOf("source_nest_id").forGetter(MobScalingState::sourceNestId)
    ).apply(instance, MobScalingState::new));

    public MobScalingState withApplied(boolean value) {
        return new MobScalingState(value, distanceTier, outbreakCarrier, sourceNestId);
    }
}
