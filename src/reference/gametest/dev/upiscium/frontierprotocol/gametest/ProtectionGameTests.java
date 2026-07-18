package dev.upiscium.frontierprotocol.gametest;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.protection.BeaconStatus;
import dev.upiscium.frontierprotocol.protection.ServerProtectionService;
import dev.upiscium.frontierprotocol.protection.StabilizationBeaconBlockEntity;
import dev.upiscium.frontierprotocol.protection.StabilizationBeaconBlock;
import dev.upiscium.frontierprotocol.protection.ProtectionIndex;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.world.FrontierProtocolWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProtectionGameTests {
    private ProtectionGameTests() {}

    @GameTest(template = "empty")
    public static void initialSpawnProtectionHasFiveByFiveBoundary(GameTestHelper helper) {
        var level = helper.getLevel().getServer().overworld();
        FrontierProtocolWorldData data = FrontierProtocolWorldData.get(level);
        ChunkPos origin = new ChunkPos(data.originChunkX(), data.originChunkZ());
        helper.assertTrue(ServerProtectionService.INSTANCE.isChunkProtected(level,
                new ChunkPos(origin.x + 2, origin.z - 2)), "initial boundary was not protected");
        helper.assertTrue(!ServerProtectionService.INSTANCE.isChunkProtected(level,
                new ChunkPos(origin.x + 3, origin.z)), "chunk beyond initial boundary was protected");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void fueledBeaconProtectsThreeByThreeAndEntersGrace(GameTestHelper helper) {
        BlockPos relative = new BlockPos(0, 0, 0);
        helper.setBlock(relative, ModBlocks.STABILIZATION_BEACON.get());
        StabilizationBeaconBlockEntity beacon = helper.getBlockEntity(relative);
        CompoundTag state = new CompoundTag();
        state.putInt("FuelTicks", 2);
        beacon.loadCustomOnly(state, helper.getLevel().registryAccess());
        beacon.setChanged();

        helper.succeedWhen(() -> {
            ChunkPos beaconChunk = new ChunkPos(beacon.getBlockPos());
            helper.assertTrue(ServerProtectionService.INSTANCE.isChunkProtected(helper.getLevel(),
                    new ChunkPos(beaconChunk.x + 1, beaconChunk.z + 1)), "beacon boundary was not protected");
            helper.assertTrue(ProtectionIndex.get(helper.getLevel()).findProtecting(
                    new ChunkPos(beaconChunk.x + 2, beaconChunk.z), 1).isEmpty(),
                    "chunk beyond beacon boundary was protected by the beacon");
            helper.assertTrue(beacon.status() == BeaconStatus.GRACE, "beacon did not enter grace after fuel exhaustion");
        });
    }

    @GameTest(template = "empty")
    public static void fuelAndGraceSurviveNbtRoundTrip(GameTestHelper helper) {
        BlockPos relative = new BlockPos(0, 0, 0);
        helper.setBlock(relative, ModBlocks.STABILIZATION_BEACON.get());
        StabilizationBeaconBlockEntity original = helper.getBlockEntity(relative);
        CompoundTag state = new CompoundTag();
        state.putInt("FuelTicks", 120);
        state.putInt("GraceTicksRemaining", 7);
        original.loadCustomOnly(state, helper.getLevel().registryAccess());

        CompoundTag saved = original.saveCustomOnly(helper.getLevel().registryAccess());
        helper.setBlock(relative, net.minecraft.world.level.block.Blocks.AIR);
        helper.setBlock(relative, ModBlocks.STABILIZATION_BEACON.get());
        StabilizationBeaconBlockEntity restored = helper.getBlockEntity(relative);
        restored.loadCustomOnly(saved, helper.getLevel().registryAccess());

        helper.assertTrue(restored.fuelTicks() == 120, "fuel ticks were not restored");
        helper.assertTrue(restored.graceTicksRemaining() == 7, "grace ticks were not restored");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disabledBeaconStopsProtectingWithoutConsumingFuel(GameTestHelper helper) {
        BlockPos relative = new BlockPos(0, 0, 0);
        helper.setBlock(relative, ModBlocks.STABILIZATION_BEACON.get().defaultBlockState()
                .setValue(StabilizationBeaconBlock.ENABLED, false));
        StabilizationBeaconBlockEntity beacon = helper.getBlockEntity(relative);
        CompoundTag state = new CompoundTag();
        state.putInt("FuelTicks", 20);
        beacon.loadCustomOnly(state, helper.getLevel().registryAccess());

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(beacon.fuelTicks() == 20, "disabled beacon consumed fuel");
            helper.assertTrue(beacon.status() == BeaconStatus.OFFLINE, "disabled beacon was not offline");
            var source = ServerProtectionService.INSTANCE.findSource(
                    helper.getLevel(), new ChunkPos(beacon.getBlockPos()));
            helper.assertTrue(source.flatMap(protection -> protection.blockPos())
                    .filter(beacon.getBlockPos()::equals).isEmpty(), "disabled beacon was still returned as a protection source");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void graceExpiresIntoOfflineState(GameTestHelper helper) {
        BlockPos relative = new BlockPos(0, 0, 0);
        helper.setBlock(relative, ModBlocks.STABILIZATION_BEACON.get());
        StabilizationBeaconBlockEntity beacon = helper.getBlockEntity(relative);
        CompoundTag state = new CompoundTag();
        state.putInt("GraceTicksRemaining", 2);
        beacon.loadCustomOnly(state, helper.getLevel().registryAccess());

        helper.succeedWhen(() -> helper.assertTrue(
                beacon.status() == BeaconStatus.OFFLINE, "beacon grace had not expired"));
    }
}
