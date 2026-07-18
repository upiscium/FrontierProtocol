package dev.upiscium.frontierprotocol.mob;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.registry.ModAttachments;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class MobScalingService {
    private static final int[] DEFAULT_THRESHOLDS = {0, 16, 40, 80, 128};
    public static final ResourceLocation HEALTH_MODIFIER_ID = id("distance_health");
    public static final ResourceLocation ATTACK_MODIFIER_ID = id("distance_attack");
    public static final ResourceLocation ARMOR_MODIFIER_ID = id("distance_armor");
    public static final ResourceLocation SPEED_MODIFIER_ID = id("distance_speed");

    private MobScalingService() {}

    public static int tierForDistance(long distance, int[] thresholds) {
        int tier = 0;
        for (int index = 1; index < thresholds.length; index++) {
            if (distance < thresholds[index]) break;
            tier = index;
        }
        return tier;
    }

    public static int configuredTierForDistance(long distance) {
        List<? extends Integer> values = FrontierProtocolServerConfig.MOB_TIER_DISTANCES.get();
        int[] thresholds = values.stream().mapToInt(Integer::intValue).toArray();
        if (!validThresholds(thresholds)) thresholds = DEFAULT_THRESHOLDS;
        return tierForDistance(distance, thresholds);
    }

    public static void applyIfPrepared(Mob mob) {
        if (!mob.hasData(ModAttachments.MOB_SCALING)) return;
        MobScalingState state = mob.getData(ModAttachments.MOB_SCALING);
        if (state.applied()) return;

        int tier = Math.max(0, Math.min(state.distanceTier(), tierCount() - 1));
        setMultiplier(mob.getAttribute(Attributes.MAX_HEALTH), HEALTH_MODIFIER_ID,
                valueAt(FrontierProtocolServerConfig.MOB_HEALTH_MULTIPLIERS.get(), tier) - 1.0);
        setMultiplier(mob.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_MODIFIER_ID,
                valueAt(FrontierProtocolServerConfig.MOB_ATTACK_MULTIPLIERS.get(), tier) - 1.0);
        setAddition(mob.getAttribute(Attributes.ARMOR), ARMOR_MODIFIER_ID,
                valueAt(FrontierProtocolServerConfig.MOB_ARMOR_ADDITIONS.get(), tier));
        setMultiplier(mob.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_MODIFIER_ID,
                valueAt(FrontierProtocolServerConfig.MOB_SPEED_MULTIPLIERS.get(), tier) - 1.0);
        mob.setHealth(mob.getMaxHealth());
        mob.setData(ModAttachments.MOB_SCALING, state.withApplied(true));
    }

    private static int tierCount() {
        return Math.min(FrontierProtocolServerConfig.MOB_TIER_DISTANCES.get().size(),
                Math.min(FrontierProtocolServerConfig.MOB_HEALTH_MULTIPLIERS.get().size(),
                        Math.min(FrontierProtocolServerConfig.MOB_ATTACK_MULTIPLIERS.get().size(),
                                Math.min(FrontierProtocolServerConfig.MOB_ARMOR_ADDITIONS.get().size(),
                                        FrontierProtocolServerConfig.MOB_SPEED_MULTIPLIERS.get().size()))));
    }

    private static double valueAt(List<? extends Number> values, int tier) {
        return values.get(Math.min(tier, values.size() - 1)).doubleValue();
    }

    private static boolean validThresholds(int[] thresholds) {
        if (thresholds.length == 0 || thresholds[0] != 0) return false;
        for (int index = 1; index < thresholds.length; index++) {
            if (thresholds[index] <= thresholds[index - 1]) return false;
        }
        return true;
    }

    private static void setMultiplier(AttributeInstance attribute, ResourceLocation id, double amount) {
        if (attribute == null) return;
        attribute.addOrReplacePermanentModifier(new AttributeModifier(
                id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void setAddition(AttributeInstance attribute, ResourceLocation id, double amount) {
        if (attribute == null) return;
        attribute.addOrReplacePermanentModifier(new AttributeModifier(
                id, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, path);
    }
}
