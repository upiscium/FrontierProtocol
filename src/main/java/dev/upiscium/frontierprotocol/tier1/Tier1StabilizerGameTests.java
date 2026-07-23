package dev.upiscium.frontierprotocol.tier1;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.SporeGameTestAssertions;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceType;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.registry.ModBlockEntities;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.registry.ModItemTags;
import dev.upiscium.frontierprotocol.registry.ModItems;
import dev.upiscium.frontierprotocol.suppression.ServerInfectionSuppressionService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
        BlockPos netherDevice = new BlockPos(-1595, 64, -1595);
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
            helper.assertTrue(ModItems.STABILIZATION_CELL.isBound(), "stabilization cell is not registered");
            helper.assertTrue(
                    !BuiltInRegistries.ITEM.getKey(ModItems.STABILIZATION_COMPOUND.get())
                            .equals(BuiltInRegistries.ITEM.getKey(ModItems.STABILIZATION_CELL.get())),
                    "stabilization compound and cell share a registry ID");
            helper.assertTrue(ModItems.STABILIZATION_COMPOUND.get().getDefaultMaxStackSize() == 64,
                    "stabilization compound stack size is not 64");
            helper.assertTrue(ModItems.STABILIZATION_CELL.get().getDefaultMaxStackSize() == 64,
                    "stabilization cell stack size is not 64");
            helper.assertTrue(new ItemStack(ModItems.STABILIZATION_CELL.get()).is(ModItemTags.STABILIZER_CONSUMABLES),
                    "stabilization cell is missing from stabilizer_consumables");
            helper.assertTrue(
                    !new ItemStack(ModItems.STABILIZATION_COMPOUND.get()).is(ModItemTags.STABILIZER_CONSUMABLES),
                    "stabilization compound is unexpectedly a Stabilizer consumable");
            helper.assertTrue(ModBlockEntities.TIER_1_STABILIZER.isBound(), "Tier 1 block entity is not registered");
            helper.assertTrue(BlockStressValues.getImpact(ModBlocks.TIER_1_STABILIZER.get())
                            == FrontierProtocolServerConfig.TIER1_STRESS_IMPACT.getAsDouble(),
                    "Tier 1 stress impact does not match the current server config");

            FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.set(8);
            FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.set(10);
            FrontierProtocolServerConfig.TIER1_CONSUMABLE_DURATION_TICKS.set(100);
            placeDevice(overworld, first);
            helper.runAfterDelay(2, () -> verifyOfflineWithoutConsumable(context));
        });
    }

    @GameTest(template = "empty", batch = "tier1_cell_consumption", timeoutTicks = 200)
    public static void tierOneConsumesCellsOneAtATimeAndRejectsCompound(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        ChunkPos chunk = new ChunkPos(origin);
        BlockPos device = new BlockPos(chunk.getMinBlockX() + 8, origin.getY() + 1, chunk.getMinBlockZ() + 8);
        CellTestContext context = new CellTestContext(
                helper,
                level,
                device,
                FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.get(),
                FrontierProtocolServerConfig.TIER1_CONSUMABLE_DURATION_TICKS.get());

        runCellStage(context, () -> {
            FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.set(8);
            FrontierProtocolServerConfig.TIER1_CONSUMABLE_DURATION_TICKS.set(20);
            placeDevice(level, device);
            placeMotor(level, device.west());
            IItemHandler capability = level.getCapability(Capabilities.ItemHandler.BLOCK, device, Direction.UP);
            helper.assertTrue(capability != null, "Tier 1 item capability is unavailable");
            ItemStack compound = new ItemStack(ModItems.STABILIZATION_COMPOUND.get(), 2);
            ItemStack rejected = capability.insertItem(0, compound, false);
            helper.assertTrue(ItemStack.isSameItemSameComponents(compound, rejected) && rejected.getCount() == 2,
                    "powered Tier 1 accepted or consumed stabilization compound");
            helper.runAfterDelay(8, () -> verifyCompoundCannotActivate(context));
        });
    }

    private static void verifyCompoundCannotActivate(CellTestContext context) {
        runCellStage(context, () -> {
            Tier1StabilizerBlockEntity blockEntity = blockEntity(context.level(), context.device());
            context.helper().assertTrue(blockEntity.status() == Tier1StabilizerStatus.OFFLINE,
                    "Tier 1 became active with rotation and rejected compound only");
            context.helper().assertTrue(blockEntity.externalInventory().getStackInSlot(0).isEmpty(),
                    "rejected compound entered the Tier 1 inventory");
            insertCell(context.level(), context.device(), 2, context.helper());
            context.helper().runAfterDelay(8, () -> verifyFirstCellConsumption(context));
        });
    }

    private static void verifyFirstCellConsumption(CellTestContext context) {
        runCellStage(context, () -> {
            Tier1StabilizerBlockEntity blockEntity = blockEntity(context.level(), context.device());
            context.helper().assertTrue(blockEntity.status() == Tier1StabilizerStatus.ACTIVE,
                    "Tier 1 did not activate from an inserted stabilization cell");
            context.helper().assertTrue(blockEntity.externalInventory().getStackInSlot(0).getCount() == 1,
                    "Tier 1 did not consume exactly one stabilization cell at activation");
            int remainingTicks = blockEntity.consumableRemainingTicks();
            context.helper().assertTrue(remainingTicks > 2, "cell duration was exhausted before consumption checks");
            context.helper().runAfterDelay(2, () -> verifyNoEarlyAdditionalConsumption(context, remainingTicks));
        });
    }

    private static void verifyNoEarlyAdditionalConsumption(CellTestContext context, int remainingTicks) {
        runCellStage(context, () -> {
            context.helper().assertTrue(
                    blockEntity(context.level(), context.device()).externalInventory().getStackInSlot(0).getCount() == 1,
                    "Tier 1 consumed another cell while active duration remained");
            context.helper().runAfterDelay(
                    remainingTicks + 1, () -> verifySecondCellConsumption(context));
        });
    }

    private static void verifySecondCellConsumption(CellTestContext context) {
        runCellStage(context, () -> {
            Tier1StabilizerBlockEntity blockEntity = blockEntity(context.level(), context.device());
            context.helper().assertTrue(blockEntity.status() == Tier1StabilizerStatus.ACTIVE,
                    "Tier 1 did not remain active after consuming its next cell");
            context.helper().assertTrue(blockEntity.externalInventory().getStackInSlot(0).isEmpty(),
                    "Tier 1 did not consume exactly one next cell after duration expiry");
            context.helper().assertTrue(blockEntity.consumableRemainingTicks() > 0,
                    "next cell was consumed without starting a new active duration");
            cleanupCellTest(context);
            context.helper().succeed();
        });
    }

    private static void verifyOfflineWithoutConsumable(TestContext context) {
        runStage(context, () -> {
            Tier1StabilizerBlockEntity blockEntity = blockEntity(context.overworld(), context.first());
            context.helper().assertTrue(blockEntity.status() == Tier1StabilizerStatus.OFFLINE,
                    "unpowered Tier 1 is not offline");
            context.helper().assertTrue(!service().isSuppressed(context.overworld(), new ChunkPos(context.first())),
                    "unpowered Tier 1 registered suppression");

            rejectInvalidItem(context.overworld(), context.first(), context.helper());
            rejectCompound(context.overworld(), context.first(), context.helper());
            insertCell(context.overworld(), context.first(), 2, context.helper());
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
                    "Tier 1 did not consume exactly one cell");
            context.helper().assertTrue(service().isSuppressed(context.overworld(), chunk),
                    "active Tier 1 did not suppress its chunk");
            context.helper().assertTrue(!service().isSuppressed(context.overworld(), new ChunkPos(chunk.x + 1, chunk.z)),
                    "Tier 1 suppressed an adjacent chunk");
            SporeGameTestAssertions.assertProtoMutationBlocked(
                    context.helper(), context.overworld(), sporeTarget(context.first()),
                    "ACTIVE Tier 1 did not block Proto CDU mutation");

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
            SporeGameTestAssertions.assertProtoMutationBlocked(
                    context.helper(), context.overworld(), sporeTarget(context.first()),
                    "GRACE_PERIOD Tier 1 did not block Proto CDU mutation");
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
            SporeGameTestAssertions.assertProtoMutationAllowed(
                    context.helper(), context.overworld(), sporeTarget(context.first()),
                    "OFFLINE Tier 1 still blocked Proto CDU mutation");

            placeMotor(context.overworld(), context.first().west());
            placeDevice(context.overworld(), context.second());
            insertCell(context.overworld(), context.second(), 2, context.helper());
            placeMotor(context.overworld(), context.second().west());
            context.helper().runAfterDelay(8, () -> verifyOverlap(context));
        });
    }

    private static void verifyOverlap(TestContext context) {
        runStage(context, () -> {
            ChunkPos chunk = new ChunkPos(context.first());
            context.helper().assertTrue(tierOneSourceCount(context.overworld(), chunk) == 2,
                    "two Tier 1 devices did not register independent sources");
            SporeGameTestAssertions.assertProtoMutationBlocked(
                    context.helper(), context.overworld(), sporeTarget(context.first()),
                    "overlapping Tier 1 sources did not block Proto CDU mutation");
            blockEntity(context.overworld(), context.first()).markVirtual();
            context.helper().runAfterDelay(1, () -> verifyVirtualSourceRemoved(context));
        });
    }

    private static void verifyVirtualSourceRemoved(TestContext context) {
        runStage(context, () -> {
            ChunkPos chunk = new ChunkPos(context.first());
            context.helper().assertTrue(blockEntity(context.overworld(), context.first()).isVirtual(),
                    "Tier 1 test block entity was not marked virtual");
            context.helper().assertTrue(tierOneSourceCount(context.overworld(), chunk) == 1,
                    "virtual Tier 1 did not unregister exactly one source on its next tick");
            context.helper().assertTrue(service().getSources(context.overworld(), chunk).stream()
                            .anyMatch(source -> source.id().equals(Tier1SuppressionSource.at(context.second()).id())),
                    "virtual Tier 1 removed the neighboring device source");
            context.helper().assertTrue(service().isSuppressed(context.overworld(), chunk),
                    "virtual Tier 1 removed overlapping suppression");
            SporeGameTestAssertions.assertProtoMutationBlocked(
                    context.helper(), context.overworld(), sporeTarget(context.second()),
                    "removing one overlapping source allowed Proto CDU mutation");
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
            int remainingTicks = oldBlockEntity.consumableRemainingTicks();
            CompoundTag saved = oldBlockEntity.saveWithFullMetadata(context.overworld().registryAccess());
            oldBlockEntity.onChunkUnloaded();
            context.helper().assertTrue(!service().isSuppressed(context.overworld(), chunk),
                    "chunk unload did not unregister Tier 1 suppression");
            SporeGameTestAssertions.assertProtoMutationAllowed(
                    context.helper(), context.overworld(), sporeTarget(context.second()),
                    "removing both overlapping sources still blocked Proto CDU mutation");

            BlockState state = context.overworld().getBlockState(context.second());
            context.overworld().removeBlockEntity(context.second());
            Tier1StabilizerBlockEntity reloaded = new Tier1StabilizerBlockEntity(context.second(), state);
            reloaded.loadWithComponents(saved, context.overworld().registryAccess());
            context.overworld().setBlockEntity(reloaded);
            context.helper().assertTrue(
                    reloaded.consumableRemainingTicks() == remainingTicks,
                    "Tier 1 NBT roundtrip changed the remaining cell duration");
            context.helper().runAfterDelay(8, () -> verifyReloadAndDestroy(context));
        });
    }

    private static void verifyReloadAndDestroy(TestContext context) {
        runStage(context, () -> {
            ChunkPos chunk = new ChunkPos(context.second());
            context.helper().assertTrue(blockEntity(context.overworld(), context.second()).status()
                            == Tier1StabilizerStatus.ACTIVE,
                    "reloaded Tier 1 did not reevaluate to active");
            context.helper().assertTrue(
                    blockEntity(context.overworld(), context.second()).externalInventory().getStackInSlot(0).getCount()
                            == 1,
                    "reloaded Tier 1 did not preserve its remaining inventory");
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
            int droppedCells = context.overworld()
                    .getEntitiesOfClass(ItemEntity.class, new AABB(context.second()).inflate(2.0))
                    .stream()
                    .filter(entity -> entity.getItem().is(ModItems.STABILIZATION_CELL.get()))
                    .mapToInt(entity -> entity.getItem().getCount())
                    .sum();
            context.helper().assertTrue(droppedCells == 1, "destroyed Tier 1 did not drop its one unconsumed cell");

            ChunkPos negativeChunk = new ChunkPos(context.netherDevice());
            context.nether().getChunkSource().addRegionTicket(
                    TicketType.FORCED, negativeChunk, 2, negativeChunk, true);
            for (int x = negativeChunk.x - 1; x <= negativeChunk.x + 1; x++) {
                for (int z = negativeChunk.z - 1; z <= negativeChunk.z + 1; z++) {
                    context.nether().getChunk(x, z);
                }
            }
            context.helper().runAfterDelay(20, () -> placeNegativeNetherDevice(context));
        });
    }

    private static void placeNegativeNetherDevice(TestContext context) {
        runStage(context, () -> {
            ChunkPos negativeChunk = new ChunkPos(context.netherDevice());
            context.helper().assertTrue(context.nether().getChunkSource().isPositionTicking(negativeChunk.toLong()),
                    "negative Nether chunk did not become ticking before Tier 1 placement");
            placeDevice(context.nether(), context.netherDevice());
            insertCell(context.nether(), context.netherDevice(), 2, context.helper());
            placeMotor(context.nether(), context.netherDevice().west());
            Object motorBlockEntity = context.nether().getBlockEntity(context.netherDevice().west());
            context.helper().assertTrue(motorBlockEntity instanceof KineticBlockEntity,
                    "negative-coordinate Nether Creative Motor block entity is missing");
            context.helper().runAfterDelay(1, () -> verifyNegativeNetherDevice(context));
        });
    }

    private static void verifyNegativeNetherDevice(TestContext context) {
        runStage(context, () -> {
            ChunkPos negativeChunk = new ChunkPos(context.netherDevice());
            Tier1StabilizerBlockEntity netherBlockEntity = blockEntity(context.nether(), context.netherDevice());
            Object motorBlockEntity = context.nether().getBlockEntity(context.netherDevice().west());
            // GameTestServer does not advance newly placed block entity tickers in its remote Nether level.
            ((KineticBlockEntity) motorBlockEntity).tick();
            netherBlockEntity.tick();
            context.helper().assertTrue(netherBlockEntity.hasNetwork(),
                    "negative-coordinate Nether Tier 1 did not join a Create kinetic network: shouldTick="
                            + context.nether().shouldTickBlocksAt(negativeChunk.toLong())
                            + ", positionTicking="
                            + context.nether().getChunkSource().isPositionTicking(negativeChunk.toLong())
                            + ", deviceSpeed=" + netherBlockEntity.getSpeed()
                            + ", deviceStatus=" + netherBlockEntity.status()
                            + ", motor=" + (motorBlockEntity == null ? "null" : motorBlockEntity.getClass().getName())
                            + ", motorSpeed=" + (motorBlockEntity instanceof KineticBlockEntity kinetic
                                    ? kinetic.getSpeed()
                                    : "n/a"));
            context.helper().assertTrue(netherBlockEntity.status() == Tier1StabilizerStatus.ACTIVE,
                    "negative-coordinate Nether Tier 1 did not become active");
            context.helper().assertTrue(netherBlockEntity.externalInventory().getStackInSlot(0).getCount() == 1,
                    "negative-coordinate Nether Tier 1 did not consume exactly one cell");
            context.helper().assertTrue(service().isSuppressed(context.nether(), negativeChunk),
                    "negative-coordinate Nether Tier 1 did not suppress its placement chunk");
            context.helper().assertTrue(!service().isSuppressed(context.overworld(), negativeChunk),
                    "Nether Tier 1 suppression leaked into the Overworld");
            context.helper().assertTrue(!service().isSuppressed(
                            context.nether(), new ChunkPos(negativeChunk.x + 1, negativeChunk.z)),
                    "negative-coordinate Tier 1 suppressed an adjacent chunk");
            SporeGameTestAssertions.assertProtoMutationBlocked(
                    context.helper(), context.nether(), sporeTarget(context.netherDevice()),
                    "negative-coordinate Nether Tier 1 did not block Proto CDU mutation");
            SporeGameTestAssertions.assertProtoMutationAllowed(
                    context.helper(), context.overworld(), sporeTarget(context.netherDevice()),
                    "Nether Tier 1 blocked Proto CDU mutation in the Overworld");

            netherBlockEntity.onChunkUnloaded();
            context.helper().assertTrue(!service().isSuppressed(context.nether(), negativeChunk),
                    "unloaded negative-coordinate Nether Tier 1 retained suppression");
            context.nether().destroyBlock(context.netherDevice(), true);
            context.nether().setBlock(
                    context.netherDevice().west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            context.helper().runAfterDelay(2, () -> verifyNegativeNetherSourceRemoved(context));
        });
    }

    private static void verifyNegativeNetherSourceRemoved(TestContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(!service().isSuppressed(
                            context.nether(), new ChunkPos(context.netherDevice())),
                    "cleaned-up negative-coordinate Nether Tier 1 retained suppression");
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

    private static void insertCell(ServerLevel level, BlockPos pos, int count, GameTestHelper helper) {
        IItemHandler capability = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        helper.assertTrue(capability != null, "Tier 1 item capability is unavailable");
        ItemStack remainder = capability.insertItem(0, new ItemStack(ModItems.STABILIZATION_CELL.get(), count), false);
        helper.assertTrue(remainder.isEmpty(), "Tier 1 capability rejected stabilization cells");
        helper.assertTrue(capability.extractItem(0, 1, false).isEmpty(),
                "Tier 1 capability allowed external extraction");
    }

    private static void rejectCompound(ServerLevel level, BlockPos pos, GameTestHelper helper) {
        IItemHandler capability = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        helper.assertTrue(capability != null, "Tier 1 item capability is unavailable");
        ItemStack compound = new ItemStack(ModItems.STABILIZATION_COMPOUND.get(), 2);
        ItemStack remainder = capability.insertItem(0, compound, false);
        helper.assertTrue(ItemStack.isSameItemSameComponents(compound, remainder) && remainder.getCount() == 2,
                "Tier 1 capability accepted stabilization compound");
        helper.assertTrue(capability.getStackInSlot(0).isEmpty(),
                "rejected stabilization compound changed the Tier 1 inventory");
    }

    private static void rejectInvalidItem(ServerLevel level, BlockPos pos, GameTestHelper helper) {
        IItemHandler capability = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        helper.assertTrue(capability != null, "Tier 1 item capability is unavailable");
        ItemStack invalid = new ItemStack(Items.DIRT);
        ItemStack remainder = capability.insertItem(0, invalid, false);
        helper.assertTrue(ItemStack.isSameItemSameComponents(invalid, remainder) && remainder.getCount() == 1,
                "Tier 1 capability accepted an invalid item");
        helper.assertTrue(capability.getStackInSlot(0).isEmpty(),
                "invalid item changed the Tier 1 inventory");
    }

    private static Tier1StabilizerBlockEntity blockEntity(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof Tier1StabilizerBlockEntity blockEntity) return blockEntity;
        throw new IllegalStateException("Tier 1 block entity is missing at " + pos);
    }

    private static BlockPos sporeTarget(BlockPos device) {
        return device.above(4);
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

    private static void runCellStage(CellTestContext context, Runnable stage) {
        try {
            stage.run();
        } catch (RuntimeException error) {
            cleanupCellTest(context);
            throw error;
        }
    }

    private static void cleanup(TestContext context) {
        removeDevice(context.overworld(), context.first());
        removeDevice(context.overworld(), context.second());
        removeDevice(context.nether(), context.netherDevice());
        ChunkPos netherChunk = new ChunkPos(context.netherDevice());
        context.nether().getChunkSource().removeRegionTicket(
                TicketType.FORCED, netherChunk, 2, netherChunk, true);
        FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.set(context.originalMinimumRpm());
        FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.set(context.originalGraceTicks());
        FrontierProtocolServerConfig.TIER1_CONSUMABLE_DURATION_TICKS.set(context.originalConsumableTicks());
    }

    private static void removeDevice(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static void cleanupCellTest(CellTestContext context) {
        removeDevice(context.level(), context.device());
        FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.set(context.originalMinimumRpm());
        FrontierProtocolServerConfig.TIER1_CONSUMABLE_DURATION_TICKS.set(context.originalConsumableTicks());
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

    private record CellTestContext(
            GameTestHelper helper,
            ServerLevel level,
            BlockPos device,
            int originalMinimumRpm,
            int originalConsumableTicks) {}
}
