package dev.upiscium.frontierprotocol.datagen;

import com.drmangotea.tfmg.registry.TFMGFluids;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.DeployingRecipeGen;
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeBuilder;
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeGen;
import com.simibubi.create.api.data.recipe.MixingRecipeGen;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.registry.ModItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public final class FrontierProtocolRecipeProvider {
    private FrontierProtocolRecipeProvider() {}

    public static final class Mixing extends MixingRecipeGen {
        public Mixing(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, FrontierProtocolMod.MOD_ID);
            create("stabilization_compound", builder -> builder
                    .require(TFMGFluids.MOLTEN_PLASTIC.get(), 100)
                    .require(Items.SAND)
                    .require(Items.BLUE_ICE)
                    .require(Items.IRON_NUGGET)
                    .require(Items.IRON_NUGGET)
                    .require(Items.IRON_NUGGET)
                    .require(Items.IRON_NUGGET)
                    .require(Items.IRON_NUGGET)
                    .require(Items.IRON_NUGGET)
                    .require(Items.IRON_NUGGET)
                    .require(Items.IRON_NUGGET)
                    .output(ModItems.STABILIZATION_COMPOUND.get()));
        }
    }

    public static final class Deploying extends DeployingRecipeGen {
        public Deploying(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, FrontierProtocolMod.MOD_ID);
            create("stabilization_cell", builder -> builder
                    .require(AllItems.IRON_SHEET.get())
                    .require(ModItems.STABILIZATION_COMPOUND.get())
                    .output(ModItems.STABILIZATION_CELL.get()));
        }
    }

    public static final class MechanicalCrafting extends MechanicalCraftingRecipeGen {
        public MechanicalCrafting(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, FrontierProtocolMod.MOD_ID);
            register(recipeOutput -> {
                MechanicalCraftingRecipeBuilder.shapedRecipe(ModItems.TIER_1_STABILIZER.get())
                        .key('I', AllItems.IRON_SHEET.get())
                        .key('C', ModItems.STABILIZATION_CELL.get())
                        .key('A', AllBlocks.ANDESITE_CASING.get())
                        .key('P', AllItems.PRECISION_MECHANISM.get())
                        .key('S', AllBlocks.SHAFT.get())
                        .patternLine("ICI")
                        .patternLine("APA")
                        .patternLine("ISI")
                        .build(
                                recipeOutput,
                                ResourceLocation.fromNamespaceAndPath(
                                        FrontierProtocolMod.MOD_ID,
                                        "mechanical_crafting/tier_1_stabilizer"));
                MechanicalCraftingRecipeBuilder.shapedRecipe(ModItems.TIER_2_STABILIZER.get())
                        .key('S', AllItems.STURDY_SHEET.get())
                        .key('C', ModItems.STABILIZATION_CELL.get())
                        .key('P', AllItems.PRECISION_MECHANISM.get())
                        .key('1', ModItems.TIER_1_STABILIZER.get())
                        .key('B', AllBlocks.BRASS_CASING.get())
                        .patternLine("SCS")
                        .patternLine("P1P")
                        .patternLine("SBS")
                        .build(
                                recipeOutput,
                                ResourceLocation.fromNamespaceAndPath(
                                        FrontierProtocolMod.MOD_ID,
                                        "mechanical_crafting/tier_2_stabilizer"));
                MechanicalCraftingRecipeBuilder.shapedRecipe(ModItems.TIER_3_STABILIZER.get())
                        .key('S', AllItems.STURDY_SHEET.get())
                        .key('C', ModItems.STABILIZATION_CELL.get())
                        .key('R', AllBlocks.RAILWAY_CASING.get())
                        .key('P', AllItems.PRECISION_MECHANISM.get())
                        .key('2', ModItems.TIER_2_STABILIZER.get())
                        .patternLine("SSCSS")
                        .patternLine("SRPRS")
                        .patternLine("CP2PC")
                        .patternLine("SRPRS")
                        .patternLine("SSCSS")
                        .build(
                                recipeOutput,
                                ResourceLocation.fromNamespaceAndPath(
                                        FrontierProtocolMod.MOD_ID,
                                        "mechanical_crafting/tier_3_stabilizer"));
            });
        }
    }
}
