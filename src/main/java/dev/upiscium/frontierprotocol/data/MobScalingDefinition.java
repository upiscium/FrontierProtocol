package dev.upiscium.frontierprotocol.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

public record MobScalingDefinition(boolean enabled, Optional<Integer> maxTier) {
    public static final Codec<MobScalingDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(MobScalingDefinition::enabled),
            Codec.intRange(0, 4).optionalFieldOf("max_tier").forGetter(MobScalingDefinition::maxTier)
    ).apply(instance, MobScalingDefinition::new));
}
