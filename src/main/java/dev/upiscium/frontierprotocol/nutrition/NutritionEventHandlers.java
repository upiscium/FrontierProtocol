package dev.upiscium.frontierprotocol.nutrition;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.network.NetworkRegistration;
import dev.upiscium.frontierprotocol.network.NutritionResultPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = FrontierProtocolMod.MOD_ID)
public final class NutritionEventHandlers {
    private static final Map<UUID, MealSnapshot> SNAPSHOTS = new HashMap<>();

    private NutritionEventHandlers() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SNAPSHOTS.remove(player.getUUID());
        if (event.isCanceled() || event.getItem().getFoodProperties(player) == null
                || player.isCreative() && !FrontierProtocolServerConfig.NUTRITION_RECORD_CREATIVE.get()) return;
        var food = player.getFoodData();
        SNAPSHOTS.put(player.getUUID(), new MealSnapshot(
                food.getFoodLevel(), food.getSaturationLevel(), event.getHand(), event.getItem().getItem()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MealSnapshot before = SNAPSHOTS.remove(player.getUUID());
        if (before == null || before.hand() != event.getHand() || before.item() != event.getItem().getItem()) return;
        NutritionService.NutritionResult result = NutritionService.completeMeal(
                player, event.getItem(), before.foodLevel(), before.saturation());
        if (!result.food()) return;
        int percent = (int) Math.round(result.efficiency() * 100.0);
        PacketDistributor.sendToPlayer(player, new NutritionResultPayload(
                NetworkRegistration.PROTOCOL_VERSION, percent, result.efficiency() < 0.999));
    }

    @SubscribeEvent
    public static void onUseStop(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity() instanceof ServerPlayer player) SNAPSHOTS.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.isUsingItem()) {
            SNAPSHOTS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SNAPSHOTS.remove(event.getEntity().getUUID());
    }

    private record MealSnapshot(
            int foodLevel, float saturation, InteractionHand hand, net.minecraft.world.item.Item item) {}
}
