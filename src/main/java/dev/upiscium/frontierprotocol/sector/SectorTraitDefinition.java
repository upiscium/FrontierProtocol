package dev.upiscium.frontierprotocol.sector;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public record SectorTraitDefinition(
        ResourceLocation id,
        int priority,
        Placement placement,
        Guarantee guarantee,
        List<ResourceLocation> resourceNodes) {
    private static final ResourceLocation UNBOUND_ID = ResourceLocation.fromNamespaceAndPath("frontier_protocol", "unbound");

    public static final Codec<SectorTraitDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("priority").forGetter(SectorTraitDefinition::priority),
            Placement.CODEC.fieldOf("placement").forGetter(SectorTraitDefinition::placement),
            Guarantee.CODEC.optionalFieldOf("guarantee", Guarantee.DISABLED).forGetter(SectorTraitDefinition::guarantee),
            ResourceLocation.CODEC.listOf().optionalFieldOf("resource_nodes", List.of()).forGetter(SectorTraitDefinition::resourceNodes)
    ).apply(instance, (priority, placement, guarantee, nodes) ->
            new SectorTraitDefinition(UNBOUND_ID, priority, placement, guarantee, List.copyOf(nodes))));

    public SectorTraitDefinition withId(ResourceLocation newId) {
        return new SectorTraitDefinition(newId, priority, placement, guarantee, resourceNodes);
    }

    public record Placement(
            int spacing,
            int separation,
            double anchorChance,
            int minDistance,
            Optional<Integer> maxDistance,
            int clusterMin,
            int clusterMax,
            long salt) {
        private static final Codec<Placement> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("spacing").forGetter(Placement::spacing),
                Codec.INT.fieldOf("separation").forGetter(Placement::separation),
                Codec.DOUBLE.fieldOf("anchor_chance").forGetter(Placement::anchorChance),
                Codec.INT.fieldOf("min_distance").forGetter(Placement::minDistance),
                Codec.INT.optionalFieldOf("max_distance").forGetter(Placement::maxDistance),
                Codec.INT.fieldOf("cluster_min").forGetter(Placement::clusterMin),
                Codec.INT.fieldOf("cluster_max").forGetter(Placement::clusterMax),
                Codec.LONG.fieldOf("salt").forGetter(Placement::salt)
        ).apply(instance, Placement::new));
        public static final Codec<Placement> CODEC = RAW_CODEC.validate(Placement::validate);

        private static DataResult<Placement> validate(Placement value) {
            if (value.spacing < 1) return DataResult.error(() -> "spacing must be at least 1");
            if (value.separation < 0 || value.separation >= value.spacing) return DataResult.error(() -> "separation must be in [0, spacing)");
            if (!Double.isFinite(value.anchorChance) || value.anchorChance < 0 || value.anchorChance > 1) return DataResult.error(() -> "anchor_chance must be in [0, 1]");
            if (value.minDistance < 0) return DataResult.error(() -> "min_distance must not be negative");
            if (value.maxDistance.isPresent() && value.maxDistance.get() < value.minDistance) return DataResult.error(() -> "max_distance must be at least min_distance");
            if (value.clusterMin < 1 || value.clusterMax < value.clusterMin) return DataResult.error(() -> "cluster range must satisfy 1 <= min <= max");
            if (value.clusterMax > 16) return DataResult.error(() -> "cluster_max must not exceed 16");
            if (value.clusterMax > value.spacing) return DataResult.error(() -> "cluster_max must not exceed spacing");
            return DataResult.success(value);
        }
    }

    public record Guarantee(boolean enabled, int count, int ringMin, int ringMax) {
        public static final Guarantee DISABLED = new Guarantee(false, 0, 0, 0);
        private static final Codec<Guarantee> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.fieldOf("enabled").forGetter(Guarantee::enabled),
                Codec.INT.fieldOf("count").forGetter(Guarantee::count),
                Codec.INT.fieldOf("ring_min").forGetter(Guarantee::ringMin),
                Codec.INT.fieldOf("ring_max").forGetter(Guarantee::ringMax)
        ).apply(instance, Guarantee::new));
        public static final Codec<Guarantee> CODEC = RAW_CODEC.validate(Guarantee::validate);

        private static DataResult<Guarantee> validate(Guarantee value) {
            if (value.count < 0) return DataResult.error(() -> "guarantee count must not be negative");
            if (value.ringMin < 0 || value.ringMax < value.ringMin) return DataResult.error(() -> "guarantee ring must satisfy 0 <= min <= max");
            if (value.ringMax > 128) return DataResult.error(() -> "ring_max must not exceed 128");
            return DataResult.success(value);
        }
    }
}
