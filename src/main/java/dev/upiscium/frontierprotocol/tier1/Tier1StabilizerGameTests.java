package dev.upiscium.frontierprotocol.tier1;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceType;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.registry.ModBlockEntities;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.registry.ModItems;
import dev.upiscium.frontierprotocol.suppression.ServerInfectionSuppressionService;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class Tier1StabilizerGameTests {
    private Tier1StabilizerGameTests() {}

    @GameTest(template = "empty", batch = "tier1", timeoutTicks = 200)
    public static void tierOneLifecycleUsesCreateKineticsAndSuppressionCore(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel().getServer().overworld();
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "Nether level is unavailable");

        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        ChunkPos chunk = new ChunkPos(origin);
        int y = origin.getY() + 1;
        BlockPos first = new BlockPos(chunk.getMinBlockX() + 5, y, chunk.getMinBlockZ() + 5);
        BlockPos second = new BlockPos(chunk.getMinBlockX() + 10, y, chunk.getMinBlockZ() + 5);
        BlockPos netherDevice = new BlockPos(-1600, 64, -1600);
        TestContext context = new TestContext(
                helper,
                overworld,
                nether,
                first,
                second,
                netherDevice,
                FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.getAsInt(),
                FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.getAsInt(),
                FrontierProtocolServerConfig.TIER1_CONSUMABLE_DURATION_TICKS.getAsInt());

        runStage(context, () -> {
            helper.assertTrue(ModBlocks.TIER_1_STABILIZER.isBound(), "Tier 1 block is not registered");
            helper.assertTrue(ModItems.TIER_1_STABILIZER.isBound(), "Tier 1 block item is not registered");
            helper.assertTrue(ModItems.STABILIZATION_COMPOUND.isBound(), "stabilization compound is not registered");
            helper.assertTrue(ModBlockEntities.TIER_1_STABILIZER.isBound(), "Tier 1 block entity is not registered");

            FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.set(8);
            FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.set(10);
            FrontierProtocolServerConfig.TIER1_CONSUMABLE_DURATION_TICKS.set(100);
            placeDevice(overworld, first);
            helper.runAfterDelay(2, () -> verifyOfflineWithoutConsumable(context));
        });
    }

    private static void verifyOfflineWithoutConsumable(TestContext context) {
        runStage(context, () -> {
            Tier1StabilizerBlockEntity blockEntity = blockEntity(context.overworld(), context.first());
            context.helper().assertTrue(blockEntity.status() == Tier1StabilizerStatus.OFFLINE,
                    "unpowered Tier 1 is not offline");
            context.helper().assertTrue(!service().isSuppressed(context.overworld(), new ChunkPos(context.first())),
                    "unpowered Tier 1 registered suppression");

            insertCompound(context.overworld(), context.first(), 2, context.helper());
            context.helper().runAfterDelay(2, () -> verifyOfflineWithoutPower(context));
        });
    }

    private static void verifyOfflineWithoutPower(TestContext context) {
        runStage(context, () -> {
            Tier1StabilizerBlockEntity blockEntity = blockEntity(context.overworld(), context.first());
            context.helper().assertTrue(blockEntity.status() == Tier1StabilizerStatus.OFFLINE,
                    "Tier 1 became active without a kinetic network");
            context.helper().assertTrue(blockEntity.externalInventory().getStackInSlot(0).getCount() == 2,
                    "Tier 1 consumed an item without power");
            placeMotor(context.overworld(), context.first().west());
            context.helper().runAfterDelay(8, () -> verifyActive(context));
        });
    }

    private static void verifyActive(TestContext context) {
        runStage(context, () -> {
            Tier1StabilizerBlockEntity blockEntity = blockEntity(context.overworld(), context.first());
            ChunkPos chunk = new ChunkPos(context.first());
            context.helper().assertTrue(blockEntity.status() == Tier1StabilizerStatus.ACTIVE,
                    "Create-powered Tier 1 did not become active");
            context.helper().assertTrue(blockEntity.externalInventory().getStackInSlot(0).getCount() == 1,
                    "Tier 1 did not consume exactly one compound");
            context.helper().assertTrue(service().isSuppressed(context.overworld(), chunk),
                    "active Tier 1 did not suppress its chunk");
            context.helper().assertTrue(!service().isSuppressed(context.overworld(), new ChunkPos(chunk.x + 1, chunk.z)),
                    "Tier 1 suppressed an adjacent chunk");

            context.overworld().setBlock(context.first().west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            context.helper().runAfterDelay(3, () -> verifyGrace(context));
        });
    }

    private static void verifyGrace(TestContext context) {
        runStage(context, () -> {
            Tier1StabilizerBlockEntity blockEntity = blockEntity(context.overworld(), context.first());
            context.helper().assertTrue(blockEntity.status() == Tier1StabilizerStatus.GRACE_PERIOD,
                    "power loss did not enter grace period");
            context.helper().assertTrue(service().isSuppressed(context.overworld(), new ChunkPos(context.first())),
                    "grace period did not retain suppression");
            placeMotor(context.overworld(), context.first().west());
            context.helper().runAfterDelay(5, () -> verifyRecovered(context));
        });
    }

    private static void verifyRecovered(TestContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(blockEntity(context.overworld(), context.first()).status()
                            == Tier1StabilizerStatus.ACTIVE,
                    "power restoration did not return Tier 1 to active");
            context.overworld().setBlock(context.first().west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            context.helper().runAfterDelay(12, () -> verifyGraceExpired(context));
        });
    }

    private static void verifyGraceExpired(TestContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(blockEntity(context.overworld(), context.first()).status()
                            == Tier1StabilizerStatus.OFFLINE,
                    "expired grace period did not become offline");
            context.helper().assertTrue(!service().isSuppressed(context.overworld(), new ChunkPos(context.first())),
                    "expired grace period retained suppression");

            placeMotor(context.overworld(), context.first().west());
            placeDevice(context.overworld(), context.second());
            insertCompound(context.overworld(), context.second(), 1, context.helper());
            placeMotor(context.overworld(), context.second().west());
            context.helper().runAfterDelay(8, () -> verifyOverlap(context));
        });
    }

    private static void verifyOverlap(TestContext context) {
        runStage(context, () -> {
            ChunkPos chunk = new ChunkPos(context.first());
            context.helper().assertTrue(tierOneSourceCount(context.overworld(), chunk) == 2,
                    "two Tier 1 devices did not register independent sources");
            context.overworld().destroyBlock(context.first(), true);
            context.helper().runAfterDelay(2, () -> verifyOneOverlapRemains(context));
        });
    }

    private static void verifyOneOverlapRemains(TestContext context) {
        runStage(context, () -> {
            ChunkPos chunk = new ChunkPos(context.second());
            context.helper().assertTrue(service().isSuppressed(context.overworld(), chunk),
                    "breaking one Tier 1 removed overlapping suppression");
            context.helper().assertTrue(tierOneSourceCount(context.overworld(), chunk) == 1,
                    "breaking one Tier 1 removed the wrong source set");

            Tier1StabilizerBlockEntity oldBlockEntity = blockEntity(context.overworld(), context.second());
            CompoundTag saved = oldBlockEntity.saveWithFullMetadata(context.overworld().registryAccess());
            oldBlockEntity.onChunkUnloaded();
            context.helper().assertTrue(!service().isSuppressed(context.overworld(), chunk),
                    "chunk unload did not unregister Tier 1 suppression");

            BlockState state = context.overworld().getBlockState(context.second());
            context.overworld().removeBlockEntity(context.second());
            Tier1StabilizerBlockEntity reloaded = new Tier1StabilizerBlockEntity(context.second(), state);
            reloaded.loadWithComponents(saved, context.overworld().registryAccess());
            context.overworld().setBlockEntity(reloaded);
            context.helper().runAfterDelay(8, () -> verifyReloadAndDestroy(context));
        });
    }

    private static void verifyReloadAndDestroy(TestContext context) {
        runStage(context, () -> {
            ChunkPos chunk = new ChunkPos(context.second());
            context.helper().assertTrue(blockEntity(context.overworld(), context.second()).status()
                            == Tier1StabilizerStatus.ACTIVE,
                    "reloaded Tier 1 did not reevaluate to active");
            context.helper().assertTrue(service().isSuppressed(context.overworld(), chunk),
                    "reloaded Tier 1 did not rebuild suppression");
            context.overworld().destroyBlock(context.second(), true);
            context.helper().runAfterDelay(2, () -> verifyDestroyed(context));
        });
    }

    private static void verifyDestroyed(TestContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(!service().isSuppressed(context.overworld(), new ChunkPos(context.second())),
                    "destroyed Tier 1 retained suppression");

            ChunkPos negativeChunk = new ChunkPos(context.netherDevice());
            service().registerOrUpdateSource(
                    context.nether(), Tier1SuppressionSource.at(context.netherDevice()), Set.of(negativeChunk));
            context.helper().runAfterDelay(1, () -> verifyNegativeNetherIsolation(context));
        });
    }

    private static void verifyNegativeNetherIsolation(TestContext context) {
        runStage(context, () -> {
            ChunkPos negativeChunk = new ChunkPos(context.netherDevice());
            context.helper().assertTrue(service().isSuppressed(context.nether(), negativeChunk),
                    "negative-coordinate Nether Tier 1 did not register suppression");
            context.helper().assertTrue(!service().isSuppressed(context.overworld(), negativeChunk),
                    "Nether Tier 1 suppression leaked into the Overworld");
            context.helper().assertTrue(!service().isSuppressed(
                            context.nether(), new ChunkPos(negativeChunk.x + 1, negativeChunk.z)),
                    "negative-coordinate Tier 1 suppressed an adjacent chunk");

            Tier1StabilizerBlockEntity virtual = new Tier1StabilizerBlockEntity(BlockPos.ZERO,
                    ModBlocks.TIER_1_STABILIZER.get().defaultBlockState());
            virtual.markVirtual();
            virtual.tick();
            cleanup(context);
            context.helper().succeed();
        });
    }

    private static void placeDevice(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, ModBlocks.TIER_1_STABILIZER.get().defaultBlockState()
                .setValue(Tier1StabilizerBlock.HORIZONTAL_AXIS, Direction.Axis.X), Block.UPDATE_ALL);
    }

    private static void placeMotor(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
    }

    private static void insertCompound(ServerLevel level, BlockPos pos, int count, GameTestHelper helper) {
        IItemHandler capability = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        helper.assertTrue(capability != null, "Tier 1 item capability is unavailable");
        ItemStack remainder = capability.insertItem(0, new ItemStack(ModItems.STABILIZATION_COMPOUND.get(), count), false);
        helper.assertTrue(remainder.isEmpty(), "Tier 1 capability rejected stabilization compound");
        helper.assertTrue(capability.extractItem(0, 1, false).isEmpty(),
                "Tier 1 capability allowed external extraction");
    }

    private static Tier1StabilizerBlockEntity blockEntity(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof Tier1StabilizerBlockEntity blockEntity) return blockEntity;
        throw new IllegalStateException("Tier 1 block entity is missing at " + pos);
    }

    private static long tierOneSourceCount(ServerLevel level, ChunkPos chunk) {
        return service().getSources(level, chunk).stream()
                .filter(source -> source.type() == SuppressionSourceType.TIER_1_STABILIZER)
                .count();
    }

    private static ServerInfectionSuppressionService service() {
        return ServerInfectionSuppressionService.INSTANCE;
    }

    private static void runStage(TestContext context, Runnable stage) {
        try {
            stage.run();
        } catch (RuntimeException error) {
            cleanup(context);
            throw error;
        }
    }

    private static void cleanup(TestContext context) {
        removeDevice(context.overworld(), context.first());
        removeDevice(context.overworld(), context.second());
        service().unregisterSource(context.nether(), Tier1SuppressionSource.at(context.netherDevice()).id());
        FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.set(context.originalMinimumRpm());
        FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.set(context.originalGraceTicks());
        FrontierProtocolServerConfig.TIER1_CONSUMABLE_DURATION_TICKS.set(context.originalConsumableTicks());
    }

    private static void removeDevice(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    private record TestContext(
            GameTestHelper helper,
            ServerLevel overworld,
            ServerLevel nether,
            BlockPos first,
            BlockPos second,
            BlockPos netherDevice,
            int originalMinimumRpm,
            int originalGraceTicks,
            int originalConsumableTicks) {}
}
