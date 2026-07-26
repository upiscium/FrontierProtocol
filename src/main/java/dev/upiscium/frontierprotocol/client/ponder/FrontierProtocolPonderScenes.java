package dev.upiscium.frontierprotocol.client.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.registry.ModItems;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerBlock;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerStatus;
import java.util.List;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredBlock;

public final class FrontierProtocolPonderScenes {
    private FrontierProtocolPonderScenes() {}

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        for (OperationSceneDefinition definition : operationSceneDefinitions()) {
            helper.addStoryBoard(
                    definition.component(),
                    "stabilizer/operation",
                    (scene, util) -> operation(scene, util, definition.block().get()),
                    FrontierProtocolPonderTags.CONTAINMENT);
        }
        for (ResourceLocation component : new ResourceLocation[] {
            ModItems.TIER_1_STABILIZER.getId(),
            ModItems.TIER_2_STABILIZER.getId(),
            ModItems.TIER_3_STABILIZER.getId()
        }) {
            helper.addStoryBoard(
                    component, "stabilizer/coverage", FrontierProtocolPonderScenes::coverage, FrontierProtocolPonderTags.CONTAINMENT);
        }
        for (ResourceLocation component : new ResourceLocation[] {
            ModItems.STABILIZATION_COMPOUND.getId(),
            ModItems.STABILIZATION_CELL.getId(),
            ModItems.TIER_1_STABILIZER.getId(),
            ModItems.TIER_2_STABILIZER.getId(),
            ModItems.TIER_3_STABILIZER.getId()
        }) {
            helper.addStoryBoard(
                    component, "stabilizer/production", FrontierProtocolPonderScenes::production, FrontierProtocolPonderTags.CONTAINMENT);
        }
    }

    static List<OperationSceneDefinition> operationSceneDefinitions() {
        return List.of(
                new OperationSceneDefinition(ModItems.TIER_1_STABILIZER.getId(), ModBlocks.TIER_1_STABILIZER),
                new OperationSceneDefinition(ModItems.TIER_2_STABILIZER.getId(), ModBlocks.TIER_2_STABILIZER),
                new OperationSceneDefinition(ModItems.TIER_3_STABILIZER.getId(), ModBlocks.TIER_3_STABILIZER),
                new OperationSceneDefinition(ModItems.STABILIZATION_CELL.getId(), ModBlocks.TIER_1_STABILIZER));
    }

    private static void operation(
            SceneBuilder scene, SceneBuildingUtil util, StabilizerBlock stabilizerBlock) {
        scene.title("stabilizer_operation", "Operating a Stabilizer");
        scene.configureBasePlate(0, 0, 5);
        Selection plate = util.select().fromTo(0, 0, 0, 4, 0, 4);
        BlockPos stabilizerPos = util.grid().at(2, 1, 2);
        BlockPos shaftPos = util.grid().at(1, 1, 2);
        BlockPos funnelPos = util.grid().at(2, 2, 2);
        BlockPos depotPos = util.grid().at(2, 2, 3);
        Selection stabilizer = util.select().position(stabilizerPos);
        Selection shaft = util.select().position(shaftPos);
        Selection logistics = util.select().fromTo(funnelPos, depotPos);
        scene.world().setBlocks(plate, Blocks.SMOOTH_STONE.defaultBlockState(), false);
        scene.showBasePlate();
        scene.world().setBlock(stabilizerPos, state(stabilizerBlock, StabilizerStatus.OFFLINE), false);
        scene.world().setBlock(shaftPos, AllBlocks.SHAFT.getDefaultState(), false);
        scene.world().setBlock(funnelPos, AllBlocks.ANDESITE_FUNNEL.getDefaultState(), false);
        scene.world().setBlock(depotPos, AllBlocks.DEPOT.getDefaultState(), false);
        scene.world().showSection(stabilizer, Direction.DOWN);
        scene.world().showSection(shaft, Direction.EAST);
        scene.world().showSection(logistics, Direction.DOWN);
        scene.overlay()
                .showText(60)
                .text("Connect Create rotational power to the Stabilizer shaft.")
                .pointAt(util.vector().topOf(stabilizerPos))
                .placeNearTarget();
        scene.idle(70);
        scene.overlay()
                .showControls(util.vector().topOf(depotPos), Pointing.DOWN, 50)
                .withItem(new ItemStack(ModItems.STABILIZATION_CELL.get()));
        scene.overlay().showOutline(PonderPalette.BLUE, "cell_logistics", logistics, 60);
        scene.overlay()
                .showText(60)
                .text("Use Funnels, Chutes, Belts, or other Create logistics to insert Stabilization Cells into the machine.")
                .pointAt(util.vector().topOf(funnelPos))
                .placeNearTarget();
        scene.idle(70);
        scene.world().setBlock(stabilizerPos, state(stabilizerBlock, StabilizerStatus.ACTIVE), false);
        CreateSceneBuilder createScene = new CreateSceneBuilder(scene);
        createScene.world().setKineticSpeed(stabilizer, 64.0F);
        createScene.world().setKineticSpeed(shaft, 64.0F);
        scene.overlay()
                .showText(70)
                .text("At the configured RPM, a Cell starts an ACTIVE operating duration with suppression and progressive cleanup.")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().topOf(stabilizerPos))
                .placeNearTarget();
        scene.idle(80);
        scene.world().setBlock(stabilizerPos, state(stabilizerBlock, StabilizerStatus.GRACE_PERIOD), false);
        createScene.world().setKineticSpeed(stabilizer, 0.0F);
        createScene.world().setKineticSpeed(shaft, 0.0F);
        scene.overlay()
                .showText(70)
                .text("If rotation is lost, the machine enters GRACE_PERIOD. Suppression remains, but cleanup pauses.")
                .colored(PonderPalette.MEDIUM)
                .pointAt(util.vector().topOf(stabilizerPos))
                .placeNearTarget();
        scene.idle(80);
        scene.world().setBlock(stabilizerPos, state(stabilizerBlock, StabilizerStatus.OFFLINE), false);
        scene.overlay()
                .showText(60)
                .text("When grace expires the machine becomes OFFLINE. Restore rotation while stored Cell time remains, or supply another Cell.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(stabilizerPos))
                .placeNearTarget();
        scene.idle(70);
    }

    public static void coverage(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("stabilizer_coverage", "Containment Coverage");
        scene.configureBasePlate(0, 0, 7);
        Selection grid = util.select().fromTo(1, 0, 1, 5, 0, 5);
        scene.world().setBlocks(grid, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), false);
        scene.showBasePlate();
        scene.overlay()
                .showText(55)
                .text("Each tile represents one chunk, centered on the Stabilizer placement chunk.")
                .pointAt(util.vector().centerOf(3, 0, 3))
                .placeNearTarget();
        scene.idle(65);
        scene.overlay().showOutline(PonderPalette.GREEN, "tier_1", util.select().position(3, 0, 3), 55);
        scene.overlay()
                .showText(55)
                .text("Tier 1 covers 1x1, Tier 2 covers 3x3, and Tier 3 covers 5x5 chunks by default.")
                .pointAt(util.vector().centerOf(3, 0, 3))
                .placeNearTarget();
        scene.idle(65);
        scene.overlay().showOutline(PonderPalette.BLUE, "tier_2", util.select().fromTo(2, 0, 2, 4, 0, 4), 55);
        scene.idle(35);
        scene.overlay().showOutline(PonderPalette.RED, "tier_3", grid, 55);
        scene.overlay()
                .showText(70)
                .text("Coverage spans the full dimension height. If one overlapping Stabilizer stops, another source keeps shared chunks suppressed.")
                .pointAt(util.vector().centerOf(3, 0, 3))
                .placeNearTarget();
        scene.idle(80);
        scene.overlay()
                .showText(70)
                .text("Suppression does not stop hostile mobs. Build walls, lighting, and physical defenses separately.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(3, 0, 3))
                .placeNearTarget();
        scene.idle(80);
        scene.overlay()
                .showText(90)
                .text("Initial Overworld spawn is a separate permanent 5x5 suppression source by default. It needs no Cell or rotation, performs no cleanup, and does not stop mobs.")
                .colored(PonderPalette.MEDIUM)
                .pointAt(util.vector().centerOf(3, 0, 3))
                .placeNearTarget();
        scene.idle(100);
    }

    public static void production(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("stabilizer_production", "Stabilizer Production");
        scene.configureBasePlate(0, 0, 7);
        Selection plate = util.select().fromTo(0, 0, 0, 6, 0, 6);
        scene.world().setBlocks(plate, Blocks.SMOOTH_STONE.defaultBlockState(), false);
        scene.showBasePlate();
        BlockPos display = util.grid().at(3, 1, 3);
        scene.overlay()
                .showControls(util.vector().topOf(display), Pointing.DOWN, 60)
                .withItem(new ItemStack(ModItems.STABILIZATION_COMPOUND.get()));
        scene.overlay()
                .showText(70)
                .text("Heated Mixing combines Spore Biomass Block, Redstone, Charcoal, and Water into Stabilization Compound.")
                .pointAt(util.vector().topOf(display))
                .placeNearTarget();
        scene.idle(80);
        scene.overlay()
                .showControls(util.vector().topOf(display), Pointing.DOWN, 60)
                .withItem(new ItemStack(ModItems.STABILIZATION_CELL.get()));
        scene.overlay()
                .showText(70)
                .text("Deploy Compound onto an Iron Sheet to seal a Stabilization Cell. Compound cannot power a Stabilizer directly.")
                .pointAt(util.vector().topOf(display))
                .placeNearTarget();
        scene.idle(80);
        scene.overlay()
                .showControls(util.vector().topOf(display), Pointing.DOWN, 60)
                .withItem(new ItemStack(ModItems.TIER_1_STABILIZER.get()));
        scene.overlay()
                .showText(70)
                .text("Mechanical Crafters produce Tier 1, then upgrade Tier 1 to Tier 2 and Tier 2 to Tier 3.")
                .pointAt(util.vector().topOf(display))
                .placeNearTarget();
        scene.idle(80);
        scene.overlay()
                .showText(70)
                .text("Every tier consumes the same Stabilization Cell. Automate production and continuous delivery with Create logistics.")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().topOf(display))
                .placeNearTarget();
        scene.idle(80);
    }

    private static net.minecraft.world.level.block.state.BlockState state(
            StabilizerBlock block, StabilizerStatus status) {
        return block.defaultBlockState().setValue(StabilizerBlock.STATUS, status);
    }

    record OperationSceneDefinition(
            ResourceLocation component, DeferredBlock<StabilizerBlock> block) {}
}
