package dev.upiscium.frontierprotocol.api.suppression;

import dev.upiscium.frontierprotocol.suppression.ServerInfectionSuppressionService;

/**
 * Entry point for the server-side infection suppression query API.
 *
 * <p>Queries must be made from the Minecraft server thread.</p>
 */
public final class InfectionSuppressionApi {
    private InfectionSuppressionApi() {}

    /**
     * Returns the query service. Registration lifecycle methods on its concrete implementation are internal APIs.
     */
    public static InfectionSuppressionService get() {
        return ServerInfectionSuppressionService.INSTANCE;
    }
}
