package dev.upiscium.frontierprotocol.stabilizer;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.registry.ModItems;
import dev.upiscium.frontierprotocol.stabilizer.display.StabilizerDisplayNbt;
import dev.upiscium.frontierprotocol.stabilizer.display.StabilizerDisplaySnapshot;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StabilizerDisplayGameTests {
    private StabilizerDisplayGameTests() {}

    @GameTest(template = "empty", batch = "stabilizer_display_sync", timeoutTicks = 200)
    public static void snapshotsFollowAllTierLifecyclesAndUseDisplayOnlyPackets(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        List<Device> devices = List.of(
                new Device(StabilizerTier.TIER_1, origin),
                new Device(StabilizerTier.TIER_2, origin.offset(0, 0, 4)),
                new Device(StabilizerTier.TIER_3, origin.offset(0, 0, 8)));
        ConfigSnapshot original = ConfigSnapshot.capture();
        try {
            configure();
            for (Device device : devices) {
                level.setBlock(
                        device.pos(),
                        block(device.tier())
                                .defaultBlockState()
                                .setValue(StabilizerBlock.HORIZONTAL_AXIS, Direction.Axis.X),
                        Block.UPDATE_ALL);
                StabilizerDisplaySnapshot offline = blockEntity(level, device.pos()).displaySnapshot();
                assertSnapshot(helper, offline, device.tier(), StabilizerStatus.OFFLINE, 0);
                int insertedCells = device.tier() == StabilizerTier.TIER_2 ? 32 : 2;
                insertCells(level, device.pos(), insertedCells, helper);
                assertSnapshot(
                        helper,
                        blockEntity(level, device.pos()).displaySnapshot(),
                        device.tier(),
                        StabilizerStatus.OFFLINE,
                        insertedCells);
                assertNbtBoundaries(helper, level, blockEntity(level, device.pos()));
            }
            FrontierProtocolServerConfig.TIER2_CELL_CAPACITY.set(8);
            helper.runAfterDelay(2, () -> verifyOverCapacity(helper, level, devices, original));
        } catch (RuntimeException error) {
            cleanup(level, devices, original);
            throw error;
        }
    }

    private static void verifyOverCapacity(
            GameTestHelper helper, ServerLevel level, List<Device> devices, ConfigSnapshot original) {
        try {
            Device tierTwo = devices.get(1);
            StabilizerBlockEntity blockEntity = blockEntity(level, tierTwo.pos());
            StabilizerDisplaySnapshot snapshot = blockEntity.displaySnapshot();
            helper.assertTrue(snapshot != null, "over-capacity display snapshot was null");
            helper.assertTrue(snapshot.cellCount() == 32, "over-capacity snapshot clamped the Cell count");
            helper.assertTrue(snapshot.cellCapacity() == 8, "over-capacity snapshot did not refresh capacity");

            IItemHandler inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, tierTwo.pos(), Direction.UP);
            helper.assertTrue(inventory != null, "over-capacity item capability unavailable");
            helper.assertTrue(inventory.getStackInSlot(0).getCount() == 32, "capacity shrink removed stored Cells");

            CompoundTag client = new CompoundTag();
            blockEntity.write(client, level.registryAccess(), true);
            helper.assertTrue(
                    client.contains(StabilizerDisplayNbt.DISPLAY_KEY, Tag.TAG_COMPOUND),
                    "over-capacity client packet omitted display NBT");
            StabilizerDisplaySnapshot packetSnapshot = StabilizerDisplayNbt.read(client).orElse(null);
            helper.assertTrue(packetSnapshot != null, "over-capacity client display NBT was invalid");
            helper.assertTrue(
                    packetSnapshot.cellCount() == 32 && packetSnapshot.cellCapacity() == 8,
                    "over-capacity client display NBT did not preserve 32 / 8");

            FrontierProtocolServerConfig.TIER2_CELL_CAPACITY.set(original.tier2Capacity());
            for (Device device : devices) placeMotor(level, device.pos().west());
            helper.runAfterDelay(10, () -> verifyActive(helper, level, devices, original));
        } catch (RuntimeException error) {
            cleanup(level, devices, original);
            throw error;
        }
    }

    private static void verifyActive(
            GameTestHelper helper, ServerLevel level, List<Device> devices, ConfigSnapshot original) {
        try {
            for (Device device : devices) {
                StabilizerDisplaySnapshot active = blockEntity(level, device.pos()).displaySnapshot();
                int expectedCells = device.tier() == StabilizerTier.TIER_2 ? 31 : 1;
                assertSnapshot(helper, active, device.tier(), StabilizerStatus.ACTIVE, expectedCells);
                helper.assertTrue(active.cellRemainingTicks() > 0, device.tier() + " omitted active Cell time");
                level.setBlock(device.pos().west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            helper.runAfterDelay(3, () -> verifyGrace(helper, level, devices, original));
        } catch (RuntimeException error) {
            cleanup(level, devices, original);
            throw error;
        }
    }

    private static void verifyGrace(
            GameTestHelper helper, ServerLevel level, List<Device> devices, ConfigSnapshot original) {
        try {
            for (Device device : devices) {
                StabilizerDisplaySnapshot grace = blockEntity(level, device.pos()).displaySnapshot();
                int expectedCells = device.tier() == StabilizerTier.TIER_2 ? 31 : 1;
                assertSnapshot(helper, grace, device.tier(), StabilizerStatus.GRACE_PERIOD, expectedCells);
                helper.assertTrue(grace.graceRemainingTicks() > 0, device.tier() + " omitted grace time");
            }
            FrontierProtocolServerConfig.TIER1_CHUNK_RADIUS.set(16);
            FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.set(40);
            FrontierProtocolServerConfig.TIER1_STRESS_IMPACT.set(20.0);
            FrontierProtocolServerConfig.TIER1_CELL_CAPACITY.set(16);
            helper.runAfterDelay(2, () -> verifyConfigRefresh(helper, level, devices, original));
        } catch (RuntimeException error) {
            cleanup(level, devices, original);
            throw error;
        }
    }

    private static void verifyConfigRefresh(
            GameTestHelper helper, ServerLevel level, List<Device> devices, ConfigSnapshot original) {
        try {
            StabilizerDisplaySnapshot refreshed = blockEntity(level, devices.getFirst().pos()).displaySnapshot();
            helper.assertTrue(refreshed.chunkRadius() == 16, "display snapshot did not refresh radius");
            helper.assertTrue(refreshed.coverageWidth() == 33, "radius 16 display width was not 33");
            helper.assertTrue(refreshed.coverageChunkCount() == 1089, "radius 16 display coverage was not 1089");
            helper.assertTrue(refreshed.minimumRpm() == 40, "display snapshot did not refresh minimum RPM");
            helper.assertTrue(refreshed.stressImpact() == 20.0, "display snapshot did not refresh Stress impact");
            helper.assertTrue(refreshed.cellCapacity() == 16, "display snapshot did not refresh Cell capacity");
            cleanup(level, devices, original);
            helper.succeed();
        } catch (RuntimeException error) {
            cleanup(level, devices, original);
            throw error;
        }
    }

    private static void assertSnapshot(
            GameTestHelper helper,
            StabilizerDisplaySnapshot snapshot,
            StabilizerTier tier,
            StabilizerStatus status,
            int cells) {
        StabilizerTierDefinition definition = StabilizerTierDefinitions.resolve(tier);
        helper.assertTrue(snapshot != null, tier + " display snapshot was null");
        helper.assertTrue(snapshot.tier() == tier, tier + " display snapshot used wrong tier");
        helper.assertTrue(snapshot.status() == status, tier + " display snapshot used wrong status");
        helper.assertTrue(snapshot.minimumRpm() == definition.minimumRpm(), tier + " display minimum RPM mismatch");
        helper.assertTrue(snapshot.stressImpact() == definition.stressImpact(), tier + " display Stress mismatch");
        helper.assertTrue(snapshot.cellCount() == cells, tier + " display Cell count mismatch");
        helper.assertTrue(snapshot.cellCapacity() == definition.cellCapacity(), tier + " display capacity mismatch");
        helper.assertTrue(snapshot.cellDurationTicks() == definition.cellDurationTicks(), tier + " display duration mismatch");
        helper.assertTrue(snapshot.chunkRadius() == definition.chunkRadius(), tier + " display radius mismatch");
    }

    private static void assertNbtBoundaries(
            GameTestHelper helper, ServerLevel level, StabilizerBlockEntity blockEntity) {
        CompoundTag client = new CompoundTag();
        blockEntity.write(client, level.registryAccess(), true);
        helper.assertTrue(client.contains(StabilizerDisplayNbt.DISPLAY_KEY, Tag.TAG_COMPOUND), "client packet omitted display NBT");
        helper.assertTrue(!client.contains("inventory"), "client packet leaked full inventory NBT");
        helper.assertTrue(!client.contains("registeredChunkRadius"), "client packet leaked persistent radius baseline");

        CompoundTag persistent = new CompoundTag();
        blockEntity.write(persistent, level.registryAccess(), false);
        helper.assertTrue(persistent.contains("inventory", Tag.TAG_COMPOUND), "full persistence NBT lost inventory");
        helper.assertTrue(persistent.contains("registeredChunkRadius", Tag.TAG_INT), "full persistence NBT lost radius");
    }

    private static void insertCells(ServerLevel level, BlockPos pos, int count, GameTestHelper helper) {
        IItemHandler inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        helper.assertTrue(inventory != null, "display test item capability unavailable");
        ItemStack remainder = inventory.insertItem(0, new ItemStack(ModItems.STABILIZATION_CELL.get(), count), false);
        helper.assertTrue(remainder.isEmpty(), "display test Stabilizer rejected Cells");
    }

    private static void placeMotor(ServerLevel level, BlockPos pos) {
        level.setBlock(
                pos,
                AllBlocks.CREATIVE_MOTOR
                        .getDefaultState()
                        .setValue(CreativeMotorBlock.FACING, Direction.EAST),
                Block.UPDATE_ALL);
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

    private static void configure() {
        FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.set(8);
        FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.set(8);
        FrontierProtocolServerConfig.TIER3_MINIMUM_RPM.set(8);
        FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.set(20);
        FrontierProtocolServerConfig.TIER2_GRACE_PERIOD_TICKS.set(20);
        FrontierProtocolServerConfig.TIER3_GRACE_PERIOD_TICKS.set(20);
    }

    private static void cleanup(ServerLevel level, List<Device> devices, ConfigSnapshot original) {
        for (Device device : devices) {
            level.setBlock(device.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(device.pos().west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        original.restore();
    }

    private record Device(StabilizerTier tier, BlockPos pos) {}

    private record ConfigSnapshot(
            int tier1Rpm,
            int tier2Rpm,
            int tier3Rpm,
            int tier1Grace,
            int tier2Grace,
            int tier3Grace,
            int tier1Radius,
            double tier1Stress,
            int tier1Capacity,
            int tier2Capacity) {
        private static ConfigSnapshot capture() {
            return new ConfigSnapshot(
                    FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.get(),
                    FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.get(),
                    FrontierProtocolServerConfig.TIER3_MINIMUM_RPM.get(),
                    FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.get(),
                    FrontierProtocolServerConfig.TIER2_GRACE_PERIOD_TICKS.get(),
                    FrontierProtocolServerConfig.TIER3_GRACE_PERIOD_TICKS.get(),
                    FrontierProtocolServerConfig.TIER1_CHUNK_RADIUS.get(),
                    FrontierProtocolServerConfig.TIER1_STRESS_IMPACT.get(),
                    FrontierProtocolServerConfig.TIER1_CELL_CAPACITY.get(),
                    FrontierProtocolServerConfig.TIER2_CELL_CAPACITY.get());
        }

        private void restore() {
            FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.set(tier1Rpm);
            FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.set(tier2Rpm);
            FrontierProtocolServerConfig.TIER3_MINIMUM_RPM.set(tier3Rpm);
            FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.set(tier1Grace);
            FrontierProtocolServerConfig.TIER2_GRACE_PERIOD_TICKS.set(tier2Grace);
            FrontierProtocolServerConfig.TIER3_GRACE_PERIOD_TICKS.set(tier3Grace);
            FrontierProtocolServerConfig.TIER1_CHUNK_RADIUS.set(tier1Radius);
            FrontierProtocolServerConfig.TIER1_STRESS_IMPACT.set(tier1Stress);
            FrontierProtocolServerConfig.TIER1_CELL_CAPACITY.set(tier1Capacity);
            FrontierProtocolServerConfig.TIER2_CELL_CAPACITY.set(tier2Capacity);
        }
    }
}
