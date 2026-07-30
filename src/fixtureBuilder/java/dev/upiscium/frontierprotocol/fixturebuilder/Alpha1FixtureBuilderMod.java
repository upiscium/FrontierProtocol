package dev.upiscium.frontierprotocol.fixturebuilder;

import dev.upiscium.frontierprotocol.cleanup.CleanupCursor;
import dev.upiscium.frontierprotocol.cleanup.CleanupProgress;
import dev.upiscium.frontierprotocol.cleanup.InfectionCleanupSavedData;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.registry.ModItems;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerBlock;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerBlockEntity;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerStatus;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerTier;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.items.ItemStackHandler;

@Mod(Alpha1FixtureBuilderMod.MOD_ID)
public final class Alpha1FixtureBuilderMod {
    public static final String MOD_ID = "frontier_protocol_fixture_builder";
    private static final BlockPos TIER_1_POS = new BlockPos(20, 100, 84);
    private static final BlockPos TIER_2_POS = new BlockPos(22, 100, 84);
    private static final BlockPos TIER_3_POS = new BlockPos(24, 100, 84);
    private static final BlockPos CONTAINER_POS = new BlockPos(20, 100, 87);

    public Alpha1FixtureBuilderMod(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::serverStarted);
    }

    private void serverStarted(ServerStartedEvent event) {
        ServerLevel level = event.getServer().overworld();
        placePlatform(level);
        placeStabilizer(
                level,
                TIER_1_POS,
                ModBlocks.TIER_1_STABILIZER.get().defaultBlockState(),
                StabilizerTier.TIER_1,
                Direction.NORTH,
                StabilizerStatus.OFFLINE,
                0,
                0,
                0,
                16);
        placeStabilizer(
                level,
                TIER_2_POS,
                ModBlocks.TIER_2_STABILIZER.get().defaultBlockState(),
                StabilizerTier.TIER_2,
                Direction.EAST,
                StabilizerStatus.GRACE_PERIOD,
                1000,
                1800,
                1,
                4);
        placeStabilizer(
                level,
                TIER_3_POS,
                ModBlocks.TIER_3_STABILIZER.get().defaultBlockState(),
                StabilizerTier.TIER_3,
                Direction.SOUTH,
                StabilizerStatus.GRACE_PERIOD,
                2000,
                1200,
                2,
                12);
        placeContainer(level);

        long cleanupChunk = ChunkPos.asLong(7, -3);
        InfectionCleanupSavedData.get(level)
                .update(cleanupChunk, new CleanupProgress(new CleanupCursor(2, 321, false), true, -4, 24));
        level.getChunkAt(TIER_1_POS).setUnsaved(true);
        level.getChunkAt(CONTAINER_POS).setUnsaved(true);
        System.out.println("FRONTIER_PROTOCOL_ALPHA1_FIXTURE_BUILDER_COMPLETE");
    }

    private static void placePlatform(ServerLevel level) {
        for (BlockPos pos : List.of(TIER_1_POS, TIER_2_POS, TIER_3_POS, CONTAINER_POS)) {
            level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static void placeStabilizer(
            ServerLevel level,
            BlockPos pos,
            BlockState baseState,
            StabilizerTier tier,
            Direction facing,
            StabilizerStatus status,
            int graceTicks,
            int cellTicks,
            int registeredRadius,
            int cells) {
        BlockState state = baseState.setValue(StabilizerBlock.FACING, facing).setValue(StabilizerBlock.STATUS, status);
        level.setBlock(pos, state, Block.UPDATE_ALL);
        if (!(level.getBlockEntity(pos) instanceof StabilizerBlockEntity blockEntity)) {
            throw new IllegalStateException("Fixture Stabilizer Block Entity missing at " + pos);
        }
        ItemStackHandler inventory = new ItemStackHandler(1);
        inventory.setStackInSlot(0, new ItemStack(ModItems.STABILIZATION_CELL.get(), cells));
        var tag = blockEntity.saveWithFullMetadata(level.registryAccess());
        tag.putInt("schemaVersion", 1);
        tag.putString("tier", tier.serializedName());
        tag.putString("status", status.getSerializedName());
        tag.putInt("graceRemainingTicks", graceTicks);
        tag.putInt("cellRemainingTicks", cellTicks);
        tag.putInt("registeredChunkRadius", registeredRadius);
        tag.put("inventory", inventory.serializeNBT(level.registryAccess()));
        blockEntity.loadWithComponents(tag, level.registryAccess());
        blockEntity.setChanged();
    }

    private static void placeContainer(ServerLevel level) {
        level.setBlock(CONTAINER_POS, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        if (!(level.getBlockEntity(CONTAINER_POS) instanceof ChestBlockEntity chest)) {
            throw new IllegalStateException("Fixture chest missing at " + CONTAINER_POS);
        }
        setItem(chest, 0, ModItems.STABILIZATION_COMPOUND.get().getDefaultInstance(), 11);
        setItem(chest, 1, ModItems.STABILIZATION_CELL.get().getDefaultInstance(), 13);
        setItem(chest, 2, ModItems.TIER_1_STABILIZER.get().getDefaultInstance(), 1);
        setItem(chest, 3, ModItems.TIER_2_STABILIZER.get().getDefaultInstance(), 2);
        setItem(chest, 4, ModItems.TIER_3_STABILIZER.get().getDefaultInstance(), 3);
        chest.setChanged();
    }

    private static void setItem(Container container, int slot, ItemStack template, int count) {
        ItemStack stack = template.copy();
        stack.setCount(count);
        container.setItem(slot, stack);
    }
}
