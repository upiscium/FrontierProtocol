package dev.upiscium.frontierprotocol.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

public record FuelDefinition(int ticks) {
    public static final Codec<FuelDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("ticks").forGetter(FuelDefinition::ticks)
    ).apply(instance, FuelDefinition::new));
}
