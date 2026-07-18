package dev.upiscium.frontierprotocol.api.suppression;

import dev.upiscium.frontierprotocol.suppression.ServerInfectionSuppressionService;

public final class InfectionSuppressionApi {
    private InfectionSuppressionApi() {}

    public static InfectionSuppressionService get() {
        return ServerInfectionSuppressionService.INSTANCE;
    }
}
