package dev.upiscium.frontierprotocol.cleanup;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.registry.ModItems;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerBlock;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerTier;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerTierDefinition;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerTierDefinitions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TierTwoStabilizerCleanupGameTests {
    private TierTwoStabilizerCleanupGameTests() {}

    @GameTest(template = "empty", batch = "tier2_cleanup", timeoutTicks = 200)
    public static void tierTwoRegistersNineTasksWithItsProfile(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        ChunkPos center = new ChunkPos(origin);
        BlockPos device = new BlockPos(
                center.getMinBlockX() + 8, origin.getY() + 1, center.getMinBlockZ() + 8);
        ConfigSnapshot original = ConfigSnapshot.capture();
        try {
            FrontierProtocolServerConfig.TIER2_CHUNK_RADIUS.set(1);
            FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.set(8);
            FrontierProtocolServerConfig.TIER2_CELL_DURATION_TICKS.set(100);
            for (int x = center.x - 1; x <= center.x + 1; x++) {
                for (int z = center.z - 1; z <= center.z + 1; z++) level.getChunk(x, z);
            }
            ServerInfectionCleanupService.INSTANCE.clearRuntime(level.getServer());
            level.setBlock(device, ModBlocks.TIER_2_STABILIZER.get().defaultBlockState()
                    .setValue(StabilizerBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
            IItemHandler capability = level.getCapability(Capabilities.ItemHandler.BLOCK, device, Direction.UP);
            helper.assertTrue(capability != null, "Tier 2 cleanup test capability is unavailable");
            helper.assertTrue(capability.insertItem(
                            0, new ItemStack(ModItems.STABILIZATION_CELL.get()), false).isEmpty(),
                    "Tier 2 cleanup test rejected its Cell");
            level.setBlock(device.west(), AllBlocks.CREATIVE_MOTOR.getDefaultState()
                    .setValue(CreativeMotorBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        } catch (RuntimeException error) {
            cleanup(level, device, original);
            throw error;
        }

        helper.runAfterDelay(10, () -> {
            try {
                StabilizerTierDefinition definition = StabilizerTierDefinitions.resolve(StabilizerTier.TIER_2);
                helper.assertTrue(ServerInfectionCleanupService.INSTANCE.activeTaskCount(level) == 9,
                        "active Tier 2 did not register exactly nine cleanup tasks");
                helper.assertTrue(definition.cleanupProfile().equals(new CleanupSourceProfile(
                                FrontierProtocolServerConfig.TIER2_CLEANUP_INTERVAL_TICKS.get(),
                                FrontierProtocolServerConfig.TIER2_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.get(),
                                FrontierProtocolServerConfig.TIER2_CLEANUP_MUTATION_BUDGET_PER_CYCLE.get())),
                        "Tier 2 cleanup registration does not resolve its configured profile");
                helper.assertTrue(FrontierProtocolServerConfig.CLEANUP_GLOBAL_INSPECTION_BUDGET_PER_TICK.get() > 0
                                && FrontierProtocolServerConfig.CLEANUP_GLOBAL_MUTATION_BUDGET_PER_TICK.get() > 0,
                        "Tier 2 cleanup bypassed the shared global-cap configuration");
            } finally {
                cleanup(level, device, original);
            }
            helper.succeed();
        });
    }

    private static void cleanup(ServerLevel level, BlockPos device, ConfigSnapshot original) {
        level.setBlock(device, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(device.west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        ServerInfectionCleanupService.INSTANCE.clearRuntime(level.getServer());
        original.restore();
    }

    private record ConfigSnapshot(int radius, int rpm, int duration) {
        static ConfigSnapshot capture() {
            return new ConfigSnapshot(
                    FrontierProtocolServerConfig.TIER2_CHUNK_RADIUS.get(),
                    FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.get(),
                    FrontierProtocolServerConfig.TIER2_CELL_DURATION_TICKS.get());
        }

        void restore() {
            FrontierProtocolServerConfig.TIER2_CHUNK_RADIUS.set(radius);
            FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.set(rpm);
            FrontierProtocolServerConfig.TIER2_CELL_DURATION_TICKS.set(duration);
        }
    }
}
