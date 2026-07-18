package dev.upiscium.frontierprotocol.nutrition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public record NutritionEntry(ResourceLocation item, Optional<ResourceLocation> category) {
    public static final Codec<NutritionEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("item").forGetter(NutritionEntry::item),
            ResourceLocation.CODEC.optionalFieldOf("category").forGetter(NutritionEntry::category)
    ).apply(instance, NutritionEntry::new));
}
