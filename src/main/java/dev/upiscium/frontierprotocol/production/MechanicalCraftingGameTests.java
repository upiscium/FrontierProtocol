package dev.upiscium.frontierprotocol.production;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.crafter.ConnectedInputHandler;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.createmod.catnip.math.Pointing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MechanicalCraftingGameTests {
    private static final String TEMPLATE = "r9_physical_mechanical_crafting";
    private static final String BATCH = "r9_physical_mechanical_crafting";

    private MechanicalCraftingGameTests() {}

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 600)
    public static void tierOneRunsThroughPhysicalMechanicalCrafters(GameTestHelper helper) {
        runPhysicalCraft(
                helper,
                new String[] {"ICI", "APA", "ISI"},
                Map.of(
                        'I', AllItems.IRON_SHEET.get(),
                        'C', ModItems.STABILIZATION_CELL.get(),
                        'A', AllBlocks.ANDESITE_CASING.get(),
                        'P', AllItems.PRECISION_MECHANISM.get(),
                        'S', AllBlocks.SHAFT.get()),
                ModItems.TIER_1_STABILIZER.get(),
                null);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 600)
    public static void tierTwoConsumesTierOneInPhysicalMechanicalCrafters(GameTestHelper helper) {
        runPhysicalCraft(
                helper,
                new String[] {"SCS", "P1P", "SBS"},
                Map.of(
                        'S', AllItems.STURDY_SHEET.get(),
                        'C', ModItems.STABILIZATION_CELL.get(),
                        'P', AllItems.PRECISION_MECHANISM.get(),
                        '1', ModItems.TIER_1_STABILIZER.get(),
                        'B', AllBlocks.BRASS_CASING.get()),
                ModItems.TIER_2_STABILIZER.get(),
                ModItems.TIER_1_STABILIZER.get());
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 800)
    public static void tierThreeConsumesTierTwoInPhysicalMechanicalCrafters(GameTestHelper helper) {
        runPhysicalCraft(
                helper,
                new String[] {"SSCSS", "SRPRS", "CP2PC", "SRPRS", "SSCSS"},
                Map.of(
                        'S', AllItems.STURDY_SHEET.get(),
                        'C', ModItems.STABILIZATION_CELL.get(),
                        'R', AllBlocks.RAILWAY_CASING.get(),
                        'P', AllItems.PRECISION_MECHANISM.get(),
                        '2', ModItems.TIER_2_STABILIZER.get()),
                ModItems.TIER_3_STABILIZER.get(),
                ModItems.TIER_2_STABILIZER.get());
    }

    private static void runPhysicalCraft(
            GameTestHelper gameTestHelper,
            String[] pattern,
            Map<Character, ItemLike> ingredients,
            ItemLike output,
            ItemLike consumedUpgrade) {
        CreateGameTestHelper helper = CreateGameTestHelper.of(gameTestHelper);
        ServerLevel level = gameTestHelper.getLevel();
        clearFixture(helper);

        int size = pattern.length;
        BlockPos bottomLeft = new BlockPos(size == 5 ? 1 : 2, 2, 3);
        BlockPos outputChest = bottomLeft.offset(size, 0, 0);
        List<BlockPos> crafterPositions = placeCrafterGrid(helper, level, bottomLeft, size);
        helper.setBlock(outputChest, Blocks.CHEST.defaultBlockState());
        helper.assertTrue(helper.itemStorageAt(outputChest) != null,
                "physical Mechanical Crafter output chest is unavailable");
        helper.assertTrue(findBlocks(helper, Blocks.CRAFTING_TABLE).isEmpty(),
                "physical fixture must not contain a normal Crafting Table");

        BlockPos controllerPos = crafterPositions.getFirst();
        MechanicalCrafterBlockEntity controller = crafter(level, helper.absolutePos(controllerPos));
        List<BlockPos> connected = crafterPositions.stream()
                .skip(1)
                .map(helper::absolutePos)
                .toList();
        ConnectedInputHandler.initAndAddAll(level, controller, connected);
        controller.connectivityChanged();
        controller.setChanged();

        BlockPos cogwheelPos = bottomLeft.below();
        level.setBlock(
                helper.absolutePos(cogwheelPos),
                AllBlocks.COGWHEEL.getDefaultState()
                        .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z),
                Block.UPDATE_ALL);
        BlockPos motorPos = cogwheelPos.north();
        level.setBlock(
                helper.absolutePos(motorPos),
                AllBlocks.CREATIVE_MOTOR.getDefaultState()
                        .setValue(CreativeMotorBlock.FACING, Direction.SOUTH),
                Block.UPDATE_ALL);

        gameTestHelper.runAfterDelay(2, () -> {
            if (!(level.getBlockEntity(helper.absolutePos(motorPos))
                    instanceof CreativeMotorBlockEntity motor)) {
                throw new IllegalStateException("physical Mechanical Crafter motor is missing");
            }
            motor.generatedSpeed.setValue(256);
            motor.updateGeneratedRotation();
            motor.setChanged();
        });

        gameTestHelper.runAfterDelay(12, () -> {
            helper.assertTrue(
                    controller.getInput().getInventories(level, helper.absolutePos(controllerPos)).size()
                            == size * size,
                    "physical Mechanical Crafter grid is not fully connected");
            for (BlockPos pos : crafterPositions) {
                MechanicalCrafterBlockEntity blockEntity = crafter(level, helper.absolutePos(pos));
                helper.assertTrue(Math.abs(blockEntity.getSpeed()) > 0,
                        "physical Mechanical Crafter is not powered at " + pos);
            }
            insertPattern(helper, level, bottomLeft, pattern, ingredients);

            gameTestHelper.succeedWhen(() -> {
                helper.assertContainerContains(outputChest, new ItemStack(output));
                helper.assertTrue(helper.getTotalItems(outputChest) == 1,
                        "physical Mechanical Crafting produced an unexpected output count");
                for (BlockPos pos : crafterPositions) {
                    helper.assertTrue(crafter(level, helper.absolutePos(pos))
                                    .getInventory().getStackInSlot(0).isEmpty(),
                            "physical Mechanical Crafter retained an input at " + pos);
                }
                if (consumedUpgrade != null) {
                    boolean returnedUpgrade = level.getEntitiesOfClass(
                                    ItemEntity.class,
                                    gameTestHelper.getBounds(),
                                    entity -> entity.getItem().is(consumedUpgrade.asItem()))
                            .stream()
                            .findAny()
                            .isPresent();
                    helper.assertTrue(!returnedUpgrade,
                            "physical Mechanical Crafting returned the consumed upgrade Stabilizer");
                }
            });
        });
    }

    private static List<BlockPos> placeCrafterGrid(
            CreateGameTestHelper helper, ServerLevel level, BlockPos bottomLeft, int size) {
        List<BlockPos> positions = new ArrayList<>(size * size);
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                BlockPos pos = bottomLeft.offset(column, size - 1 - row, 0);
                Pointing pointing = row == size - 1 ? Pointing.RIGHT : Pointing.DOWN;
                level.setBlock(
                        helper.absolutePos(pos),
                        AllBlocks.MECHANICAL_CRAFTER.getDefaultState()
                                .setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.SOUTH)
                                .setValue(MechanicalCrafterBlock.POINTING, pointing),
                        Block.UPDATE_ALL);
                Direction expectedTarget = row == size - 1 ? Direction.EAST : Direction.DOWN;
                helper.assertTrue(
                        MechanicalCrafterBlock.getTargetDirection(helper.getBlockState(pos)) == expectedTarget,
                        "physical Mechanical Crafter points in the wrong direction at " + pos);
                positions.add(pos);
            }
        }
        return positions;
    }

    private static void insertPattern(
            CreateGameTestHelper helper,
            ServerLevel level,
            BlockPos bottomLeft,
            String[] pattern,
            Map<Character, ItemLike> ingredients) {
        int size = pattern.length;
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                BlockPos relativePos = bottomLeft.offset(column, size - 1 - row, 0);
                ItemLike ingredient = ingredients.get(pattern[row].charAt(column));
                helper.assertTrue(ingredient != null, "physical recipe contains an undefined symbol");
                ItemStack remainder = crafter(level, helper.absolutePos(relativePos))
                        .getInventory()
                        .insertItem(0, new ItemStack(ingredient), false);
                helper.assertTrue(remainder.isEmpty(),
                        "physical Mechanical Crafter rejected input at row " + row + ", column " + column);
            }
        }
    }

    private static MechanicalCrafterBlockEntity crafter(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MechanicalCrafterBlockEntity blockEntity) return blockEntity;
        throw new IllegalStateException("physical Mechanical Crafter is missing at " + pos);
    }

    private static void clearFixture(CreateGameTestHelper helper) {
        helper.forEveryBlockInStructure(pos -> helper.setBlock(pos, Blocks.AIR));
    }

    private static List<BlockPos> findBlocks(CreateGameTestHelper helper, Block block) {
        List<BlockPos> positions = new ArrayList<>();
        helper.forEveryBlockInStructure(pos -> {
            if (helper.getBlockState(pos).is(block)) positions.add(pos.immutable());
        });
        return positions;
    }
}
