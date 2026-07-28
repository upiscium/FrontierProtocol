package dev.upiscium.frontierprotocol.production;

import com.drmangotea.tfmg.registry.TFMGFluids;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.registry.ModItems;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProductionRecipeGameTests {
    private static final ResourceLocation MIXING = id("mixing/stabilization_compound");
    private static final ResourceLocation DEPLOYING = id("deploying/stabilization_cell");
    private static final ResourceLocation TIER_1 = id("mechanical_crafting/tier_1_stabilizer");
    private static final ResourceLocation TIER_2 = id("mechanical_crafting/tier_2_stabilizer");
    private static final ResourceLocation TIER_3 = id("mechanical_crafting/tier_3_stabilizer");
    private static final Set<ResourceLocation> EXPECTED_RECIPES = Set.of(MIXING, DEPLOYING, TIER_1, TIER_2, TIER_3);

    private ProductionRecipeGameTests() {}

    @GameTest(template = "empty", batch = "r6_production_recipes")
    public static void minimalCreateProductionRecipesMatchContract(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        RecipeManager recipes = level.getRecipeManager();

        Recipe<?> mixingRecipe = requireRecipe(helper, recipes, MIXING, "create:mixing");
        helper.assertTrue(mixingRecipe instanceof ProcessingRecipe<?, ?>, "mixing recipe is not a Create processing recipe");
        ProcessingRecipe<?, ?> mixing = (ProcessingRecipe<?, ?>) mixingRecipe;
        assertResult(helper, level, mixing, ModItems.STABILIZATION_COMPOUND.get(), 1, "mixing");
        helper.assertTrue(mixing.getIngredients().size() == 10, "mixing recipe does not have exactly ten item inputs");
        assertExactIngredient(helper, mixing.getIngredients().get(0), Items.SAND, "sand");
        assertExactIngredient(helper, mixing.getIngredients().get(1), Items.BLUE_ICE, "blue ice");
        for (int index = 2; index < 10; index++) {
            assertExactIngredient(helper, mixing.getIngredients().get(index), Items.IRON_NUGGET, "iron nugget " + (index - 1));
        }
        helper.assertTrue(mixing.getFluidIngredients().size() == 1, "mixing recipe fluid input count changed");
        helper.assertTrue(
                mixing.getFluidIngredients().getFirst().amount() == 100
                        && mixing.getFluidIngredients().getFirst().test(new FluidStack(
                                (net.minecraft.world.level.material.Fluid) TFMGFluids.MOLTEN_PLASTIC.getSource(), 100)),
                "mixing recipe does not require exactly 100 mB TFMG molten plastic");
        helper.assertTrue(mixing.getRequiredHeat() == HeatCondition.NONE,
                "mixing recipe unexpectedly requires heat");
        helper.assertTrue(
                mixing.getIngredients().stream().noneMatch(ingredient -> ingredient.test(new ItemStack(
                        BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("spore", "biomass_block"))))),
                "mixing recipe still accepts Spore biomass");
        helper.assertTrue(mixing.getIngredients().stream().noneMatch(ingredient -> ingredient.test(new ItemStack(Items.REDSTONE))),
                "mixing recipe still accepts redstone");
        helper.assertTrue(mixing.getIngredients().stream().noneMatch(ingredient -> ingredient.test(new ItemStack(Items.CHARCOAL))),
                "mixing recipe still accepts charcoal");
        helper.assertTrue(
                !mixing.getFluidIngredients().getFirst().test(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 100)),
                "mixing recipe still accepts water");

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

        assertMechanicalRecipe(
                helper,
                level,
                recipes,
                TIER_1,
                ModItems.TIER_1_STABILIZER.get(),
                new String[] {"ICI", "APA", "ISI"},
                Map.of(
                        'I', AllItems.IRON_SHEET.get(),
                        'C', ModItems.STABILIZATION_CELL.get(),
                        'A', AllBlocks.ANDESITE_CASING.get(),
                        'P', AllItems.PRECISION_MECHANISM.get(),
                        'S', AllBlocks.SHAFT.get()),
                "Tier 1");
        assertMechanicalRecipe(
                helper,
                level,
                recipes,
                TIER_2,
                ModItems.TIER_2_STABILIZER.get(),
                new String[] {"SCS", "P1P", "SBS"},
                Map.of(
                        'S', AllItems.STURDY_SHEET.get(),
                        'C', ModItems.STABILIZATION_CELL.get(),
                        'P', AllItems.PRECISION_MECHANISM.get(),
                        '1', ModItems.TIER_1_STABILIZER.get(),
                        'B', AllBlocks.BRASS_CASING.get()),
                "Tier 2");
        assertMechanicalRecipe(
                helper,
                level,
                recipes,
                TIER_3,
                ModItems.TIER_3_STABILIZER.get(),
                new String[] {"SSCSS", "SRPRS", "CP2PC", "SRPRS", "SSCSS"},
                Map.of(
                        'S', AllItems.STURDY_SHEET.get(),
                        'C', ModItems.STABILIZATION_CELL.get(),
                        'R', AllBlocks.RAILWAY_CASING.get(),
                        'P', AllItems.PRECISION_MECHANISM.get(),
                        '2', ModItems.TIER_2_STABILIZER.get()),
                "Tier 3");

        helper.assertTrue(
                BuiltInRegistries.FLUID.getKey(TFMGFluids.MOLTEN_PLASTIC.getSource())
                        .equals(ResourceLocation.fromNamespaceAndPath("tfmg", "molten_plastic")),
                "TFMG Liquid Plastic has an unexpected registry ID");
        assertNoProductionBypasses(helper, level, recipes);
        helper.succeed();
    }

    @GameTest(
            template = "r6_physical_mixing",
            batch = "r6_create_equipment",
            timeoutTicks = 400)
    public static void compoundRunsThroughPhysicalMixerAndOutputLogistics(GameTestHelper gameTestHelper) {
        CreateGameTestHelper helper = CreateGameTestHelper.of(gameTestHelper);
        BlockPos basin = findBlock(helper, AllBlocks.BASIN.get());
        IItemHandler items = helper.itemStorageAt(basin);
        helper.assertTrue(items != null, "physical Basin item storage is unavailable");
        insert(helper, items, new ItemStack(Items.SAND));
        insert(helper, items, new ItemStack(Items.BLUE_ICE));
        insert(helper, items, new ItemStack(Items.IRON_NUGGET, 8));

        IFluidHandler fluids = helper.fluidStorageAt(basin);
        helper.assertTrue(fluids != null, "physical Basin fluid storage is unavailable");
        int filled = fluids.fill(
                new FluidStack((net.minecraft.world.level.material.Fluid) TFMGFluids.MOLTEN_PLASTIC.getSource(), 100),
                IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled == 100, "physical Basin did not accept exactly 100 mB TFMG Liquid Plastic");

        helper.pullLever(new BlockPos(2, 3, 2));
        helper.succeedWhen(() -> helper.assertContainerContains(
                new BlockPos(7, 3, 1), new ItemStack(ModItems.STABILIZATION_COMPOUND.get())));
    }

    private static BlockPos findBlock(CreateGameTestHelper helper, net.minecraft.world.level.block.Block block) {
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 10, 10, 10)) {
            if (helper.getBlockState(pos).is(block)) return pos.immutable();
        }
        throw new IllegalStateException("missing physical Create block " + block);
    }

    private static void insert(GameTestHelper helper, IItemHandler handler, ItemStack stack) {
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, stack, false);
        helper.assertTrue(remainder.isEmpty(), "physical Basin rejected input " + stack);
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

    private static void assertMechanicalRecipe(
            GameTestHelper helper,
            ServerLevel level,
            RecipeManager recipes,
            ResourceLocation id,
            ItemLike output,
            String[] pattern,
            Map<Character, ItemLike> symbols,
            String description) {
        Recipe<?> recipe = requireRecipe(helper, recipes, id, "create:mechanical_crafting");
        assertResult(helper, level, recipe, output, 1, description + " mechanical crafting");
        helper.assertTrue(recipe instanceof ShapedRecipe, description + " mechanical recipe is not shaped");
        ShapedRecipe shaped = (ShapedRecipe) recipe;
        int width = pattern[0].length();
        helper.assertTrue(shaped.getWidth() == width && shaped.getHeight() == pattern.length,
                description + " mechanical pattern dimensions changed");
        helper.assertTrue(shaped.getIngredients().size() == width * pattern.length,
                description + " mechanical pattern slot count changed");
        for (int row = 0; row < pattern.length; row++) {
            helper.assertTrue(pattern[row].length() == width, description + " expected pattern row width is invalid");
            for (int column = 0; column < width; column++) {
                char symbol = pattern[row].charAt(column);
                ItemLike expected = symbols.get(symbol);
                helper.assertTrue(expected != null, description + " pattern uses an undefined symbol " + symbol);
                int slot = row * width + column;
                assertExactIngredient(
                        helper,
                        shaped.getIngredients().get(slot),
                        expected,
                        description + " symbol " + symbol + " at row " + row + ", column " + column);
            }
        }
    }

    private static void assertNoProductionBypasses(
            GameTestHelper helper, ServerLevel level, RecipeManager recipes) {
        long frontierRecipes = recipes.getRecipeIds()
                .filter(id -> id.getNamespace().equals(FrontierProtocolMod.MOD_ID))
                .count();
        helper.assertTrue(frontierRecipes == 5, "loaded more or fewer than exactly five Frontier recipes");

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
                || stack.is(ModItems.TIER_1_STABILIZER.get())
                || stack.is(ModItems.TIER_2_STABILIZER.get())
                || stack.is(ModItems.TIER_3_STABILIZER.get());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, path);
    }
}
