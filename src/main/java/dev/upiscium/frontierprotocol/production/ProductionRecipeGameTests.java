package dev.upiscium.frontierprotocol.production;

import com.Harbinger.Spore.core.Sitems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.registry.ModItems;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProductionRecipeGameTests {
    private static final ResourceLocation MIXING = id("mixing/stabilization_compound");
    private static final ResourceLocation DEPLOYING = id("deploying/stabilization_cell");
    private static final ResourceLocation MECHANICAL_CRAFTING = id("mechanical_crafting/tier_1_stabilizer");
    private static final Set<ResourceLocation> EXPECTED_RECIPES = Set.of(MIXING, DEPLOYING, MECHANICAL_CRAFTING);

    private ProductionRecipeGameTests() {}

    @GameTest(template = "empty", batch = "r6_production_recipes")
    public static void minimalCreateProductionRecipesMatchContract(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        RecipeManager recipes = level.getRecipeManager();

        Recipe<?> mixingRecipe = requireRecipe(helper, recipes, MIXING, "create:mixing");
        helper.assertTrue(mixingRecipe instanceof ProcessingRecipe<?, ?>, "mixing recipe is not a Create processing recipe");
        ProcessingRecipe<?, ?> mixing = (ProcessingRecipe<?, ?>) mixingRecipe;
        assertResult(helper, level, mixing, ModItems.STABILIZATION_COMPOUND.get(), 4, "mixing");
        helper.assertTrue(mixing.getIngredients().size() == 3, "mixing recipe does not have exactly three item inputs");
        assertExactIngredient(helper, mixing.getIngredients().get(0), Sitems.BIOMASS_BLOCK.get(), "audited Spore biomass block");
        assertExactIngredient(helper, mixing.getIngredients().get(1), Items.REDSTONE, "redstone");
        assertExactIngredient(helper, mixing.getIngredients().get(2), Items.CHARCOAL, "charcoal");
        helper.assertTrue(mixing.getFluidIngredients().size() == 1, "mixing recipe fluid input count changed");
        helper.assertTrue(
                mixing.getFluidIngredients().getFirst().amount() == 250
                        && mixing.getFluidIngredients().getFirst().test(new FluidStack(Fluids.WATER, 250)),
                "mixing recipe does not require exactly 250 mB water");
        helper.assertTrue(mixing.getRequiredHeat() == HeatCondition.HEATED,
                "mixing recipe is not heated or became superheated");

        Recipe<?> deploying = requireRecipe(helper, recipes, DEPLOYING, "create:deploying");
        helper.assertTrue(deploying instanceof ProcessingRecipe<?, ?>,
                "deploying recipe is not a Create processing recipe");
        ProcessingRecipe<?, ?> deployingProcess = (ProcessingRecipe<?, ?>) deploying;
        assertResult(helper, level, deploying, ModItems.STABILIZATION_CELL.get(), 1, "deploying");
        helper.assertTrue(deploying.getIngredients().size() == 2, "deploying recipe input count changed");
        assertExactIngredient(helper, deploying.getIngredients().get(0), AllItems.IRON_SHEET.get(), "iron sheet");
        assertExactIngredient(
                helper,
                deploying.getIngredients().get(1),
                ModItems.STABILIZATION_COMPOUND.get(),
                "stabilization compound");
        helper.assertTrue(deployingProcess.getFluidIngredients().isEmpty(),
                "deploying recipe unexpectedly requires fluid");
        helper.assertTrue(deployingProcess.getRequiredHeat() == HeatCondition.NONE,
                "deploying recipe unexpectedly requires heat");

        Recipe<?> mechanical = requireRecipe(
                helper, recipes, MECHANICAL_CRAFTING, "create:mechanical_crafting");
        assertResult(helper, level, mechanical, ModItems.TIER_1_STABILIZER.get(), 1, "mechanical crafting");
        helper.assertTrue(mechanical instanceof ShapedRecipe, "Tier 1 mechanical recipe is not shaped");
        ShapedRecipe shaped = (ShapedRecipe) mechanical;
        helper.assertTrue(shaped.getWidth() == 3 && shaped.getHeight() == 3,
                "Tier 1 mechanical pattern is not 3 by 3");
        ItemLike[] pattern = {
            AllItems.IRON_SHEET.get(),
            ModItems.STABILIZATION_CELL.get(),
            AllItems.IRON_SHEET.get(),
            AllBlocks.ANDESITE_CASING.get(),
            AllItems.PRECISION_MECHANISM.get(),
            AllBlocks.ANDESITE_CASING.get(),
            AllItems.IRON_SHEET.get(),
            AllBlocks.SHAFT.get(),
            AllItems.IRON_SHEET.get()
        };
        for (int index = 0; index < pattern.length; index++) {
            assertExactIngredient(helper, shaped.getIngredients().get(index), pattern[index], "mechanical slot " + index);
        }

        helper.assertTrue(Sitems.BIOMASS_BLOCK.get() instanceof BlockItem,
                "audited spore:biomass_block is not registered as a BlockItem");
        helper.assertTrue(
                BuiltInRegistries.ITEM.getKey(Sitems.BIOMASS_BLOCK.get())
                        .equals(ResourceLocation.fromNamespaceAndPath("spore", "biomass_block")),
                "audited Spore input has an unexpected registry ID");
        assertNoProductionBypasses(helper, level, recipes);
        helper.succeed();
    }

    private static Recipe<?> requireRecipe(
            GameTestHelper helper, RecipeManager recipes, ResourceLocation id, String serializerId) {
        RecipeHolder<?> holder = recipes.byKey(id).orElseThrow(() -> new IllegalStateException("missing recipe " + id));
        ResourceLocation actualSerializer = BuiltInRegistries.RECIPE_SERIALIZER.getKey(holder.value().getSerializer());
        helper.assertTrue(actualSerializer.toString().equals(serializerId),
                id + " uses serializer " + actualSerializer + " instead of " + serializerId);
        ResourceLocation actualType = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
        helper.assertTrue(actualType.toString().equals(serializerId),
                id + " uses recipe type " + actualType + " instead of " + serializerId);
        return holder.value();
    }

    private static void assertResult(
            GameTestHelper helper,
            ServerLevel level,
            Recipe<?> recipe,
            ItemLike expected,
            int count,
            String description) {
        ItemStack result = recipe.getResultItem(level.registryAccess());
        helper.assertTrue(result.is(expected.asItem()) && result.getCount() == count,
                description + " recipe has the wrong output: " + result);
    }

    private static void assertExactIngredient(
            GameTestHelper helper,
            net.minecraft.world.item.crafting.Ingredient ingredient,
            ItemLike expected,
            String description) {
        ItemStack[] accepted = ingredient.getItems();
        helper.assertTrue(
                accepted.length == 1 && accepted[0].is(expected.asItem()),
                description + " ingredient is missing or accepts alternate items");
    }

    private static void assertNoProductionBypasses(
            GameTestHelper helper, ServerLevel level, RecipeManager recipes) {
        long frontierRecipes = recipes.getRecipeIds()
                .filter(id -> id.getNamespace().equals(FrontierProtocolMod.MOD_ID))
                .count();
        helper.assertTrue(frontierRecipes == 3, "R6 loaded more or fewer than exactly three Frontier recipes");

        for (RecipeHolder<?> holder : recipes.getRecipes()) {
            if (!producesProductionOutput(holder.value(), level)) continue;
            helper.assertTrue(EXPECTED_RECIPES.contains(holder.id()),
                    "production output has an unapproved alternate recipe: " + holder.id());
            helper.assertTrue(holder.value().getType() != RecipeType.CRAFTING,
                    "production output has a normal crafting recipe: " + holder.id());
        }
    }

    private static boolean producesProductionOutput(Recipe<?> recipe, ServerLevel level) {
        if (isProductionOutput(recipe.getResultItem(level.registryAccess()))) return true;
        if (recipe instanceof ProcessingRecipe<?, ?> processing) {
            return processing.getRollableResultsAsItemStacks().stream()
                    .anyMatch(ProductionRecipeGameTests::isProductionOutput);
        }
        return false;
    }

    private static boolean isProductionOutput(ItemStack stack) {
        return stack.is(ModItems.STABILIZATION_COMPOUND.get())
                || stack.is(ModItems.STABILIZATION_CELL.get())
                || stack.is(ModItems.TIER_1_STABILIZER.get());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, path);
    }
}
