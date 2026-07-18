package dev.upiscium.frontierprotocol.gametest;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.infection.ChunkInfectionState;
import dev.upiscium.frontierprotocol.infection.InfectionNestBlockEntity;
import dev.upiscium.frontierprotocol.infection.InfectionRuntimeIndex;
import dev.upiscium.frontierprotocol.infection.InfectionService;
import dev.upiscium.frontierprotocol.mob.MobScalingState;
import dev.upiscium.frontierprotocol.registry.ModAttachments;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.protection.ProtectionIndex;
import dev.upiscium.frontierprotocol.protection.ServerProtectionService;
import dev.upiscium.frontierprotocol.protection.StabilizationBeaconBlockEntity;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InfectionGameTests {
    private InfectionGameTests() {}

    @GameTest(template = "empty", batch = "infectionProtected")
    public static void initialProtectionStopsCarrierPressure(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Zombie carrier = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        carrier.setData(ModAttachments.MOB_SCALING, new MobScalingState(false, 0, true, Optional.empty()));
        InfectionRuntimeIndex.get(level).registerCarrier(carrier);
        ChunkPos pos = carrier.chunkPosition();
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        helper.assertTrue(chunk != null, "test chunk was not loaded");
        chunk.setData(ModAttachments.CHUNK_INFECTION, ChunkInfectionState.DEFAULT);
        BlockPos beaconPos = new BlockPos(2, 1, 2);
        StabilizationBeaconBlockEntity beacon = installProtectingBeacon(helper, beaconPos);
        try {
            helper.assertTrue(ServerProtectionService.INSTANCE.isChunkProtected(level, pos), "test chunk was not protected");
            InfectionService.processLoadedChunk(level, pos);
            helper.assertTrue(chunk.getData(ModAttachments.CHUNK_INFECTION).pressure() == 0,
                    "protected carrier increased infection pressure");
        } finally {
            removeBeacon(helper, beaconPos, beacon);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "infectionProtected")
    public static void initialProtectionStopsCoreMaturation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos absolute = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.INFECTION_CORE.get());
        BlockPos beaconPos = new BlockPos(2, 1, 2);
        StabilizationBeaconBlockEntity beacon = installProtectingBeacon(helper, beaconPos);
        ChunkPos pos = new ChunkPos(absolute);
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        ChunkInfectionState state = new ChunkInfectionState(80, Optional.of(absolute), 23950, Optional.empty());
        chunk.setData(ModAttachments.CHUNK_INFECTION, state);
        InfectionRuntimeIndex.get(level).markPersistentChunk(pos, true);

        try {
            InfectionService.processLoadedChunk(level, pos);
            helper.assertTrue(chunk.getData(ModAttachments.CHUNK_INFECTION).activeLoadedTicks() == 23950,
                    "protected core maturation advanced");
            helper.assertBlockPresent(ModBlocks.INFECTION_CORE.get(), new BlockPos(1, 1, 1));
        } finally {
            removeBeacon(helper, beaconPos, beacon);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "infectionProtected")
    public static void initialProtectionStopsNestScheduleAndSpawn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos relative = new BlockPos(1, 1, 1);
        BlockPos absolute = helper.absolutePos(relative);
        helper.setBlock(relative, ModBlocks.INFECTION_NEST.get());
        BlockPos beaconPos = new BlockPos(2, 1, 2);
        StabilizationBeaconBlockEntity beacon = installProtectingBeacon(helper, beaconPos);
        InfectionNestBlockEntity nest = helper.getBlockEntity(relative);
        ChunkPos pos = new ChunkPos(absolute);
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        chunk.setData(ModAttachments.CHUNK_INFECTION,
                new ChunkInfectionState(100, Optional.of(absolute), 24000, Optional.of(nest.nestId())));
        long scheduled = nest.nextSpawnGameTime();

        try {
            InfectionService.processLoadedChunk(level, pos);
            helper.assertTrue(nest.nextSpawnGameTime() == scheduled, "protected nest scheduled a spawn attempt");
            helper.assertEntityNotPresent(EntityType.ZOMBIE);
        } finally {
            removeBeacon(helper, beaconPos, beacon);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "infectionProgression")
    public static void loadedUnprotectedCoreMatures(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos relative = new BlockPos(1, 1, 1);
        BlockPos absolute = helper.absolutePos(relative);
        helper.setBlock(relative, ModBlocks.INFECTION_CORE.get());
        ChunkPos pos = new ChunkPos(absolute);
        helper.assertTrue(!ServerProtectionService.INSTANCE.isChunkProtected(level, pos),
                "progression test chunk was unexpectedly protected");
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        chunk.setData(ModAttachments.CHUNK_INFECTION,
                new ChunkInfectionState(80, Optional.of(absolute), 23950, Optional.empty()));

        InfectionService.processLoadedChunk(level, pos);

        helper.assertBlockPresent(ModBlocks.INFECTION_NEST.get(), relative);
        helper.assertTrue(chunk.getData(ModAttachments.CHUNK_INFECTION).nestId().isPresent(),
                "matured nest identity was not persisted in the chunk");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "infectionProgression")
    public static void dueUnprotectedNestSchedulesOneAttempt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos relative = new BlockPos(1, 1, 1);
        BlockPos absolute = helper.absolutePos(relative);
        helper.setBlock(relative, ModBlocks.INFECTION_NEST.get());
        InfectionNestBlockEntity nest = helper.getBlockEntity(relative);
        CompoundTag saved = new CompoundTag();
        saved.putUUID("NestId", nest.nestId());
        saved.putLong("NextSpawnGameTime", level.getGameTime());
        nest.loadCustomOnly(saved, level.registryAccess());
        InfectionRuntimeIndex.get(level).registerNest(nest);
        ChunkPos pos = new ChunkPos(absolute);
        helper.assertTrue(!ServerProtectionService.INSTANCE.isChunkProtected(level, pos),
                "progression test chunk was unexpectedly protected");
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        chunk.setData(ModAttachments.CHUNK_INFECTION,
                new ChunkInfectionState(100, Optional.of(absolute), 24000, Optional.of(nest.nestId())));

        InfectionService.processLoadedChunk(level, pos);

        helper.assertTrue(nest.nextSpawnGameTime() > level.getGameTime(),
                "due nest did not schedule its next bounded attempt");
        helper.succeed();
    }

    private static StabilizationBeaconBlockEntity installProtectingBeacon(GameTestHelper helper, BlockPos relative) {
        helper.setBlock(relative, ModBlocks.STABILIZATION_BEACON.get());
        StabilizationBeaconBlockEntity beacon = helper.getBlockEntity(relative);
        CompoundTag state = new CompoundTag();
        state.putInt("FuelTicks", 200);
        beacon.loadCustomOnly(state, helper.getLevel().registryAccess());
        ProtectionIndex.get(helper.getLevel()).register(beacon);
        return beacon;
    }

    private static void removeBeacon(GameTestHelper helper, BlockPos relative, StabilizationBeaconBlockEntity beacon) {
        ProtectionIndex.get(helper.getLevel()).unregister(beacon);
        helper.setBlock(relative, net.minecraft.world.level.block.Blocks.AIR);
    }
}
