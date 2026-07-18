package dev.upiscium.frontierprotocol.gametest;

import com.mojang.authlib.GameProfile;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.nutrition.FoodHistoryState;
import dev.upiscium.frontierprotocol.nutrition.NutritionEntry;
import dev.upiscium.frontierprotocol.nutrition.NutritionService;
import dev.upiscium.frontierprotocol.nutrition.NutritionEventHandlers;
import dev.upiscium.frontierprotocol.registry.ModAttachments;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NutritionGameTests {
    private NutritionGameTests() {}

    @GameTest(template = "empty", batch = "nutrition")
    public static void repeatedFoodScalesActualVanillaGain(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ResourceLocation apple = ResourceLocation.withDefaultNamespace("apple");
        ResourceLocation fruit = ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, "food_categories/fruit");
        player.setData(ModAttachments.FOOD_HISTORY, new FoodHistoryState(List.of(
                new NutritionEntry(apple, Optional.of(fruit)),
                new NutritionEntry(apple, Optional.of(fruit)),
                new NutritionEntry(apple, Optional.of(fruit)),
                new NutritionEntry(apple, Optional.of(fruit)))));
        player.getFoodData().setFoodLevel(14);
        player.getFoodData().setSaturation(3.4F);

        NutritionService.NutritionResult result = NutritionService.completeMeal(
                player, new ItemStack(Items.APPLE), 10, 1.0F);

        helper.assertTrue(result.efficiency() == 0.25, "four repeated apples did not reach minimum efficiency");
        helper.assertTrue(player.getFoodData().getFoodLevel() == 11, "actual food gain was not scaled to 25 percent");
        helper.assertTrue(Math.abs(player.getFoodData().getSaturationLevel() - 1.6F) < 0.001F,
                "actual saturation gain was not scaled to 25 percent");
        helper.assertTrue(player.getData(ModAttachments.FOOD_HISTORY).entries().size() == 5,
                "completed meal was not appended after adjustment");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "nutrition")
    public static void differentFoodRestoresEfficiency(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ResourceLocation apple = ResourceLocation.withDefaultNamespace("apple");
        player.setData(ModAttachments.FOOD_HISTORY, new FoodHistoryState(List.of(
                new NutritionEntry(apple, Optional.empty()), new NutritionEntry(apple, Optional.empty()),
                new NutritionEntry(apple, Optional.empty()), new NutritionEntry(apple, Optional.empty()))));
        player.getFoodData().setFoodLevel(13);
        player.getFoodData().setSaturation(2.8F);

        NutritionService.NutritionResult result = NutritionService.completeMeal(
                player, new ItemStack(Items.CARROT), 10, 1.0F);

        helper.assertTrue(result.efficiency() == 1.0, "different food did not restore full efficiency");
        helper.assertTrue(player.getFoodData().getFoodLevel() == 13, "full-efficiency food gain was changed");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "nutrition")
    public static void startAndFinishEventsAdjustAndRecordMeal(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ItemStack apple = new ItemStack(Items.APPLE);
        player.setItemInHand(InteractionHand.MAIN_HAND, apple);
        player.getFoodData().setFoodLevel(10);
        player.getFoodData().setSaturation(1.0F);
        NutritionEventHandlers.onUseStart(new LivingEntityUseItemEvent.Start(
                player, apple, InteractionHand.MAIN_HAND, apple.getUseDuration(player)));
        player.getFoodData().setFoodLevel(14);
        player.getFoodData().setSaturation(3.4F);

        NutritionEventHandlers.onUseFinish(new LivingEntityUseItemEvent.Finish(
                player, apple.copy(), 0, ItemStack.EMPTY));

        helper.assertTrue(player.getFoodData().getFoodLevel() == 14, "first meal was incorrectly penalized");
        helper.assertTrue(player.getData(ModAttachments.FOOD_HISTORY).entries().size() == 1,
                "Finish event did not record the completed meal");
        helper.succeed();
    }

    private static FakePlayer player(GameTestHelper helper) {
        return FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "nutrition-test"));
    }
}
