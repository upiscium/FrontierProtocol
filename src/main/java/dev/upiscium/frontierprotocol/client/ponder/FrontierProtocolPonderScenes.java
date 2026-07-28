package dev.upiscium.frontierprotocol.client.ponder;

import com.drmangotea.tfmg.registry.TFMGFluids;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.registry.ModItems;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerBlock;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerStatus;
import java.util.List;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.registries.DeferredBlock;

public final class FrontierProtocolPonderScenes {
    private FrontierProtocolPonderScenes() {}

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        for (OperationSceneDefinition definition : operationSceneDefinitions()) {
            helper.addStoryBoard(
                    definition.component(),
                    "stabilizer/operation",
                    (scene, util) -> operation(scene, util, definition),
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
                new OperationSceneDefinition(ModItems.TIER_1_STABILIZER.getId(), ModBlocks.TIER_1_STABILIZER, 32.0F),
                new OperationSceneDefinition(ModItems.TIER_2_STABILIZER.getId(), ModBlocks.TIER_2_STABILIZER, 64.0F),
                new OperationSceneDefinition(ModItems.TIER_3_STABILIZER.getId(), ModBlocks.TIER_3_STABILIZER, 128.0F),
                new OperationSceneDefinition(ModItems.STABILIZATION_CELL.getId(), ModBlocks.TIER_1_STABILIZER, 32.0F));
    }

    private static void operation(
            SceneBuilder scene, SceneBuildingUtil util, OperationSceneDefinition definition) {
        StabilizerBlock stabilizerBlock = definition.block().get();
        scene.title("stabilizer_operation", "Operating a Stabilizer");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.7F);
        scene.setSceneOffsetY(-1.0F);
        Selection plate = util.select().fromTo(0, 0, 0, 4, 0, 4);
        BlockPos stabilizerPos = util.grid().at(2, 1, 2);
        BlockPos shaftPos = util.grid().at(1, 1, 2);
        BlockPos chutePos = util.grid().at(2, 2, 2);
        BlockPos depotPos = util.grid().at(2, 3, 2);
        Selection stabilizer = util.select().position(stabilizerPos);
        Selection shaft = util.select().position(shaftPos);
        Selection logistics = util.select().fromTo(chutePos, depotPos);
        scene.world().setBlocks(plate, Blocks.SMOOTH_STONE.defaultBlockState(), false);
        scene.showBasePlate();
        scene.world().setBlock(stabilizerPos, state(stabilizerBlock, StabilizerStatus.OFFLINE), false);
        scene.world().setBlock(shaftPos, AllBlocks.SHAFT.getDefaultState(), false);
        scene.world().setBlock(chutePos, AllBlocks.CHUTE.getDefaultState(), false);
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
                .pointAt(util.vector().topOf(chutePos))
                .placeNearTarget();
        scene.idle(70);
        scene.overlay()
                .showControls(util.vector().topOf(chutePos), Pointing.DOWN, 35)
                .withItem(new ItemStack(ModItems.STABILIZATION_CELL.get()));
        scene.world().setBlock(stabilizerPos, state(stabilizerBlock, StabilizerStatus.ACTIVE), false);
        CreateSceneBuilder createScene = new CreateSceneBuilder(scene);
        createScene.world().setKineticSpeed(stabilizer, definition.kineticSpeed());
        createScene.world().setKineticSpeed(shaft, definition.kineticSpeed());
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
        scene.scaleSceneView(0.8F);
        scene.setSceneOffsetY(-1.0F);
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
        ProductionSceneEquipment equipment = productionSceneEquipment();
        CreateSceneBuilder createScene = new CreateSceneBuilder(scene);
        scene.title("stabilizer_production", "Stabilizer Production");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.7F);
        scene.setSceneOffsetY(-1.0F);
        Selection plate = util.select().fromTo(0, 0, 0, 6, 0, 6);
        scene.world().setBlocks(plate, Blocks.SMOOTH_STONE.defaultBlockState(), false);
        scene.showBasePlate();

        BlockPos tankPos = util.grid().at(1, 1, 3);
        BlockPos pipePos = util.grid().at(2, 1, 3);
        BlockPos basinPos = util.grid().at(3, 1, 3);
        BlockPos mixerPos = util.grid().at(3, 2, 3);
        BlockPos mixerShaftPos = util.grid().at(3, 3, 3);
        BlockPos compoundOutputPos = util.grid().at(5, 1, 3);
        Selection compoundEquipment = util.select()
                .position(tankPos)
                .add(util.select().position(pipePos))
                .add(util.select().position(basinPos))
                .add(util.select().position(mixerPos))
                .add(util.select().position(mixerShaftPos))
                .add(util.select().position(compoundOutputPos));
        Selection mixerKinetics = util.select().position(mixerPos).add(util.select().position(mixerShaftPos));

        scene.world().setBlock(tankPos, equipment.fluidTank().defaultBlockState(), false);
        scene.world().setBlock(pipePos, equipment.fluidPipe().defaultBlockState(), false);
        scene.world().setBlock(basinPos, equipment.basin().defaultBlockState(), false);
        scene.world().setBlock(mixerPos, equipment.mechanicalMixer().defaultBlockState(), false);
        scene.world().setBlock(mixerShaftPos, equipment.shaft().defaultBlockState(), false);
        scene.world().setBlock(compoundOutputPos, equipment.depot().defaultBlockState(), false);
        ElementLink<WorldSectionElement> compoundSection =
                scene.world().showIndependentSection(compoundEquipment, Direction.DOWN);
        scene.world().modifyBlockEntity(tankPos, FluidTankBlockEntity.class, tank -> tank.getTankInventory()
                .fill(
                        new FluidStack(
                                (net.minecraft.world.level.material.Fluid) TFMGFluids.MOLTEN_PLASTIC.getSource(), 100),
                        IFluidHandler.FluidAction.EXECUTE));
        scene.overlay()
                .showControls(util.vector().topOf(tankPos), Pointing.DOWN, 80)
                .withItem(new ItemStack(TFMGFluids.MOLTEN_PLASTIC.getBucket().orElseThrow()));
        scene.overlay()
                .showControls(util.vector().of(2.5, 1.5, 2.5), Pointing.DOWN, 80)
                .withItem(new ItemStack(Items.SAND));
        scene.overlay()
                .showControls(util.vector().of(3.5, 1.5, 2.5), Pointing.DOWN, 80)
                .withItem(new ItemStack(Items.BLUE_ICE));
        scene.overlay()
                .showControls(util.vector().of(4.5, 1.5, 2.5), Pointing.DOWN, 80)
                .withItem(new ItemStack(Items.IRON_NUGGET, 8));
        scene.overlay()
                .showText(90)
                .text("Mix 100 mB of TFMG Liquid Plastic with Sand, Blue Ice, and eight Iron Nuggets. No heat is required.")
                .pointAt(util.vector().topOf(basinPos))
                .placeNearTarget();
        scene.idle(100);
        createScene.world().setKineticSpeed(mixerKinetics, 64.0F);
        scene.world().modifyBlockEntity(mixerPos, MechanicalMixerBlockEntity.class, MechanicalMixerBlockEntity::startProcessingBasin);
        scene.idle(45);
        scene.overlay()
                .showControls(util.vector().topOf(compoundOutputPos), Pointing.DOWN, 70)
                .withItem(new ItemStack(ModItems.STABILIZATION_COMPOUND.get()));
        scene.overlay()
                .showText(70)
                .text("The unheated Mixing step produces one Stabilization Compound.")
                .pointAt(util.vector().topOf(compoundOutputPos))
                .placeNearTarget();
        scene.idle(80);

        scene.world().hideIndependentSection(compoundSection, Direction.UP);
        scene.idle(25);
        scene.world().setBlocks(compoundEquipment, Blocks.AIR.defaultBlockState(), false);

        BlockPos depotPos = util.grid().at(3, 1, 3);
        BlockPos deployerPos = util.grid().at(3, 2, 3);
        BlockPos deployerShaftPos = util.grid().at(3, 3, 3);
        Selection cellEquipment = util.select().fromTo(depotPos, deployerShaftPos);
        scene.world().setBlock(depotPos, equipment.depot().defaultBlockState(), false);
        scene.world().setBlock(
                deployerPos,
                equipment.deployer().defaultBlockState().setValue(DeployerBlock.FACING, Direction.DOWN),
                false);
        scene.world().setBlock(deployerShaftPos, equipment.shaft().defaultBlockState(), false);
        ElementLink<WorldSectionElement> cellSection =
                scene.world().showIndependentSection(cellEquipment, Direction.DOWN);
        scene.world().modifyBlockEntityNBT(
                util.select().position(deployerPos),
                DeployerBlockEntity.class,
                tag -> tag.put(
                        "HeldItem",
                        new ItemStack(ModItems.STABILIZATION_COMPOUND.get())
                                .saveOptional(scene.world().getHolderLookupProvider())));
        createScene.world().setKineticSpeed(util.select().fromTo(deployerPos, deployerShaftPos), 32.0F);
        scene.overlay()
                .showControls(util.vector().topOf(depotPos), Pointing.DOWN, 70)
                .withItem(new ItemStack(com.simibubi.create.AllItems.IRON_SHEET.get()));
        scene.overlay()
                .showControls(util.vector().topOf(deployerPos), Pointing.DOWN, 70)
                .withItem(new ItemStack(ModItems.STABILIZATION_COMPOUND.get()));
        scene.overlay()
                .showText(80)
                .text("A powered Deployer presses Compound onto an Iron Sheet on a Depot.")
                .pointAt(util.vector().topOf(depotPos))
                .placeNearTarget();
        scene.idle(90);
        createScene.world().moveDeployer(deployerPos, 1.0F, 20);
        scene.idle(22);
        createScene.world().moveDeployer(deployerPos, -1.0F, 20);
        scene.overlay()
                .showControls(util.vector().topOf(depotPos), Pointing.DOWN, 70)
                .withItem(new ItemStack(ModItems.STABILIZATION_CELL.get()));
        scene.overlay()
                .showText(80)
                .text("Deploying produces one Stabilization Cell. Compound cannot power a Stabilizer directly.")
                .pointAt(util.vector().topOf(depotPos))
                .placeNearTarget();
        scene.idle(90);

        scene.world().hideIndependentSection(cellSection, Direction.UP);
        scene.idle(25);
        scene.world().setBlocks(cellEquipment, Blocks.AIR.defaultBlockState(), false);

        Selection crafters = util.select().fromTo(2, 1, 3, 4, 3, 3);
        BlockPos crafterShaftPos = util.grid().at(5, 2, 3);
        Selection craftingEquipment = crafters.add(util.select().position(crafterShaftPos));
        scene.world().setBlocks(
                crafters,
                equipment.mechanicalCrafter().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH),
                false);
        scene.world().setBlock(
                crafterShaftPos,
                equipment.shaft().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X),
                false);
        scene.world().showIndependentSection(craftingEquipment, Direction.DOWN);
        createScene.world().setKineticSpeed(craftingEquipment, 32.0F);
        createScene.world().setCraftingResult(util.grid().at(3, 2, 3), new ItemStack(ModItems.TIER_1_STABILIZER.get()));
        scene.overlay()
                .showControls(util.vector().centerOf(3, 2, 3), Pointing.RIGHT, 80)
                .withItem(new ItemStack(ModItems.TIER_1_STABILIZER.get()));
        scene.overlay()
                .showText(80)
                .text("A representative 3x3 Mechanical Crafter array produces the Tier 1 Stabilizer.")
                .pointAt(util.vector().centerOf(3, 2, 3))
                .placeNearTarget();
        scene.idle(90);
        scene.overlay()
                .showControls(util.vector().of(2.5, 3.8, 3.5), Pointing.DOWN, 80)
                .withItem(new ItemStack(ModItems.TIER_2_STABILIZER.get()));
        scene.overlay()
                .showControls(util.vector().of(4.5, 3.8, 3.5), Pointing.DOWN, 80)
                .withItem(new ItemStack(ModItems.TIER_3_STABILIZER.get()));
        scene.overlay()
                .showText(90)
                .text("Mechanical Crafters then upgrade Tier 1 to Tier 2 and Tier 2 to Tier 3. Every tier uses the same Cell.")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(3, 2, 3))
                .placeNearTarget();
        scene.idle(100);
    }

    static ProductionSceneEquipment productionSceneEquipment() {
        return new ProductionSceneEquipment(
                AllBlocks.MECHANICAL_MIXER.get(),
                AllBlocks.BASIN.get(),
                AllBlocks.FLUID_TANK.get(),
                AllBlocks.FLUID_PIPE.get(),
                AllBlocks.DEPOT.get(),
                AllBlocks.DEPLOYER.get(),
                AllBlocks.MECHANICAL_CRAFTER.get(),
                AllBlocks.SHAFT.get());
    }

    private static net.minecraft.world.level.block.state.BlockState state(
            StabilizerBlock block, StabilizerStatus status) {
        return block.defaultBlockState().setValue(StabilizerBlock.STATUS, status);
    }

    record OperationSceneDefinition(
            ResourceLocation component,
            DeferredBlock<StabilizerBlock> block,
            float kineticSpeed) {}

    record ProductionSceneEquipment(
            Block mechanicalMixer,
            Block basin,
            Block fluidTank,
            Block fluidPipe,
            Block depot,
            Block deployer,
            Block mechanicalCrafter,
            Block shaft) {}
}
