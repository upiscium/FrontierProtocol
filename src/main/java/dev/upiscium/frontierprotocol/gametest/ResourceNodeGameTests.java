package dev.upiscium.frontierprotocol.gametest;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.protection.ProtectionIndex;
import dev.upiscium.frontierprotocol.protection.StabilizationBeaconBlockEntity;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.resource.ResourceNodeBlockEntity;
import dev.upiscium.frontierprotocol.resource.ResourceNodeStatus;
import dev.upiscium.frontierprotocol.sector.SectorPos;
import dev.upiscium.frontierprotocol.world.FrontierProtocolWorldData;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResourceNodeGameTests {
    private static final ResourceLocation FERROUS_NODE = id("ferrous_node");
    private static final ResourceLocation BIOMASS_NODE = id("biomass_node");
    private static final ResourceLocation FERROUS_TRAIT = id("ferrous_strata");
    private static final ResourceLocation FERTILE_TRAIT = id("fertile_basin");

    private ResourceNodeGameTests() {}

    @GameTest(template = "empty", batch = "resourceNodeMatching")
    public static void matchingTraitProducesOutput(GameTestHelper helper) {
        ResourceNodeBlockEntity node = placeUnboundNode(helper);
        withTrait(helper, node.getBlockPos(), FERTILE_TRAIT, () -> tick(helper, node, 200));
        helper.assertTrue(node.definitionId().filter(BIOMASS_NODE::equals).isPresent(),
                "node did not bind from the sector trait definition");
        helper.assertTrue(node.output().getStackInSlot(0).is(Items.WHEAT), "matching node did not produce output");
        helper.assertTrue(node.status() == ResourceNodeStatus.WORKING, "matching node was not working");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "resourceNodeNormal")
    public static void relocatedNodeStopsInNormalSector(GameTestHelper helper) {
        ResourceNodeBlockEntity node = placeNode(helper, FERROUS_NODE);
        withTrait(helper, node.getBlockPos(), id("normal"), () -> tick(helper, node, 250));
        helper.assertTrue(node.output().getStackInSlot(0).isEmpty(), "relocated node produced in a normal sector");
        helper.assertTrue(node.status() == ResourceNodeStatus.WRONG_TRAIT, "normal sector did not stop the node");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "resourceNodeFull")
    public static void fullOutputPausesWithoutLosingProgress(GameTestHelper helper) {
        ResourceNodeBlockEntity node = placeNode(helper, BIOMASS_NODE);
        node.output().setStackInSlot(0, new net.minecraft.world.item.ItemStack(Items.WHEAT, 64));
        withTrait(helper, node.getBlockPos(), FERTILE_TRAIT, () -> tick(helper, node, 20));
        helper.assertTrue(node.progress() == 0, "full output advanced work progress");
        helper.assertTrue(node.status() == ResourceNodeStatus.OUTPUT_FULL, "full output reason was not exposed");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "resourceNodeWrong")
    public static void relocatedNodeStopsInWrongTrait(GameTestHelper helper) {
        ResourceNodeBlockEntity node = placeNode(helper, FERROUS_NODE);
        withTrait(helper, node.getBlockPos(), FERTILE_TRAIT, () -> tick(helper, node, 250));
        helper.assertTrue(node.output().getStackInSlot(0).isEmpty(), "node produced in the wrong trait");
        helper.assertTrue(node.status() == ResourceNodeStatus.WRONG_TRAIT, "wrong trait reason was not exposed");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "resourceNodeProtection")
    public static void requiredProtectionPausesAndResumes(GameTestHelper helper) {
        ProtectionIndex.clear(helper.getLevel());
        ResourceNodeBlockEntity node = placeNode(helper, FERROUS_NODE);
        withTrait(helper, node.getBlockPos(), FERROUS_TRAIT, () -> {
            tick(helper, node, 20);
            helper.assertTrue(node.progress() == 0, "unprotected node advanced progress");
            helper.assertTrue(node.status() == ResourceNodeStatus.PROTECTION_REQUIRED,
                    "missing protection reason was not exposed");
            BlockPos beaconPos = new BlockPos(1, 0, 0);
            StabilizationBeaconBlockEntity beacon = installBeacon(helper, beaconPos);
            try {
                tick(helper, node, 200);
                helper.assertTrue(node.output().getStackInSlot(0).is(Items.RAW_IRON),
                        "protected node did not resume production");
            } finally {
                ProtectionIndex.get(helper.getLevel()).unregister(beacon);
                helper.setBlock(beaconPos, Blocks.AIR);
            }
        });
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "resourceNodePersistence")
    public static void outputProgressAndDefinitionPersist(GameTestHelper helper) {
        ResourceNodeBlockEntity node = placeNode(helper, BIOMASS_NODE);
        withTrait(helper, node.getBlockPos(), FERTILE_TRAIT, () -> tick(helper, node, 210));
        CompoundTag saved = node.saveCustomOnly(helper.getLevel().registryAccess());
        BlockPos relative = BlockPos.ZERO;
        helper.setBlock(relative, Blocks.AIR);
        helper.setBlock(relative, ModBlocks.RESOURCE_NODE.get());
        ResourceNodeBlockEntity restored = helper.getBlockEntity(relative);
        restored.loadCustomOnly(saved, helper.getLevel().registryAccess());
        helper.assertTrue(restored.definitionId().filter(BIOMASS_NODE::equals).isPresent(), "definition ID was not restored");
        helper.assertTrue(restored.progress() == 10, "work progress was not restored");
        helper.assertTrue(restored.output().getStackInSlot(0).is(Items.WHEAT), "output was not restored");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "resourceNodeTags")
    public static void nodeIsCreateNonMovable(GameTestHelper helper) {
        TagKey<Block> nonMovable = TagKey.create(Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath("create", "non_movable"));
        helper.assertTrue(ModBlocks.RESOURCE_NODE.get().defaultBlockState().is(nonMovable),
                "resource node was not tagged create:non_movable");
        helper.succeed();
    }

    private static ResourceNodeBlockEntity placeNode(GameTestHelper helper, ResourceLocation definition) {
        ResourceNodeBlockEntity node = placeUnboundNode(helper);
        node.setDefinitionId(definition);
        return node;
    }

    private static ResourceNodeBlockEntity placeUnboundNode(GameTestHelper helper) {
        helper.setBlock(BlockPos.ZERO, ModBlocks.RESOURCE_NODE.get());
        return helper.getBlockEntity(BlockPos.ZERO);
    }

    private static void tick(GameTestHelper helper, ResourceNodeBlockEntity node, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            ResourceNodeBlockEntity.serverTick(helper.getLevel(), node.getBlockPos(), node.getBlockState(), node);
        }
    }

    private static void withTrait(GameTestHelper helper, BlockPos pos, ResourceLocation trait, Runnable action) {
        FrontierProtocolWorldData data = FrontierProtocolWorldData.get(helper.getLevel());
        SectorPos sector = SectorPos.fromChunk(new ChunkPos(pos), data.sectorSizeChunks());
        Optional<ResourceLocation> previous = Optional.ofNullable(data.forcedTraitOverrides().get(sector));
        data.setOverride(sector, trait);
        try {
            action.run();
        } finally {
            if (previous.isPresent()) data.setOverride(sector, previous.get()); else data.clearOverride(sector);
        }
    }

    private static StabilizationBeaconBlockEntity installBeacon(GameTestHelper helper, BlockPos relative) {
        helper.setBlock(relative, ModBlocks.STABILIZATION_BEACON.get());
        StabilizationBeaconBlockEntity beacon = helper.getBlockEntity(relative);
        CompoundTag state = new CompoundTag();
        state.putInt("FuelTicks", 400);
        beacon.loadCustomOnly(state, helper.getLevel().registryAccess());
        ProtectionIndex.get(helper.getLevel()).register(beacon);
        return beacon;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, path);
    }
}
