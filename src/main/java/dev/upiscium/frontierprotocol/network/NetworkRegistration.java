package dev.upiscium.frontierprotocol.network;

import dev.upiscium.frontierprotocol.config.FrontierProtocolClientConfig;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class NetworkRegistration {
    public static final int PROTOCOL_VERSION = 1;

    private NetworkRegistration() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(Integer.toString(PROTOCOL_VERSION));
        registrar.playToClient(
                SectorInfoPayload.TYPE,
                SectorInfoPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (payload.protocolVersion() != PROTOCOL_VERSION || !FrontierProtocolClientConfig.SHOW_DISCOVERY_MESSAGES.get()) return;
                    context.player().displayClientMessage(Component.translatable(
                            "message.frontier_protocol.sector_discovered",
                            Component.translatable("sector_trait." + payload.trait().getNamespace() + "." + payload.trait().getPath())), true);
                });
        registrar.playToClient(
                NutritionResultPayload.TYPE,
                NutritionResultPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (payload.protocolVersion() != PROTOCOL_VERSION) return;
                    String key = payload.repeated()
                            ? "message.frontier_protocol.nutrition_efficiency_repeated"
                            : "message.frontier_protocol.nutrition_efficiency";
                    context.player().displayClientMessage(
                            Component.translatable(key, payload.efficiencyPercent()), true);
                });
    }
}
