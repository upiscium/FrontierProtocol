package dev.upiscium.frontierprotocol.api.suppression;

import java.util.Objects;

public record SuppressionSource(SuppressionSourceId id, SuppressionSourceType type) {
    public SuppressionSource {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
    }
}
