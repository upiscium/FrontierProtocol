package dev.upiscium.frontierprotocol.api.suppression;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record SuppressionSourceId(ResourceLocation value) {
    public SuppressionSourceId {
        Objects.requireNonNull(value, "value");
    }
}
