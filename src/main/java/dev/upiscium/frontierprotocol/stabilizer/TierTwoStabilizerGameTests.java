package dev.upiscium.frontierprotocol.stabilizer;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.registry.ModBlockEntities;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.registry.ModItems;
import dev.upiscium.frontierprotocol.suppression.ServerInfectionSuppressionService;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TierTwoStabilizerGameTests {
    private static final ServerInfectionSuppressionService SERVICE = ServerInfectionSuppressionService.INSTANCE;

    private TierTwoStabilizerGameTests() {}

    @GameTest(template = "empty", batch = "tier2_registration", timeoutTicks = 200)
    public static void registrationCapabilityCapacityAndThreshold(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        BlockPos tier1Pos = origin.offset(4, 1, 4);
        BlockPos tier2Pos = origin.offset(8, 1, 4);
        int originalCapacity = FrontierProtocolServerConfig.TIER2_CELL_CAPACITY.get();
        try {
            helper.assertTrue(ModBlocks.TIER_2_STABILIZER.isBound(), "Tier 2 block is not registered");
            helper.assertTrue(ModItems.TIER_2_STABILIZER.isBound(), "Tier 2 BlockItem is not registered");
            helper.assertTrue(ModItems.TIER_2_STABILIZER.get().getBlock() == ModBlocks.TIER_2_STABILIZER.get(),
                    "Tier 2 BlockItem does not place the Tier 2 block");
            long modBlockEntityTypes = BuiltInRegistries.BLOCK_ENTITY_TYPE.keySet().stream()
                    .filter(id -> id.getNamespace().equals(FrontierProtocolMod.MOD_ID))
                    .count();
            helper.assertTrue(modBlockEntityTypes == 1, "expected exactly one Frontier Protocol Block Entity type");
            BlockEntityType<StabilizerBlockEntity> type = ModBlockEntities.STABILIZER.get();
            helper.assertTrue(type.isValid(ModBlocks.TIER_1_STABILIZER.get().defaultBlockState())
                            && type.isValid(ModBlocks.TIER_2_STABILIZER.get().defaultBlockState()),
                    "shared Stabilizer Block Entity type does not accept both tiers");
            helper.assertTrue(BlockStressValues.getImpact(ModBlocks.TIER_2_STABILIZER.get()) == 64.0,
                    "Tier 2 stress impact is not 64");
            helper.assertTrue(!StabilizerBlockEntity.isRpmSufficient(63.99F, 64)
                            && StabilizerBlockEntity.isRpmSufficient(64.0F, 64)
                            && StabilizerBlockEntity.isRpmSufficient(-64.0F, 64),
                    "Tier 2 RPM threshold is not exactly 64 absolute RPM");

            placeDevice(level, tier1Pos, ModBlocks.TIER_1_STABILIZER.get());
            placeDevice(level, tier2Pos, ModBlocks.TIER_2_STABILIZER.get());
            helper.assertTrue(level.getBlockEntity(tier1Pos).getClass() == StabilizerBlockEntity.class
                            && level.getBlockEntity(tier2Pos).getClass() == StabilizerBlockEntity.class,
                    "Tier 1 and Tier 2 do not share the Stabilizer Block Entity class");
            IItemHandler tier1Capability = capability(level, tier1Pos, helper);
            IItemHandler tier2Capability = capability(level, tier2Pos, helper);
            helper.assertTrue(tier1Capability.getClass() == tier2Capability.getClass(),
                    "Tier 1 and Tier 2 capabilities use different handlers");

            ItemStack compound = new ItemStack(ModItems.STABILIZATION_COMPOUND.get(), 2);
            ItemStack rejected = tier2Capability.insertItem(0, compound, false);
            helper.assertTrue(rejected.getCount() == 2 && tier2Capability.getStackInSlot(0).isEmpty(),
                    "Tier 2 accepted Stabilization Compound");
            ItemStack remainder = tier2Capability.insertItem(
                    0, new ItemStack(ModItems.STABILIZATION_CELL.get(), 40), false);
            helper.assertTrue(tier2Capability.getStackInSlot(0).getCount() == 32 && remainder.getCount() == 8,
                    "Tier 2 capacity/remainder is not 32/8");
            FrontierProtocolServerConfig.TIER2_CELL_CAPACITY.set(8);
            ItemStack reducedRemainder = tier2Capability.insertItem(
                    0, new ItemStack(ModItems.STABILIZATION_CELL.get()), false);
            helper.assertTrue(tier2Capability.getStackInSlot(0).getCount() == 32 && reducedRemainder.getCount() == 1,
                    "capacity reduction did not preserve the existing Tier 2 inventory");
        } finally {
            FrontierProtocolServerConfig.TIER2_CELL_CAPACITY.set(originalCapacity);
            removeDevice(level, tier1Pos);
            removeDevice(level, tier2Pos);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "tier2_lifecycle", timeoutTicks = 240)
    public static void lifecycleCoverageOverlapReloadVirtualAndDestroy(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        ChunkPos center = new ChunkPos(origin);
        BlockPos tier2Pos = new BlockPos(center.getMinBlockX() + 5, origin.getY() + 1, center.getMinBlockZ() + 8);
        BlockPos tier1Pos = new BlockPos(center.getMinBlockX() + 11, origin.getY() + 1, center.getMinBlockZ() + 8);
        LifecycleContext context = new LifecycleContext(
                helper, level, tier2Pos, tier1Pos, ConfigSnapshot.capture());
        try {
            FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.set(8);
            FrontierProtocolServerConfig.TIER1_CELL_DURATION_TICKS.set(100);
            FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.set(8);
            FrontierProtocolServerConfig.TIER2_CELL_DURATION_TICKS.set(100);
            FrontierProtocolServerConfig.TIER2_GRACE_PERIOD_TICKS.set(6);
            placeDevice(level, tier2Pos, ModBlocks.TIER_2_STABILIZER.get());
            insertCells(level, tier2Pos, 2, helper);
            placeMotor(level, tier2Pos.west());
            helper.runAfterDelay(8, () -> verifyTier2Active(context));
        } catch (RuntimeException error) {
            cleanup(context);
            throw error;
        }
    }

    private static void verifyTier2Active(LifecycleContext context) {
        runStage(context, () -> {
            StabilizerBlockEntity tier2 = blockEntity(context.level(), context.tier2Pos());
            ChunkPos center = new ChunkPos(context.tier2Pos());
            context.helper().assertTrue(tier2.status() == StabilizerStatus.ACTIVE,
                    "Tier 2 did not activate from a Cell and Create rotation");
            context.helper().assertTrue(tier2.externalInventory().getStackInSlot(0).getCount() == 1,
                    "Tier 2 did not consume exactly one Cell");
            assertCoverage(context.helper(), context.level(), center, context.tier2Pos());
            placeDevice(context.level(), context.tier1Pos(), ModBlocks.TIER_1_STABILIZER.get());
            insertCells(context.level(), context.tier1Pos(), 1, context.helper());
            placeMotor(context.level(), context.tier1Pos().west());
            context.helper().runAfterDelay(8, () -> verifyMixedTierOverlap(context));
        });
    }

    private static void verifyMixedTierOverlap(LifecycleContext context) {
        runStage(context, () -> {
            ChunkPos center = new ChunkPos(context.tier2Pos());
            Set<?> sources = SERVICE.getSources(context.level(), center);
            context.helper().assertTrue(sources.size() == 2, "Tier 1/Tier 2 overlap does not contain two sources");
            context.helper().assertTrue(SERVICE.getSources(context.level(), center).stream()
                            .anyMatch(source -> source.id().equals(
                                    StabilizerSuppressionSource.at(StabilizerTier.TIER_1, context.tier1Pos()).id()))
                            && SERVICE.getSources(context.level(), center).stream()
                            .anyMatch(source -> source.id().equals(
                                    StabilizerSuppressionSource.at(StabilizerTier.TIER_2, context.tier2Pos()).id())),
                    "mixed-tier overlap lost tier/position source identity");
            context.level().destroyBlock(context.tier1Pos(), false);
            context.helper().runAfterDelay(2, () -> verifyMixedTierRemoval(context));
        });
    }

    private static void verifyMixedTierRemoval(LifecycleContext context) {
        runStage(context, () -> {
            ChunkPos center = new ChunkPos(context.tier2Pos());
            context.helper().assertTrue(SERVICE.getSources(context.level(), center).size() == 1
                            && SERVICE.getSources(context.level(), center).iterator().next().id().equals(
                                    StabilizerSuppressionSource.at(StabilizerTier.TIER_2, context.tier2Pos()).id()),
                    "removing Tier 1 removed or replaced the Tier 2 overlap source");
            context.level().setBlock(context.tier2Pos().west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            context.helper().runAfterDelay(2, () -> verifyGrace(context));
        });
    }

    private static void verifyGrace(LifecycleContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(blockEntity(context.level(), context.tier2Pos()).status()
                            == StabilizerStatus.GRACE_PERIOD,
                    "Tier 2 did not enter grace after losing rotation");
            placeMotor(context.level(), context.tier2Pos().west());
            context.helper().runAfterDelay(3, () -> verifyRecovery(context));
        });
    }

    private static void verifyRecovery(LifecycleContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(blockEntity(context.level(), context.tier2Pos()).status()
                            == StabilizerStatus.ACTIVE,
                    "Tier 2 did not recover from grace");
            context.level().setBlock(context.tier2Pos().west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            context.helper().runAfterDelay(8, () -> verifyExpiryAndReactivate(context));
        });
    }

    private static void verifyExpiryAndReactivate(LifecycleContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(blockEntity(context.level(), context.tier2Pos()).status()
                            == StabilizerStatus.OFFLINE,
                    "Tier 2 grace did not expire to offline");
            context.helper().assertTrue(!SERVICE.isSuppressed(context.level(), new ChunkPos(context.tier2Pos())),
                    "expired Tier 2 retained suppression");
            placeMotor(context.level(), context.tier2Pos().west());
            context.helper().runAfterDelay(8, () -> reloadActiveTier2(context));
        });
    }

    private static void reloadActiveTier2(LifecycleContext context) {
        runStage(context, () -> {
            StabilizerBlockEntity old = blockEntity(context.level(), context.tier2Pos());
            context.helper().assertTrue(old.status() == StabilizerStatus.ACTIVE,
                    "Tier 2 did not reactivate with its remaining Cell");
            int remaining = old.cellRemainingTicks();
            CompoundTag saved = old.saveWithFullMetadata(context.level().registryAccess());
            old.onChunkUnloaded();
            context.helper().assertTrue(!SERVICE.isSuppressed(context.level(), new ChunkPos(context.tier2Pos())),
                    "unloaded Tier 2 retained suppression");
            BlockState state = context.level().getBlockState(context.tier2Pos());
            context.level().removeBlockEntity(context.tier2Pos());
            StabilizerBlockEntity reloaded = new StabilizerBlockEntity(context.tier2Pos(), state);
            reloaded.loadWithComponents(saved, context.level().registryAccess());
            context.level().setBlockEntity(reloaded);
            context.helper().assertTrue(reloaded.cellRemainingTicks() == remaining,
                    "Tier 2 NBT reload changed remaining Cell duration");
            context.helper().runAfterDelay(8, () -> verifyReloadAndVirtual(context));
        });
    }

    private static void verifyReloadAndVirtual(LifecycleContext context) {
        runStage(context, () -> {
            StabilizerBlockEntity reloaded = blockEntity(context.level(), context.tier2Pos());
            context.helper().assertTrue(reloaded.status() == StabilizerStatus.ACTIVE
                            && SERVICE.isSuppressed(context.level(), new ChunkPos(context.tier2Pos())),
                    "reloaded Tier 2 did not rebuild active suppression");
            placeDevice(context.level(), context.tier1Pos(), ModBlocks.TIER_2_STABILIZER.get());
            insertCells(context.level(), context.tier1Pos(), 1, context.helper());
            placeMotor(context.level(), context.tier1Pos().west());
            context.helper().runAfterDelay(8, () -> markSecondTier2Virtual(context));
        });
    }

    private static void markSecondTier2Virtual(LifecycleContext context) {
        runStage(context, () -> {
            StabilizerBlockEntity second = blockEntity(context.level(), context.tier1Pos());
            context.helper().assertTrue(second.status() == StabilizerStatus.ACTIVE,
                    "secondary Tier 2 did not activate for the virtual lifecycle check");
            second.markVirtual();
            context.helper().runAfterDelay(1, () -> verifyVirtualAndDestroy(context));
        });
    }

    private static void verifyVirtualAndDestroy(LifecycleContext context) {
        runStage(context, () -> {
            ChunkPos center = new ChunkPos(context.tier2Pos());
            context.helper().assertTrue(SERVICE.getSources(context.level(), center).stream()
                            .noneMatch(source -> source.id().equals(
                                    StabilizerSuppressionSource.at(StabilizerTier.TIER_2, context.tier1Pos()).id())),
                    "virtual Tier 2 retained its source");
            context.helper().assertTrue(SERVICE.getSources(context.level(), center).stream()
                            .anyMatch(source -> source.id().equals(
                                    StabilizerSuppressionSource.at(StabilizerTier.TIER_2, context.tier2Pos()).id())),
                    "virtual Tier 2 removed the non-virtual Tier 2 source");
            context.helper().assertTrue(blockEntity(context.level(), context.tier2Pos())
                            .externalInventory().getStackInSlot(0).getCount() == 1,
                    "Tier 2 did not retain one Cell before destruction");
            context.level().destroyBlock(context.tier1Pos(), false);
            blockEntity(context.level(), context.tier2Pos()).destroy();
            context.level().destroyBlock(context.tier2Pos(), true);
            context.helper().runAfterDelay(1, () -> verifyDestroyDrop(context, 0));
        });
    }

    private static void verifyDestroyDrop(LifecycleContext context, int attempts) {
        runStage(context, () -> {
            int droppedCells = context.level().getEntitiesOfClass(
                            ItemEntity.class, new AABB(context.tier2Pos()).inflate(2.0)).stream()
                    .filter(entity -> entity.getItem().is(ModItems.STABILIZATION_CELL.get()))
                    .mapToInt(entity -> entity.getItem().getCount())
                    .sum();
            if (droppedCells != 1 && attempts < 20) {
                context.helper().runAfterDelay(1, () -> verifyDestroyDrop(context, attempts + 1));
                return;
            }
            context.helper().assertTrue(droppedCells == 1,
                    "destroyed Tier 2 did not drop its one unconsumed Cell");
            cleanup(context);
            context.helper().succeed();
        });
    }

    @GameTest(template = "empty", batch = "tier2_nether", timeoutTicks = 200)
    public static void negativeNetherCoverageIsDimensionIsolated(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel().getServer().overworld();
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "Nether level is unavailable");
        BlockPos pos = new BlockPos(-1595, 64, -1595);
        ChunkPos center = new ChunkPos(pos);
        int originalRpm = FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.get();
        FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.set(8);
        nether.getChunkSource().addRegionTicket(TicketType.FORCED, center, 2, center, true);
        for (int x = center.x - 1; x <= center.x + 1; x++) {
            for (int z = center.z - 1; z <= center.z + 1; z++) nether.getChunk(x, z);
        }
        helper.runAfterDelay(20, () -> {
            try {
                placeDevice(nether, pos, ModBlocks.TIER_2_STABILIZER.get());
                insertCells(nether, pos, 1, helper);
                placeMotor(nether, pos.west());
                KineticBlockEntity motor = (KineticBlockEntity) nether.getBlockEntity(pos.west());
                motor.tick();
                blockEntity(nether, pos).tick();
                assertCoverage(helper, nether, center, pos);
                helper.assertTrue(!SERVICE.isSuppressed(overworld, center),
                        "negative Nether Tier 2 suppression leaked into the Overworld");
            } finally {
                removeDevice(nether, pos);
                nether.getChunkSource().removeRegionTicket(TicketType.FORCED, center, 2, center, true);
                FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.set(originalRpm);
            }
            helper.succeed();
        });
    }

    private static void assertCoverage(
            GameTestHelper helper, ServerLevel level, ChunkPos center, BlockPos devicePos) {
        helper.assertTrue(SERVICE.getSources(level, center).size() >= 1, "Tier 2 does not cover its center");
        helper.assertTrue(SERVICE.getSources(level, new ChunkPos(center.x + 1, center.z)).size() >= 1,
                "Tier 2 does not cover an edge");
        helper.assertTrue(SERVICE.getSources(level, new ChunkPos(center.x - 1, center.z - 1)).size() >= 1,
                "Tier 2 does not cover a corner");
        helper.assertTrue(!SERVICE.isSuppressed(level, new ChunkPos(center.x + 2, center.z)),
                "Tier 2 covers a chunk outside its exact 3x3");
        long coveredByDevice = StabilizerCoverage.coveredChunks(center, 1).stream()
                .filter(chunk -> SERVICE.getSources(level, chunk).stream().anyMatch(source -> source.id().equals(
                        StabilizerSuppressionSource.at(StabilizerTier.TIER_2, devicePos).id())))
                .count();
        helper.assertTrue(coveredByDevice == 9, "Tier 2 source does not cover exactly all nine 3x3 chunks");
    }

    private static void placeDevice(ServerLevel level, BlockPos pos, StabilizerBlock block) {
        level.setBlock(pos, block.defaultBlockState()
                .setValue(StabilizerBlock.HORIZONTAL_AXIS, Direction.Axis.X), Block.UPDATE_ALL);
    }

    private static void placeMotor(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
    }

    private static void insertCells(ServerLevel level, BlockPos pos, int count, GameTestHelper helper) {
        ItemStack remainder = capability(level, pos, helper).insertItem(
                0, new ItemStack(ModItems.STABILIZATION_CELL.get(), count), false);
        helper.assertTrue(remainder.isEmpty(), "Tier 2 capability rejected Cells");
    }

    private static IItemHandler capability(ServerLevel level, BlockPos pos, GameTestHelper helper) {
        IItemHandler capability = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        helper.assertTrue(capability != null, "Stabilizer item capability is unavailable");
        return capability;
    }

    private static StabilizerBlockEntity blockEntity(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof StabilizerBlockEntity blockEntity) return blockEntity;
        throw new IllegalStateException("Stabilizer Block Entity is missing at " + pos);
    }

    private static void runStage(LifecycleContext context, Runnable stage) {
        try {
            stage.run();
        } catch (RuntimeException error) {
            cleanup(context);
            throw error;
        }
    }

    private static void cleanup(LifecycleContext context) {
        removeDevice(context.level(), context.tier1Pos());
        removeDevice(context.level(), context.tier2Pos());
        context.originalConfig().restore();
    }

    private static void removeDevice(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    private record LifecycleContext(
            GameTestHelper helper,
            ServerLevel level,
            BlockPos tier2Pos,
            BlockPos tier1Pos,
            ConfigSnapshot originalConfig) {}

    private record ConfigSnapshot(int tier1Rpm, int tier1Duration, int tier2Rpm, int tier2Duration, int tier2Grace) {
        static ConfigSnapshot capture() {
            return new ConfigSnapshot(
                    FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.get(),
                    FrontierProtocolServerConfig.TIER1_CELL_DURATION_TICKS.get(),
                    FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.get(),
                    FrontierProtocolServerConfig.TIER2_CELL_DURATION_TICKS.get(),
                    FrontierProtocolServerConfig.TIER2_GRACE_PERIOD_TICKS.get());
        }

        void restore() {
            FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.set(tier1Rpm);
            FrontierProtocolServerConfig.TIER1_CELL_DURATION_TICKS.set(tier1Duration);
            FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.set(tier2Rpm);
            FrontierProtocolServerConfig.TIER2_CELL_DURATION_TICKS.set(tier2Duration);
            FrontierProtocolServerConfig.TIER2_GRACE_PERIOD_TICKS.set(tier2Grace);
        }
    }
}
