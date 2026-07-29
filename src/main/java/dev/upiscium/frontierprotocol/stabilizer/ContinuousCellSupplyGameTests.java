package dev.upiscium.frontierprotocol.stabilizer;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.logistics.chute.ChuteBlock;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.registry.ModItems;
import dev.upiscium.frontierprotocol.suppression.ServerInfectionSuppressionService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ContinuousCellSupplyGameTests {
    private static final String TEMPLATE = "r9_continuous_cell_supply";
    private static final String BATCH = "r9_continuous_cell_supply";

    private ContinuousCellSupplyGameTests() {}

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 7500)
    public static void tierTwoMaintainsActiveThroughTwoPhysicalCellTransitions(GameTestHelper helper) {
        runContinuousSupply(helper, StabilizerTier.TIER_2, 3000);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 5500)
    public static void tierThreeMaintainsActiveThroughTwoPhysicalCellTransitions(GameTestHelper helper) {
        runContinuousSupply(helper, StabilizerTier.TIER_3, 2000);
    }

    private static void runContinuousSupply(
            GameTestHelper gameTestHelper, StabilizerTier tier, int expectedCellDuration) {
        CreateGameTestHelper helper = CreateGameTestHelper.of(gameTestHelper);
        ServerLevel level = gameTestHelper.getLevel();
        helper.forEveryBlockInStructure(pos -> helper.setBlock(pos, Blocks.AIR));

        StabilizerTierDefinition definition = StabilizerTierDefinitions.resolve(tier);
        helper.assertTrue(definition.cellDurationTicks() == expectedCellDuration,
                tier.serializedName() + " physical logistics test is not using the default Cell duration");

        BlockPos device = new BlockPos(1, 2, 2);
        BlockPos chute = device.above();
        BlockPos chest = chute.above();
        BlockPos motor = device.west();
        level.setBlock(
                helper.absolutePos(device),
                block(tier).defaultBlockState().setValue(StabilizerBlock.FACING, Direction.NORTH),
                Block.UPDATE_ALL);
        level.setBlock(
                helper.absolutePos(motor),
                AllBlocks.CREATIVE_MOTOR.getDefaultState()
                        .setValue(CreativeMotorBlock.FACING, Direction.EAST),
                Block.UPDATE_ALL);
        level.setBlock(
                helper.absolutePos(chute),
                AllBlocks.CHUTE.getDefaultState().setValue(ChuteBlock.FACING, Direction.DOWN),
                Block.UPDATE_ALL);
        level.setBlock(
                helper.absolutePos(chest),
                Blocks.CHEST.defaultBlockState(),
                Block.UPDATE_ALL);

        IItemHandler source = helper.itemStorageAt(chest);
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(
                source, new ItemStack(ModItems.STABILIZATION_CELL.get(), 3), false);
        helper.assertTrue(remainder.isEmpty(), "physical Cell source chest rejected its three Cells");

        gameTestHelper.runAfterDelay(2, () -> {
            if (!(level.getBlockEntity(helper.absolutePos(motor))
                    instanceof CreativeMotorBlockEntity motorBlockEntity)) {
                throw new IllegalStateException("physical Cell supply motor is missing");
            }
            motorBlockEntity.generatedSpeed.setValue(256);
            motorBlockEntity.updateGeneratedRotation();
            motorBlockEntity.setChanged();
        });

        SupplyMonitor monitor = new SupplyMonitor(
                gameTestHelper,
                helper,
                level,
                tier,
                helper.absolutePos(device),
                helper.absolutePos(chute),
                chest,
                definition.cellCapacity(),
                expectedCellDuration);
        gameTestHelper.onEachTick(monitor::tick);
    }

    private static StabilizerBlock block(StabilizerTier tier) {
        return switch (tier) {
            case TIER_1 -> ModBlocks.TIER_1_STABILIZER.get();
            case TIER_2 -> ModBlocks.TIER_2_STABILIZER.get();
            case TIER_3 -> ModBlocks.TIER_3_STABILIZER.get();
        };
    }

    private static final class SupplyMonitor {
        private final GameTestHelper gameTestHelper;
        private final CreateGameTestHelper helper;
        private final ServerLevel level;
        private final StabilizerTier tier;
        private final BlockPos device;
        private final BlockPos chute;
        private final BlockPos sourceChest;
        private final int capacity;
        private final int cellDuration;
        private boolean activeObserved;
        private int previousCellRemaining;
        private int previousBufferedCells;
        private int transitions;

        private SupplyMonitor(
                GameTestHelper gameTestHelper,
                CreateGameTestHelper helper,
                ServerLevel level,
                StabilizerTier tier,
                BlockPos device,
                BlockPos chute,
                BlockPos sourceChest,
                int capacity,
                int cellDuration) {
            this.gameTestHelper = gameTestHelper;
            this.helper = helper;
            this.level = level;
            this.tier = tier;
            this.device = device;
            this.chute = chute;
            this.sourceChest = sourceChest;
            this.capacity = capacity;
            this.cellDuration = cellDuration;
        }

        private void tick() {
            if (!(level.getBlockEntity(device) instanceof StabilizerBlockEntity stabilizer)) {
                gameTestHelper.fail(tier.serializedName() + " Stabilizer disappeared during physical Cell supply");
                return;
            }
            int bufferedCells = stabilizer.externalInventory().getStackInSlot(0).getCount();
            gameTestHelper.assertTrue(bufferedCells <= capacity,
                    tier.serializedName() + " physical Cell supply exceeded capacity");

            if (!activeObserved) {
                if (stabilizer.status() != StabilizerStatus.ACTIVE) return;
                activeObserved = true;
                previousCellRemaining = stabilizer.cellRemainingTicks();
                previousBufferedCells = bufferedCells;
                assertSuppressionSource();
                return;
            }

            gameTestHelper.assertTrue(stabilizer.status() == StabilizerStatus.ACTIVE,
                    tier.serializedName() + " entered GRACE_PERIOD or OFFLINE during continuous Cell supply");
            assertSuppressionSource();

            int cellRemaining = stabilizer.cellRemainingTicks();
            boolean transitioned = cellRemaining > previousCellRemaining;
            if (bufferedCells < previousBufferedCells) {
                gameTestHelper.assertTrue(transitioned,
                        tier.serializedName() + " consumed a duplicate Cell before the active Cell expired");
                gameTestHelper.assertTrue(previousBufferedCells - bufferedCells == 1,
                        tier.serializedName() + " consumed multiple Cells in one transition");
            }
            if (transitioned) {
                gameTestHelper.assertTrue(previousBufferedCells - bufferedCells == 1,
                        tier.serializedName() + " Cell rollover did not consume exactly one buffered Cell");
                gameTestHelper.assertTrue(cellRemaining == cellDuration - 1,
                        tier.serializedName() + " Cell rollover did not restart the default duration");
                transitions++;
            }

            previousCellRemaining = cellRemaining;
            previousBufferedCells = bufferedCells;
            if (transitions < 2) return;

            gameTestHelper.assertTrue(bufferedCells == 0,
                    tier.serializedName() + " retained an unexpected Cell after two rollovers");
            gameTestHelper.assertTrue(helper.getTotalItems(sourceChest) == 0,
                    tier.serializedName() + " source chest did not physically deliver every Cell");
            if (!(level.getBlockEntity(chute) instanceof ChuteBlockEntity chuteBlockEntity)) {
                gameTestHelper.fail(tier.serializedName() + " physical Cell Chute disappeared");
                return;
            }
            gameTestHelper.assertTrue(chuteBlockEntity.getItem().isEmpty(),
                    tier.serializedName() + " Cell remained stuck in the physical Chute");
            level.setBlock(device, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(device.west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(chute, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(chute.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            gameTestHelper.assertTrue(
                    ServerInfectionSuppressionService.INSTANCE.getSources(level, new ChunkPos(device)).stream()
                            .noneMatch(source -> source.id().equals(
                                    StabilizerSuppressionSource.at(tier, device).id())),
                    tier.serializedName() + " suppression source remained after fixture cleanup");
            gameTestHelper.succeed();
        }

        private void assertSuppressionSource() {
            ChunkPos center = new ChunkPos(device);
            gameTestHelper.assertTrue(
                    ServerInfectionSuppressionService.INSTANCE.getSources(level, center).stream()
                            .anyMatch(source -> source.id().equals(
                                    StabilizerSuppressionSource.at(tier, device).id())),
                    tier.serializedName() + " suppression source was interrupted during Cell rollover");
        }
    }
}
