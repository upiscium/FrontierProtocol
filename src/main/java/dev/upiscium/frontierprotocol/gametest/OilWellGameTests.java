package dev.upiscium.frontierprotocol.gametest;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.oil.OilWellBlockEntity;
import dev.upiscium.frontierprotocol.oil.OilWellStatus;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.sector.SectorPos;
import dev.upiscium.frontierprotocol.world.FrontierProtocolWorldData;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OilWellGameTests {
    private static final ResourceLocation OIL_FIELD = id("oil_field");
    private static final ResourceLocation CRUDE_OIL = ResourceLocation.fromNamespaceAndPath("tfmg", "crude_oil");

    private OilWellGameTests() {}

    @GameTest(template = "empty", batch = "oilWellOptionalDependency")
    public static void optionalTfmgStateIsSafeAndFunctional(GameTestHelper helper) {
        OilWellBlockEntity well = placeWell(helper);
        withTrait(helper, well.getBlockPos(), OIL_FIELD, () -> tick(helper, well, 200));

        if (ModList.get().isLoaded("tfmg")) {
            helper.assertTrue(well.status() == OilWellStatus.WORKING, "TFMG oil well was not working");
            helper.assertTrue(well.tank().getFluidAmount() == 250, "TFMG oil well produced the wrong amount");
            helper.assertTrue(CRUDE_OIL.equals(well.tank().getFluid().getFluidHolder().unwrapKey().orElseThrow().location()),
                    "TFMG oil well produced the wrong fluid");
        } else {
            helper.assertTrue(well.status() == OilWellStatus.MISSING_MOD, "missing TFMG reason was not exposed");
            helper.assertTrue(well.tank().isEmpty(), "oil was produced without TFMG");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "oilWellCapabilityAndTags")
    public static void capabilityIsExtractionOnlyAndControllerIsNonMovable(GameTestHelper helper) {
        OilWellBlockEntity well = placeWell(helper);
        IFluidHandler handler = helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK, well.getBlockPos(), null);
        helper.assertTrue(handler != null, "oil well fluid capability was missing");
        helper.assertTrue(handler.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000),
                IFluidHandler.FluidAction.EXECUTE) == 0, "external fluid insertion was accepted");
        TagKey<Block> createNonMovable = TagKey.create(Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath("create", "non_movable"));
        TagKey<Block> simulatedNonMovable = TagKey.create(Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath("simulated", "non_movable"));
        helper.assertTrue(ModBlocks.OIL_WELL.get().defaultBlockState().is(createNonMovable),
                "oil well was movable by Create");
        helper.assertTrue(ModBlocks.OIL_WELL.get().defaultBlockState().is(simulatedNonMovable),
                "oil well was movable by Simulated");
        helper.succeed();
    }

    private static OilWellBlockEntity placeWell(GameTestHelper helper) {
        helper.setBlock(BlockPos.ZERO, ModBlocks.OIL_WELL.get());
        return helper.getBlockEntity(BlockPos.ZERO);
    }

    private static void tick(GameTestHelper helper, OilWellBlockEntity well, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            OilWellBlockEntity.serverTick(helper.getLevel(), well.getBlockPos(), well.getBlockState(), well);
        }
    }

    private static void withTrait(GameTestHelper helper, BlockPos pos, ResourceLocation trait, Runnable action) {
        FrontierProtocolWorldData data = FrontierProtocolWorldData.get(helper.getLevel());
        SectorPos sector = SectorPos.fromChunk(new ChunkPos(pos), data.sectorSizeChunks());
        Optional<ResourceLocation> previous = Optional.ofNullable(data.forcedTraitOverrides().get(sector));
        data.setOverride(sector, trait);
        try {
            action.run();
        } finally {
            if (previous.isPresent()) data.setOverride(sector, previous.get()); else data.clearOverride(sector);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, path);
    }
}
