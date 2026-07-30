package dev.upiscium.frontierprotocol.stabilizer;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.registry.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StabilizerInstrumentationGameTests {
    private StabilizerInstrumentationGameTests() {}

    @GameTest(template = "empty", batch = "stabilizer_comparator_levels", timeoutTicks = 100)
    public static void allTiersExposeExactEmptyPartialAndFullSignals(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        List<Device> devices = List.of(
                new Device(StabilizerTier.TIER_1, origin, 8),
                new Device(StabilizerTier.TIER_2, origin.offset(0, 0, 3), 32),
                new Device(StabilizerTier.TIER_3, origin.offset(0, 0, 6), 64));
        CapacitySnapshot original = CapacitySnapshot.capture();
        try {
            original.setDefaults();
            for (Device device : devices) {
                level.setBlock(device.pos(), block(device.tier()).defaultBlockState(), Block.UPDATE_ALL);
                BlockState state = level.getBlockState(device.pos());
                helper.assertTrue(state.hasAnalogOutputSignal(), device.tier() + " does not expose analog output");
                helper.assertTrue(!state.isSignalSource(), device.tier() + " emits direct redstone power");
                assertSignal(helper, level, device.pos(), 0, "empty " + device.tier());

                insertCells(level, device.pos(), device.capacity() / 2, helper);
                assertSignal(helper, level, device.pos(), 8, "half-full " + device.tier());

                insertCells(level, device.pos(), device.capacity() / 2, helper);
                assertSignal(helper, level, device.pos(), 15, "full " + device.tier());
            }
        } finally {
            for (Device device : devices) level.setBlock(device.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            original.restore();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "stabilizer_comparator_updates", timeoutTicks = 180)
    public static void comparatorTracksInsertionConsumptionConfigAndOverCapacityReload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        BlockPos stabilizerPos = helper.absolutePos(new BlockPos(4, 2, 4));
        BlockPos comparatorPos = stabilizerPos.east();
        BlockPos motorPos = stabilizerPos.west();
        InstrumentationContext context = new InstrumentationContext(
                helper,
                level,
                stabilizerPos,
                comparatorPos,
                motorPos,
                FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.get(),
                FrontierProtocolServerConfig.TIER1_CELL_CAPACITY.get(),
                FrontierProtocolServerConfig.TIER1_CELL_DURATION_TICKS.get(),
                FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.get());
        try {
            FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.set(8);
            FrontierProtocolServerConfig.TIER1_CELL_CAPACITY.set(8);
            FrontierProtocolServerConfig.TIER1_CELL_DURATION_TICKS.set(100);
            FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.set(0);
            level.setBlock(
                    stabilizerPos,
                    ModBlocks.TIER_1_STABILIZER
                            .get()
                            .defaultBlockState()
                            .setValue(StabilizerBlock.FACING, Direction.NORTH),
                    Block.UPDATE_ALL);
            level.setBlock(comparatorPos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(
                    comparatorPos,
                    Blocks.COMPARATOR
                            .defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST),
                    Block.UPDATE_ALL);
            helper.runAfterDelay(4, () -> verifyEmptyAndInsert(context));
        } catch (RuntimeException error) {
            cleanup(context);
            throw error;
        }
    }

    private static void verifyEmptyAndInsert(InstrumentationContext context) {
        runStage(context, () -> {
            assertComparator(context, 0, "empty buffer");
            insertCells(context.level(), context.stabilizerPos(), 8, context.helper());
            context.helper().runAfterDelay(4, () -> verifyExternalInsertion(context));
        });
    }

    private static void verifyExternalInsertion(InstrumentationContext context) {
        runStage(context, () -> {
            assertComparator(context, 15, "external insertion");
            context.level().setBlock(
                    context.motorPos(),
                    AllBlocks.CREATIVE_MOTOR
                            .getDefaultState()
                            .setValue(CreativeMotorBlock.FACING, Direction.EAST),
                    Block.UPDATE_ALL);
            context.helper().runAfterDelay(10, () -> verifyAutomaticConsumption(context));
        });
    }

    private static void verifyAutomaticConsumption(InstrumentationContext context) {
        runStage(context, () -> {
            StabilizerBlockEntity blockEntity = blockEntity(context.level(), context.stabilizerPos());
            int storedCells = blockEntity.externalInventory().getStackInSlot(0).getCount();
            context.helper().assertTrue(
                    storedCells == 7,
                    "automatic consumption left " + storedCells + " Cells at speed " + blockEntity.getSpeed()
                            + " and status " + blockEntity.status());
            assertComparator(context, 13, "automatic Cell consumption");
            context.level().setBlock(context.motorPos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            FrontierProtocolServerConfig.TIER1_CELL_CAPACITY.set(16);
            context.helper().runAfterDelay(4, () -> verifyCapacityExpansion(context));
        });
    }

    private static void verifyCapacityExpansion(InstrumentationContext context) {
        runStage(context, () -> {
            assertComparator(context, 7, "capacity expansion");
            FrontierProtocolServerConfig.TIER1_CELL_CAPACITY.set(4);
            context.helper().runAfterDelay(4, () -> reloadOverCapacity(context));
        });
    }

    private static void reloadOverCapacity(InstrumentationContext context) {
        runStage(context, () -> {
            assertComparator(context, 15, "capacity reduction");
            StabilizerBlockEntity old = blockEntity(context.level(), context.stabilizerPos());
            CompoundTag saved = old.saveWithFullMetadata(context.level().registryAccess());
            BlockState state = context.level().getBlockState(context.stabilizerPos());
            context.level().removeBlockEntity(context.stabilizerPos());
            StabilizerBlockEntity reloaded = new StabilizerBlockEntity(context.stabilizerPos(), state);
            reloaded.loadWithComponents(saved, context.level().registryAccess());
            context.level().setBlockEntity(reloaded);
            context.helper().assertTrue(
                    reloaded.externalInventory().getStackInSlot(0).getCount() == 7,
                    "NBT reload did not preserve over-capacity inventory");
            context.helper().runAfterDelay(4, () -> verifyOverCapacityReload(context));
        });
    }

    private static void verifyOverCapacityReload(InstrumentationContext context) {
        runStage(context, () -> {
            assertComparator(context, 15, "over-capacity NBT reload");
            cleanup(context);
            context.helper().succeed();
        });
    }

    private static void assertSignal(
            GameTestHelper helper, ServerLevel level, BlockPos pos, int expected, String stage) {
        int actual = level.getBlockState(pos).getAnalogOutputSignal(level, pos);
        helper.assertTrue(actual == expected, stage + " signal was " + actual + " instead of " + expected);
    }

    private static void assertComparator(InstrumentationContext context, int expected, String stage) {
        if (!(context.level().getBlockEntity(context.comparatorPos()) instanceof ComparatorBlockEntity comparator)) {
            throw new IllegalStateException("Comparator Block Entity missing at " + context.comparatorPos());
        }
        int actual = comparator.getOutputSignal();
        context.helper().assertTrue(
                actual == expected, stage + " comparator output was " + actual + " instead of " + expected);
    }

    private static void insertCells(ServerLevel level, BlockPos pos, int count, GameTestHelper helper) {
        IItemHandler inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        helper.assertTrue(inventory != null, "Stabilizer item capability unavailable");
        ItemStack remainder = inventory.insertItem(0, new ItemStack(ModItems.STABILIZATION_CELL.get(), count), false);
        helper.assertTrue(remainder.isEmpty(), "Stabilizer rejected instrumentation test Cells");
    }

    private static StabilizerBlockEntity blockEntity(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof StabilizerBlockEntity blockEntity) return blockEntity;
        throw new IllegalStateException("Stabilizer Block Entity missing at " + pos);
    }

    private static StabilizerBlock block(StabilizerTier tier) {
        return switch (tier) {
            case TIER_1 -> ModBlocks.TIER_1_STABILIZER.get();
            case TIER_2 -> ModBlocks.TIER_2_STABILIZER.get();
            case TIER_3 -> ModBlocks.TIER_3_STABILIZER.get();
        };
    }

    private static void runStage(InstrumentationContext context, Runnable stage) {
        try {
            stage.run();
        } catch (RuntimeException error) {
            cleanup(context);
            throw error;
        }
    }

    private static void cleanup(InstrumentationContext context) {
        context.level().setBlock(context.motorPos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        context.level().setBlock(context.comparatorPos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        context.level().setBlock(context.comparatorPos().below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        context.level().setBlock(context.stabilizerPos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.set(context.originalMinimumRpm());
        FrontierProtocolServerConfig.TIER1_CELL_CAPACITY.set(context.originalCapacity());
        FrontierProtocolServerConfig.TIER1_CELL_DURATION_TICKS.set(context.originalDuration());
        FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.set(context.originalGrace());
    }

    private record Device(StabilizerTier tier, BlockPos pos, int capacity) {}

    private record CapacitySnapshot(int tier1, int tier2, int tier3) {
        private static CapacitySnapshot capture() {
            return new CapacitySnapshot(
                    FrontierProtocolServerConfig.TIER1_CELL_CAPACITY.get(),
                    FrontierProtocolServerConfig.TIER2_CELL_CAPACITY.get(),
                    FrontierProtocolServerConfig.TIER3_CELL_CAPACITY.get());
        }

        private void setDefaults() {
            FrontierProtocolServerConfig.TIER1_CELL_CAPACITY.set(8);
            FrontierProtocolServerConfig.TIER2_CELL_CAPACITY.set(32);
            FrontierProtocolServerConfig.TIER3_CELL_CAPACITY.set(64);
        }

        private void restore() {
            FrontierProtocolServerConfig.TIER1_CELL_CAPACITY.set(tier1);
            FrontierProtocolServerConfig.TIER2_CELL_CAPACITY.set(tier2);
            FrontierProtocolServerConfig.TIER3_CELL_CAPACITY.set(tier3);
        }
    }

    private record InstrumentationContext(
            GameTestHelper helper,
            ServerLevel level,
            BlockPos stabilizerPos,
            BlockPos comparatorPos,
            BlockPos motorPos,
            int originalMinimumRpm,
            int originalCapacity,
            int originalDuration,
            int originalGrace) {}
}
