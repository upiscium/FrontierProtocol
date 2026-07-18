package dev.upiscium.frontierprotocol.resource;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public record WorkResult(Optional<ResourceLocation> blockingReason) {
    public static WorkResult allowed() {
        return new WorkResult(Optional.empty());
    }

    public static WorkResult blocked(ResourceLocation reason) {
        return new WorkResult(Optional.of(reason));
    }

    public boolean satisfied() {
        return blockingReason.isEmpty();
    }
}
