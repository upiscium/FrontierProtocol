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
public final class TierThreeStabilizerGameTests {
    private static final ServerInfectionSuppressionService SERVICE = ServerInfectionSuppressionService.INSTANCE;

    private TierThreeStabilizerGameTests() {}

    @GameTest(template = "empty", batch = "tier3_registration", timeoutTicks = 200)
    public static void registrationCapabilityCapacityAndThreshold(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        BlockPos[] positions = {origin.offset(3, 1, 3), origin.offset(6, 1, 3), origin.offset(9, 1, 3)};
        int originalCapacity = FrontierProtocolServerConfig.TIER3_CELL_CAPACITY.get();
        double originalStress = FrontierProtocolServerConfig.TIER3_STRESS_IMPACT.get();
        try {
            FrontierProtocolServerConfig.TIER3_CELL_CAPACITY.set(64);
            FrontierProtocolServerConfig.TIER3_STRESS_IMPACT.set(256.0);
            helper.assertTrue(ModBlocks.TIER_3_STABILIZER.isBound(), "Tier 3 block is not registered");
            helper.assertTrue(ModItems.TIER_3_STABILIZER.isBound()
                            && ModItems.TIER_3_STABILIZER.get().getBlock() == ModBlocks.TIER_3_STABILIZER.get(),
                    "Tier 3 BlockItem is not registered for the Tier 3 block");
            helper.assertTrue(BuiltInRegistries.BLOCK.getKey(ModBlocks.TIER_3_STABILIZER.get()).getPath()
                            .equals("tier_3_stabilizer"),
                    "Tier 3 block has the wrong Registry ID");
            helper.assertTrue(ModBlocks.TIER_3_STABILIZER.get().getClass() == StabilizerBlock.class,
                    "Tier 3 uses a tier-specific Block class");
            helper.assertTrue(ModBlocks.TIER_3_STABILIZER.get().defaultDestroyTime() == 8.0F
                            && ModBlocks.TIER_3_STABILIZER.get().getExplosionResistance() == 8.0F,
                    "Tier 3 hardness/resistance is not the moderate 8/8 override");
            long blockEntityTypes = BuiltInRegistries.BLOCK_ENTITY_TYPE.keySet().stream()
                    .filter(id -> id.getNamespace().equals(FrontierProtocolMod.MOD_ID))
                    .count();
            helper.assertTrue(blockEntityTypes == 1, "expected exactly one Frontier Protocol Block Entity type");
            BlockEntityType<StabilizerBlockEntity> type = ModBlockEntities.STABILIZER.get();
            helper.assertTrue(type.isValid(ModBlocks.TIER_1_STABILIZER.get().defaultBlockState())
                            && type.isValid(ModBlocks.TIER_2_STABILIZER.get().defaultBlockState())
                            && type.isValid(ModBlocks.TIER_3_STABILIZER.get().defaultBlockState()),
                    "shared Stabilizer Block Entity type does not accept all three tiers");
            helper.assertTrue(BlockStressValues.getImpact(ModBlocks.TIER_3_STABILIZER.get()) == 256.0,
                    "Tier 3 dynamic stress impact is not 256");
            FrontierProtocolServerConfig.TIER3_STRESS_IMPACT.set(257.0);
            helper.assertTrue(BlockStressValues.getImpact(ModBlocks.TIER_3_STABILIZER.get()) == 257.0,
                    "Tier 3 stress impact did not resolve a live config change");
            FrontierProtocolServerConfig.TIER3_STRESS_IMPACT.set(256.0);
            helper.assertTrue(!StabilizerBlockEntity.isRpmSufficient(127.99F, 128)
                            && StabilizerBlockEntity.isRpmSufficient(128.0F, 128)
                            && StabilizerBlockEntity.isRpmSufficient(-128.0F, 128),
                    "Tier 3 RPM threshold is not exactly 128 absolute RPM");

            placeDevice(level, positions[0], ModBlocks.TIER_1_STABILIZER.get());
            placeDevice(level, positions[1], ModBlocks.TIER_2_STABILIZER.get());
            placeDevice(level, positions[2], ModBlocks.TIER_3_STABILIZER.get());
            helper.assertTrue(level.getBlockEntity(positions[0]).getClass() == StabilizerBlockEntity.class
                            && level.getBlockEntity(positions[1]).getClass() == StabilizerBlockEntity.class
                            && level.getBlockEntity(positions[2]).getClass() == StabilizerBlockEntity.class,
                    "all tiers do not share one Stabilizer Block Entity class");
            IItemHandler tier1 = capability(level, positions[0], helper);
            IItemHandler tier3 = capability(level, positions[2], helper);
            helper.assertTrue(tier1.getClass() == tier3.getClass(), "Tier 3 uses a different capability handler");
            ItemStack compound = new ItemStack(ModItems.STABILIZATION_COMPOUND.get(), 2);
            helper.assertTrue(tier3.insertItem(0, compound, false).getCount() == 2
                            && tier3.getStackInSlot(0).isEmpty(),
                    "Tier 3 accepted Stabilization Compound");
            helper.assertTrue(tier3.insertItem(
                            0, new ItemStack(ModItems.STABILIZATION_CELL.get(), 60), false).isEmpty(),
                    "Tier 3 rejected valid Stabilization Cells");
            ItemStack remainder = tier3.insertItem(
                    0, new ItemStack(ModItems.STABILIZATION_CELL.get(), 10), false);
            helper.assertTrue(tier3.getStackInSlot(0).getCount() == 64 && remainder.getCount() == 6,
                    "Tier 3 capacity/remainder is not 64/6");
            FrontierProtocolServerConfig.TIER3_CELL_CAPACITY.set(8);
            ItemStack reducedRemainder = tier3.insertItem(
                    0, new ItemStack(ModItems.STABILIZATION_CELL.get()), false);
            helper.assertTrue(tier3.getStackInSlot(0).getCount() == 64 && reducedRemainder.getCount() == 1,
                    "capacity shrink did not preserve the Tier 3 inventory");
        } finally {
            FrontierProtocolServerConfig.TIER3_CELL_CAPACITY.set(originalCapacity);
            FrontierProtocolServerConfig.TIER3_STRESS_IMPACT.set(originalStress);
            for (BlockPos position : positions) removeDevice(level, position);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "tier3_lifecycle", timeoutTicks = 260)
    public static void lifecycleCoverageReloadVirtualAndDestroy(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        ChunkPos center = new ChunkPos(helper.absolutePos(BlockPos.ZERO));
        BlockPos device = new BlockPos(center.getMinBlockX() + 5, 65, center.getMinBlockZ() + 8);
        BlockPos virtual = new BlockPos(center.getMinBlockX() + 11, 65, center.getMinBlockZ() + 8);
        LifecycleContext context = new LifecycleContext(helper, level, device, virtual, ConfigSnapshot.capture());
        try {
            FrontierProtocolServerConfig.TIER3_MINIMUM_RPM.set(8);
            FrontierProtocolServerConfig.TIER3_CELL_DURATION_TICKS.set(100);
            FrontierProtocolServerConfig.TIER3_GRACE_PERIOD_TICKS.set(6);
            placeDevice(level, device, ModBlocks.TIER_3_STABILIZER.get());
            insertCells(level, device, 2, helper);
            placeMotor(level, device.west());
            helper.runAfterDelay(8, () -> verifyActive(context));
        } catch (RuntimeException error) {
            cleanup(context);
            throw error;
        }
    }

    private static void verifyActive(LifecycleContext context) {
        runStage(context, () -> {
            StabilizerBlockEntity device = blockEntity(context.level(), context.device());
            context.helper().assertTrue(device.status() == StabilizerStatus.ACTIVE
                            && device.externalInventory().getStackInSlot(0).getCount() == 1,
                    "Tier 3 did not activate and consume exactly one Cell");
            assertCoverage(context.helper(), context.level(), new ChunkPos(context.device()), context.device());
            context.level().setBlock(context.device().west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            context.helper().runAfterDelay(2, () -> verifyGrace(context));
        });
    }

    private static void verifyGrace(LifecycleContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(blockEntity(context.level(), context.device()).status()
                            == StabilizerStatus.GRACE_PERIOD,
                    "Tier 3 did not enter grace after losing rotation");
            placeMotor(context.level(), context.device().west());
            context.helper().runAfterDelay(3, () -> verifyRecovery(context));
        });
    }

    private static void verifyRecovery(LifecycleContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(blockEntity(context.level(), context.device()).status()
                            == StabilizerStatus.ACTIVE,
                    "Tier 3 did not recover from grace");
            context.level().setBlock(context.device().west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            context.helper().runAfterDelay(8, () -> verifyExpiry(context));
        });
    }

    private static void verifyExpiry(LifecycleContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(blockEntity(context.level(), context.device()).status()
                            == StabilizerStatus.OFFLINE,
                    "Tier 3 grace did not expire to offline");
            context.helper().assertTrue(!SERVICE.isSuppressed(context.level(), new ChunkPos(context.device())),
                    "expired Tier 3 retained suppression");
            placeMotor(context.level(), context.device().west());
            context.helper().runAfterDelay(8, () -> reloadActive(context));
        });
    }

    private static void reloadActive(LifecycleContext context) {
        runStage(context, () -> {
            StabilizerBlockEntity old = blockEntity(context.level(), context.device());
            context.helper().assertTrue(old.status() == StabilizerStatus.ACTIVE,
                    "Tier 3 did not reactivate with its remaining Cells");
            int remaining = old.cellRemainingTicks();
            CompoundTag saved = old.saveWithFullMetadata(context.level().registryAccess());
            old.onChunkUnloaded();
            context.helper().assertTrue(!SERVICE.isSuppressed(context.level(), new ChunkPos(context.device())),
                    "unloaded Tier 3 retained suppression");
            BlockState state = context.level().getBlockState(context.device());
            context.level().removeBlockEntity(context.device());
            StabilizerBlockEntity reloaded = new StabilizerBlockEntity(context.device(), state);
            reloaded.loadWithComponents(saved, context.level().registryAccess());
            context.level().setBlockEntity(reloaded);
            context.helper().assertTrue(reloaded.cellRemainingTicks() == remaining,
                    "Tier 3 NBT reload changed remaining Cell duration");
            context.helper().runAfterDelay(8, () -> verifyReloadAndVirtual(context));
        });
    }

    private static void verifyReloadAndVirtual(LifecycleContext context) {
        runStage(context, () -> {
            context.helper().assertTrue(blockEntity(context.level(), context.device()).status() == StabilizerStatus.ACTIVE
                            && SERVICE.isSuppressed(context.level(), new ChunkPos(context.device())),
                    "reloaded Tier 3 did not rebuild active suppression");
            placeDevice(context.level(), context.virtual(), ModBlocks.TIER_3_STABILIZER.get());
            insertCells(context.level(), context.virtual(), 1, context.helper());
            placeMotor(context.level(), context.virtual().west());
            context.helper().runAfterDelay(8, () -> markVirtual(context));
        });
    }

    private static void markVirtual(LifecycleContext context) {
        runStage(context, () -> {
            blockEntity(context.level(), context.virtual()).markVirtual();
            context.helper().runAfterDelay(1, () -> verifyVirtualAndDestroy(context));
        });
    }

    private static void verifyVirtualAndDestroy(LifecycleContext context) {
        runStage(context, () -> {
            ChunkPos center = new ChunkPos(context.device());
            context.helper().assertTrue(SERVICE.getSources(context.level(), center).stream()
                            .noneMatch(source -> source.id().equals(StabilizerSuppressionSource.at(
                                    StabilizerTier.TIER_3, context.virtual()).id())),
                    "virtual Tier 3 retained its source");
            context.helper().assertTrue(SERVICE.getSources(context.level(), center).stream()
                            .anyMatch(source -> source.id().equals(StabilizerSuppressionSource.at(
                                    StabilizerTier.TIER_3, context.device()).id())),
                    "virtual Tier 3 removed the non-virtual source");
            context.level().destroyBlock(context.virtual(), false);
            context.level().destroyBlock(context.device(), true);
            int droppedCells = context.level().getEntitiesOfClass(
                            ItemEntity.class, new AABB(context.device()).inflate(2.0)).stream()
                    .filter(entity -> entity.getItem().is(ModItems.STABILIZATION_CELL.get()))
                    .mapToInt(entity -> entity.getItem().getCount())
                    .sum();
            context.helper().assertTrue(droppedCells == 1,
                    "destroyed Tier 3 did not drop its one unconsumed Cell");
            context.helper().assertTrue(!SERVICE.isSuppressed(context.level(), center),
                    "destroyed final Tier 3 retained suppression");
            cleanup(context);
            context.helper().succeed();
        });
    }

    @GameTest(template = "empty", batch = "tier3_mixed_overlap", timeoutTicks = 220)
    public static void allMixedTierOverlapsKeepExactIndependentSources(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        ChunkPos center = new ChunkPos(helper.absolutePos(BlockPos.ZERO));
        BlockPos tier1 = new BlockPos(center.getMinBlockX() + 3, 65, center.getMinBlockZ() + 8);
        BlockPos tier2 = new BlockPos(center.getMinBlockX() + 8, 65, center.getMinBlockZ() + 8);
        BlockPos tier3 = new BlockPos(center.getMinBlockX() + 13, 65, center.getMinBlockZ() + 8);
        MixedContext context = new MixedContext(helper, level, tier1, tier2, tier3, ConfigSnapshot.capture());
        try {
            FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.set(8);
            FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.set(8);
            FrontierProtocolServerConfig.TIER3_MINIMUM_RPM.set(8);
            FrontierProtocolServerConfig.TIER1_CELL_DURATION_TICKS.set(100);
            FrontierProtocolServerConfig.TIER2_CELL_DURATION_TICKS.set(100);
            FrontierProtocolServerConfig.TIER3_CELL_DURATION_TICKS.set(100);
            placeAndPower(level, tier1, ModBlocks.TIER_1_STABILIZER.get(), helper);
            placeAndPower(level, tier3, ModBlocks.TIER_3_STABILIZER.get(), helper);
            helper.runAfterDelay(8, () -> verifyTierOneAndThree(context));
        } catch (RuntimeException error) {
            cleanup(context);
            throw error;
        }
    }

    private static void verifyTierOneAndThree(MixedContext context) {
        runStage(context, () -> {
            assertExactSources(context.helper(), context.level(), new ChunkPos(context.tier3()), Set.of(
                    StabilizerSuppressionSource.at(StabilizerTier.TIER_1, context.tier1()).id(),
                    StabilizerSuppressionSource.at(StabilizerTier.TIER_3, context.tier3()).id()));
            placeAndPower(context.level(), context.tier2(), ModBlocks.TIER_2_STABILIZER.get(), context.helper());
            context.helper().runAfterDelay(8, () -> verifyAllThree(context));
        });
    }

    private static void verifyAllThree(MixedContext context) {
        runStage(context, () -> {
            assertExactSources(context.helper(), context.level(), new ChunkPos(context.tier3()), Set.of(
                    StabilizerSuppressionSource.at(StabilizerTier.TIER_1, context.tier1()).id(),
                    StabilizerSuppressionSource.at(StabilizerTier.TIER_2, context.tier2()).id(),
                    StabilizerSuppressionSource.at(StabilizerTier.TIER_3, context.tier3()).id()));
            removeDevice(context.level(), context.tier1());
            context.helper().runAfterDelay(2, () -> verifyTierTwoAndThree(context));
        });
    }

    private static void verifyTierTwoAndThree(MixedContext context) {
        runStage(context, () -> {
            assertExactSources(context.helper(), context.level(), new ChunkPos(context.tier3()), Set.of(
                    StabilizerSuppressionSource.at(StabilizerTier.TIER_2, context.tier2()).id(),
                    StabilizerSuppressionSource.at(StabilizerTier.TIER_3, context.tier3()).id()));
            removeDevice(context.level(), context.tier2());
            context.helper().runAfterDelay(2, () -> verifyOnlyTierThree(context));
        });
    }

    private static void verifyOnlyTierThree(MixedContext context) {
        runStage(context, () -> {
            assertExactSources(context.helper(), context.level(), new ChunkPos(context.tier3()), Set.of(
                    StabilizerSuppressionSource.at(StabilizerTier.TIER_3, context.tier3()).id()));
            removeDevice(context.level(), context.tier3());
            context.helper().runAfterDelay(2, () -> {
                try {
                    context.helper().assertTrue(!SERVICE.isSuppressed(context.level(), new ChunkPos(context.tier3())),
                            "last mixed-tier source did not release suppression");
                } finally {
                    cleanup(context);
                }
                context.helper().succeed();
            });
        });
    }

    @GameTest(template = "empty", batch = "tier3_nether", timeoutTicks = 200)
    public static void negativeNetherCoverageIsDimensionIsolated(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel().getServer().overworld();
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "Nether level is unavailable");
        BlockPos position = new BlockPos(-1595, 64, -1595);
        ChunkPos center = new ChunkPos(position);
        int originalRpm = FrontierProtocolServerConfig.TIER3_MINIMUM_RPM.get();
        try {
            FrontierProtocolServerConfig.TIER3_MINIMUM_RPM.set(8);
            nether.getChunkSource().addRegionTicket(TicketType.FORCED, center, 3, center, true);
            for (int x = center.x - 2; x <= center.x + 2; x++) {
                for (int z = center.z - 2; z <= center.z + 2; z++) nether.getChunk(x, z);
            }
        } catch (RuntimeException error) {
            removeDevice(nether, position);
            nether.getChunkSource().removeRegionTicket(TicketType.FORCED, center, 3, center, true);
            FrontierProtocolServerConfig.TIER3_MINIMUM_RPM.set(originalRpm);
            throw error;
        }
        helper.runAfterDelay(20, () -> {
            try {
                placeDevice(nether, position, ModBlocks.TIER_3_STABILIZER.get());
                insertCells(nether, position, 1, helper);
                placeMotor(nether, position.west());
                ((KineticBlockEntity) nether.getBlockEntity(position.west())).tick();
                blockEntity(nether, position).tick();
                assertCoverage(helper, nether, center, position);
                helper.assertTrue(!SERVICE.isSuppressed(overworld, center),
                        "negative Nether Tier 3 suppression leaked into the Overworld");
            } finally {
                removeDevice(nether, position);
                nether.getChunkSource().removeRegionTicket(TicketType.FORCED, center, 3, center, true);
                FrontierProtocolServerConfig.TIER3_MINIMUM_RPM.set(originalRpm);
            }
            helper.succeed();
        });
    }

    private static void assertCoverage(
            GameTestHelper helper, ServerLevel level, ChunkPos center, BlockPos device) {
        helper.assertTrue(SERVICE.isSuppressed(level, center), "Tier 3 does not cover its center");
        helper.assertTrue(SERVICE.isSuppressed(level, new ChunkPos(center.x + 2, center.z)),
                "Tier 3 does not cover a radius-two edge");
        helper.assertTrue(SERVICE.isSuppressed(level, new ChunkPos(center.x - 2, center.z - 2)),
                "Tier 3 does not cover a radius-two corner");
        helper.assertTrue(!SERVICE.isSuppressed(level, new ChunkPos(center.x + 3, center.z)),
                "Tier 3 covers a chunk outside its exact 5x5");
        long covered = StabilizerCoverage.coveredChunks(center, 2).stream()
                .filter(chunk -> SERVICE.getSources(level, chunk).stream().anyMatch(source -> source.id().equals(
                        StabilizerSuppressionSource.at(StabilizerTier.TIER_3, device).id())))
                .count();
        helper.assertTrue(covered == 25, "Tier 3 source does not cover exactly 25 chunks");
    }

    private static void assertExactSources(
            GameTestHelper helper, ServerLevel level, ChunkPos chunk, Set<?> expectedIds) {
        Set<?> actualIds = SERVICE.getSources(level, chunk).stream().map(source -> source.id()).collect(java.util.stream.Collectors.toSet());
        helper.assertTrue(actualIds.equals(expectedIds),
                "mixed-tier source IDs/count differ: expected=" + expectedIds + ", actual=" + actualIds);
    }

    private static void placeAndPower(
            ServerLevel level, BlockPos position, StabilizerBlock block, GameTestHelper helper) {
        placeDevice(level, position, block);
        insertCells(level, position, 1, helper);
        placeMotor(level, position.west());
    }

    private static void placeDevice(ServerLevel level, BlockPos position, StabilizerBlock block) {
        level.setBlock(position, block.defaultBlockState()
                .setValue(StabilizerBlock.HORIZONTAL_AXIS, Direction.Axis.X), Block.UPDATE_ALL);
    }

    private static void placeMotor(ServerLevel level, BlockPos position) {
        level.setBlock(position, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
    }

    private static void insertCells(ServerLevel level, BlockPos position, int count, GameTestHelper helper) {
        ItemStack remainder = capability(level, position, helper).insertItem(
                0, new ItemStack(ModItems.STABILIZATION_CELL.get(), count), false);
        helper.assertTrue(remainder.isEmpty(), "Stabilizer capability rejected Cells");
    }

    private static IItemHandler capability(ServerLevel level, BlockPos position, GameTestHelper helper) {
        IItemHandler capability = level.getCapability(Capabilities.ItemHandler.BLOCK, position, Direction.UP);
        helper.assertTrue(capability != null, "Stabilizer item capability is unavailable");
        return capability;
    }

    private static StabilizerBlockEntity blockEntity(ServerLevel level, BlockPos position) {
        if (level.getBlockEntity(position) instanceof StabilizerBlockEntity blockEntity) return blockEntity;
        throw new IllegalStateException("Stabilizer Block Entity is missing at " + position);
    }

    private static void removeDevice(ServerLevel level, BlockPos position) {
        level.setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(position.west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.getEntitiesOfClass(ItemEntity.class, new AABB(position).inflate(2.0))
                .forEach(ItemEntity::discard);
    }

    private static void runStage(LifecycleContext context, Runnable stage) {
        try {
            stage.run();
        } catch (RuntimeException error) {
            cleanup(context);
            throw error;
        }
    }

    private static void runStage(MixedContext context, Runnable stage) {
        try {
            stage.run();
        } catch (RuntimeException error) {
            cleanup(context);
            throw error;
        }
    }

    private static void cleanup(LifecycleContext context) {
        removeDevice(context.level(), context.device());
        removeDevice(context.level(), context.virtual());
        context.config().restore();
    }

    private static void cleanup(MixedContext context) {
        removeDevice(context.level(), context.tier1());
        removeDevice(context.level(), context.tier2());
        removeDevice(context.level(), context.tier3());
        context.config().restore();
    }

    private record LifecycleContext(
            GameTestHelper helper, ServerLevel level, BlockPos device, BlockPos virtual, ConfigSnapshot config) {}

    private record MixedContext(
            GameTestHelper helper,
            ServerLevel level,
            BlockPos tier1,
            BlockPos tier2,
            BlockPos tier3,
            ConfigSnapshot config) {}

    private record ConfigSnapshot(
            int tier1Rpm,
            int tier2Rpm,
            int tier3Rpm,
            int tier1Duration,
            int tier2Duration,
            int tier3Duration,
            int tier3Grace) {
        static ConfigSnapshot capture() {
            return new ConfigSnapshot(
                    FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.get(),
                    FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.get(),
                    FrontierProtocolServerConfig.TIER3_MINIMUM_RPM.get(),
                    FrontierProtocolServerConfig.TIER1_CELL_DURATION_TICKS.get(),
                    FrontierProtocolServerConfig.TIER2_CELL_DURATION_TICKS.get(),
                    FrontierProtocolServerConfig.TIER3_CELL_DURATION_TICKS.get(),
                    FrontierProtocolServerConfig.TIER3_GRACE_PERIOD_TICKS.get());
        }

        void restore() {
            FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.set(tier1Rpm);
            FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.set(tier2Rpm);
            FrontierProtocolServerConfig.TIER3_MINIMUM_RPM.set(tier3Rpm);
            FrontierProtocolServerConfig.TIER1_CELL_DURATION_TICKS.set(tier1Duration);
            FrontierProtocolServerConfig.TIER2_CELL_DURATION_TICKS.set(tier2Duration);
            FrontierProtocolServerConfig.TIER3_CELL_DURATION_TICKS.set(tier3Duration);
            FrontierProtocolServerConfig.TIER3_GRACE_PERIOD_TICKS.set(tier3Grace);
        }
    }
}
