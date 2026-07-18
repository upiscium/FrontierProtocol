package dev.upiscium.frontierprotocol.nutrition;

import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import dev.upiscium.frontierprotocol.registry.ModAttachments;

public final class NutritionService {
    private NutritionService() {}

    public static double efficiency(
            List<NutritionEntry> history,
            ResourceLocation item,
            Optional<ResourceLocation> category,
            List<? extends Number> itemEfficiencies,
            int categoryRepeatThreshold,
            double categoryMultiplier,
            double minimum) {
        long itemCount = history.stream().filter(entry -> entry.item().equals(item)).count();
        int itemIndex = (int) Math.min(itemCount, itemEfficiencies.size() - 1L);
        double efficiency = itemEfficiencies.get(itemIndex).doubleValue();
        if (category.isPresent()) {
            long categoryCount = history.stream()
                    .filter(entry -> entry.category().filter(category.get()::equals).isPresent()).count();
            if (categoryCount >= categoryRepeatThreshold) efficiency *= categoryMultiplier;
        }
        return Math.max(minimum, efficiency);
    }

    public static double configuredEfficiency(FoodHistoryState history, NutritionEntry meal) {
        List<? extends Number> configured = FrontierProtocolServerConfig.NUTRITION_ITEM_EFFICIENCIES.get();
        if (configured.isEmpty()) configured = List.of(1.0, 0.85, 0.65, 0.45, 0.25);
        int historyLength = FrontierProtocolServerConfig.NUTRITION_HISTORY_LENGTH.getAsInt();
        List<NutritionEntry> entries = history.entries();
        entries = entries.subList(Math.max(0, entries.size() - historyLength), entries.size());
        return efficiency(entries, meal.item(), meal.category(), configured,
                FrontierProtocolServerConfig.NUTRITION_CATEGORY_REPEAT_THRESHOLD.getAsInt(),
                FrontierProtocolServerConfig.NUTRITION_CATEGORY_MULTIPLIER.getAsDouble(),
                FrontierProtocolServerConfig.NUTRITION_MINIMUM_EFFICIENCY.getAsDouble());
    }

    public static AdjustedFood adjustGains(
            int foodBefore, float saturationBefore, int foodAfter, float saturationAfter, double efficiency) {
        int foodGain = Math.max(0, foodAfter - foodBefore);
        float saturationGain = Math.max(0.0F, saturationAfter - saturationBefore);
        int adjustedFood = Math.max(foodBefore, Math.min(20,
                foodBefore + (int) Math.ceil(foodGain * efficiency)));
        float adjustedSaturation = Math.max(0.0F, Math.min(adjustedFood,
                saturationBefore + (float) (saturationGain * efficiency)));
        return new AdjustedFood(adjustedFood, adjustedSaturation);
    }

    public static NutritionResult completeMeal(
            ServerPlayer player, ItemStack consumed, int foodBefore, float saturationBefore) {
        if (consumed.getFoodProperties(player) == null) return NutritionResult.NOT_FOOD;
        FoodHistoryState history = player.getData(ModAttachments.FOOD_HISTORY);
        NutritionEntry meal = new NutritionEntry(
                BuiltInRegistries.ITEM.getKey(consumed.getItem()), FoodCategoryResolver.resolve(consumed));
        double efficiency = configuredEfficiency(history, meal);
        var foodData = player.getFoodData();
        AdjustedFood adjusted = adjustGains(foodBefore, saturationBefore,
                foodData.getFoodLevel(), foodData.getSaturationLevel(), efficiency);
        foodData.setFoodLevel(adjusted.foodLevel());
        foodData.setSaturation(adjusted.saturation());
        player.setData(ModAttachments.FOOD_HISTORY, history.append(
                meal, FrontierProtocolServerConfig.NUTRITION_HISTORY_LENGTH.getAsInt()));
        return new NutritionResult(true, efficiency, meal, adjusted);
    }

    public record AdjustedFood(int foodLevel, float saturation) {}

    public record NutritionResult(boolean food, double efficiency, NutritionEntry meal, AdjustedFood adjusted) {
        private static final NutritionResult NOT_FOOD = new NutritionResult(
                false, 1.0, new NutritionEntry(ResourceLocation.withDefaultNamespace("air"), Optional.empty()),
                new AdjustedFood(0, 0.0F));
    }
}
