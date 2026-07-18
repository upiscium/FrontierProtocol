package dev.upiscium.frontierprotocol.nutrition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class NutritionServiceTest {
    private static final ResourceLocation APPLE = ResourceLocation.withDefaultNamespace("apple");
    private static final ResourceLocation CARROT = ResourceLocation.withDefaultNamespace("carrot");
    private static final ResourceLocation FRUIT = ResourceLocation.fromNamespaceAndPath("frontier_protocol", "food_categories/fruit");

    @Test
    void repeatedItemUsesConfiguredEfficiencyTable() {
        List<Double> table = List.of(1.0, 0.85, 0.65, 0.45, 0.25);
        List<NutritionEntry> history = new ArrayList<>();
        for (double expected : table) {
            assertEquals(expected, NutritionService.efficiency(
                    history, APPLE, Optional.of(FRUIT), table, 6, 0.8, 0.25), 0.0001);
            history.add(new NutritionEntry(APPLE, Optional.of(FRUIT)));
        }
        assertEquals(0.25, NutritionService.efficiency(
                history, APPLE, Optional.of(FRUIT), table, 6, 0.8, 0.25), 0.0001);
    }

    @Test
    void categoryPenaltyAndMinimumAreApplied() {
        List<NutritionEntry> history = java.util.stream.IntStream.range(0, 6)
                .mapToObj(index -> new NutritionEntry(
                        ResourceLocation.fromNamespaceAndPath("test", "fruit_" + index), Optional.of(FRUIT)))
                .toList();
        assertEquals(0.8, NutritionService.efficiency(
                history, CARROT, Optional.of(FRUIT), List.of(1.0, 0.85, 0.65, 0.45, 0.25), 6, 0.8, 0.25), 0.0001);
        assertEquals(0.25, NutritionService.efficiency(
                history, history.get(0).item(), Optional.of(FRUIT), List.of(0.25), 6, 0.8, 0.25), 0.0001);
    }

    @Test
    void adjustedGainUsesActualVanillaDeltaAndNeverDropsBelowBefore() {
        assertEquals(new NutritionService.AdjustedFood(11, 1.6F),
                NutritionService.adjustGains(10, 1.0F, 14, 3.4F, 0.25));
        assertEquals(new NutritionService.AdjustedFood(19, 1.6F),
                NutritionService.adjustGains(18, 1.0F, 20, 3.4F, 0.25));
        assertEquals(new NutritionService.AdjustedFood(10, 1.0F),
                NutritionService.adjustGains(10, 1.0F, 9, 0.5F, 0.25));
    }

    @Test
    void historyIsBoundedAndDifferentMealsDisplaceOldEntries() {
        FoodHistoryState history = FoodHistoryState.EMPTY;
        for (int index = 0; index < 10; index++) {
            history = history.append(new NutritionEntry(
                    ResourceLocation.fromNamespaceAndPath("test", "meal_" + index), Optional.empty()), 8);
        }
        assertEquals(8, history.entries().size());
        assertEquals("meal_2", history.entries().getFirst().item().getPath());
        assertEquals("meal_9", history.entries().getLast().item().getPath());
    }
}
