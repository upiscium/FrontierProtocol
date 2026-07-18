package dev.upiscium.frontierprotocol;

import dev.upiscium.frontierprotocol.api.suppression.SuppressionSource;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceId;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceType;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.spawnprotection.SpawnProtectionManager;
import dev.upiscium.frontierprotocol.spawnprotection.SpawnProtectionSavedData;
import dev.upiscium.frontierprotocol.suppression.ServerInfectionSuppressionService;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SuppressionGameTests {
    private SuppressionGameTests() {}

    @GameTest(template = "empty")
    public static void suppressionIndexHandlesSpawnOverlapBoundariesAndDimensions(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel overworld = server.overworld();
        ServerLevel nether = server.getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "Nether level is unavailable");
        ServerInfectionSuppressionService service = ServerInfectionSuppressionService.INSTANCE;
        boolean originalEnabled = FrontierProtocolServerConfig.SPAWN_PROTECTION_ENABLED.get();
        int originalRadius = FrontierProtocolServerConfig.SPAWN_PROTECTION_RADIUS_CHUNKS.getAsInt();
        BlockPos originalSpawn = overworld.getSharedSpawnPos();
        float originalSpawnAngle = overworld.getSharedSpawnAngle();

        try {
            assertOverworldOnly(helper, nether);
            FrontierProtocolServerConfig.SPAWN_PROTECTION_ENABLED.set(true);
            FrontierProtocolServerConfig.SPAWN_PROTECTION_RADIUS_CHUNKS.set(2);
            service.clear(overworld);
            SpawnProtectionManager.rebuild(overworld);
            SpawnProtectionSavedData savedData = SpawnProtectionSavedData.get(overworld);
            ChunkPos center = savedData.centerChunk();
            CompoundTag savedDataBeforeReload = savedData.save(new CompoundTag(), overworld.registryAccess());

            assertFiveByFive(helper, overworld, center, service);
            assertOverlapAndDimensions(helper, overworld, nether, service);

            FrontierProtocolServerConfig.SPAWN_PROTECTION_RADIUS_CHUNKS.set(1);
            SuppressionConfigEvents.queueRebuild(server);
            helper.runAfterDelay(1, () -> verifyRadiusReload(
                    helper, server, overworld, service, center, savedDataBeforeReload,
                    originalEnabled, originalRadius, originalSpawn, originalSpawnAngle));
        } catch (RuntimeException error) {
            restore(overworld, originalEnabled, originalRadius, originalSpawn, originalSpawnAngle);
            throw error;
        }
    }

    private static void verifyRadiusReload(
            GameTestHelper helper,
            MinecraftServer server,
            ServerLevel overworld,
            ServerInfectionSuppressionService service,
            ChunkPos center,
            CompoundTag savedDataBeforeReload,
            boolean originalEnabled,
            int originalRadius,
            BlockPos originalSpawn,
            float originalSpawnAngle) {
        try {
            helper.assertTrue(service.isSuppressed(overworld, new ChunkPos(center.x + 1, center.z)),
                    "reload radius does not include its boundary");
            helper.assertTrue(!service.isSuppressed(overworld, new ChunkPos(center.x + 2, center.z)),
                    "reload left the old radius registered");

            FrontierProtocolServerConfig.SPAWN_PROTECTION_ENABLED.set(false);
            SuppressionConfigEvents.queueRebuild(server);
            helper.runAfterDelay(1, () -> verifyDisabledReload(
                    helper, server, overworld, service, center, savedDataBeforeReload,
                    originalEnabled, originalRadius, originalSpawn, originalSpawnAngle));
        } catch (RuntimeException error) {
            restore(overworld, originalEnabled, originalRadius, originalSpawn, originalSpawnAngle);
            throw error;
        }
    }

    private static void verifyDisabledReload(
            GameTestHelper helper,
            MinecraftServer server,
            ServerLevel overworld,
            ServerInfectionSuppressionService service,
            ChunkPos center,
            CompoundTag savedDataBeforeReload,
            boolean originalEnabled,
            int originalRadius,
            BlockPos originalSpawn,
            float originalSpawnAngle) {
        try {
            helper.assertTrue(!service.isSuppressed(overworld, center),
                    "reload did not unregister disabled spawn protection");
            CompoundTag savedDataAfterDisable = SpawnProtectionSavedData.get(overworld)
                    .save(new CompoundTag(), overworld.registryAccess());
            helper.assertTrue(savedDataAfterDisable.equals(savedDataBeforeReload),
                    "config reload changed initial spawn SavedData");
            FrontierProtocolServerConfig.SPAWN_PROTECTION_ENABLED.set(true);
            FrontierProtocolServerConfig.SPAWN_PROTECTION_RADIUS_CHUNKS.set(2);
            SuppressionConfigEvents.queueRebuild(server);
            helper.runAfterDelay(1, () -> verifyReenabledAndRestarted(
                    helper, overworld, service, center,
                    originalEnabled, originalRadius, originalSpawn, originalSpawnAngle));
        } catch (RuntimeException error) {
            restore(overworld, originalEnabled, originalRadius, originalSpawn, originalSpawnAngle);
            throw error;
        }
    }

    private static void verifyReenabledAndRestarted(
            GameTestHelper helper,
            ServerLevel overworld,
            ServerInfectionSuppressionService service,
            ChunkPos center,
            boolean originalEnabled,
            int originalRadius,
            BlockPos originalSpawn,
            float originalSpawnAngle) {
        try {
            helper.assertTrue(service.isSuppressed(overworld, new ChunkPos(center.x + 2, center.z)),
                    "reload did not re-register the saved center with the current radius");

            ChunkPos movedSpawn = new ChunkPos(center.x + 100, center.z + 100);
            overworld.setDefaultSpawnPos(new BlockPos(movedSpawn.getMinBlockX(), 64, movedSpawn.getMinBlockZ()), 0.0F);
            service.clear(overworld);
            SpawnProtectionManager.rebuild(overworld);

            helper.assertTrue(service.isSuppressed(overworld, center),
                    "runtime rebuild did not restore the saved initial center");
            helper.assertTrue(!service.isSuppressed(overworld, movedSpawn),
                    "runtime rebuild moved protection to the current world spawn");
            helper.assertTrue(SpawnProtectionSavedData.get(overworld).centerChunk().equals(center),
                    "saved initial center changed after setworldspawn-equivalent update");
        } finally {
            restore(overworld, originalEnabled, originalRadius, originalSpawn, originalSpawnAngle);
        }
        helper.succeed();
    }

    private static void assertOverworldOnly(GameTestHelper helper, ServerLevel nether) {
        try {
            SpawnProtectionManager.rebuild(nether);
            helper.assertTrue(false, "manager accepted a non-Overworld level");
        } catch (IllegalArgumentException expected) {
            // Expected explicit rejection.
        }
        try {
            SpawnProtectionSavedData.get(nether);
            helper.assertTrue(false, "SavedData accepted a non-Overworld level");
        } catch (IllegalArgumentException expected) {
            // Expected explicit rejection.
        }
    }

    private static void assertFiveByFive(
            GameTestHelper helper,
            ServerLevel overworld,
            ChunkPos center,
            ServerInfectionSuppressionService service) {
        ChunkPos corner = new ChunkPos(center.x + 2, center.z - 2);
        BlockPos bottom = new BlockPos(corner.getMinBlockX(), overworld.getMinBuildHeight(), corner.getMinBlockZ());
        BlockPos top = new BlockPos(corner.getMaxBlockX(), overworld.getMaxBuildHeight() - 1, corner.getMaxBlockZ());
        helper.assertTrue(service.isSuppressed(overworld, bottom), "5x5 corner is not suppressed at minimum height");
        helper.assertTrue(service.isSuppressed(overworld, top), "5x5 corner is not suppressed at maximum height");
        helper.assertTrue(!service.isSuppressed(overworld, new ChunkPos(center.x + 3, center.z)),
                "chunk outside the 5x5 area is suppressed");
        helper.assertTrue(service.getSources(overworld, center).contains(SpawnProtectionManager.SOURCE),
                "spawn suppression source is missing");
    }

    private static void assertOverlapAndDimensions(
            GameTestHelper helper,
            ServerLevel overworld,
            ServerLevel nether,
            ServerInfectionSuppressionService service) {
        ChunkPos negativeTarget = new ChunkPos(-100, -100);
        SuppressionSource sourceA = source("gametest_a");
        SuppressionSource sourceB = source("gametest_b");
        try {
            service.registerOrUpdateSource(overworld, sourceA, Set.of(negativeTarget));
            service.registerOrUpdateSource(overworld, sourceB, Set.of(negativeTarget));
            helper.assertTrue(service.isSuppressed(overworld, new BlockPos(-1600, 0, -1600)),
                    "negative chunk minimum boundary is not suppressed");
            helper.assertTrue(service.isSuppressed(overworld, new BlockPos(-1585, 0, -1585)),
                    "negative chunk maximum boundary is not suppressed");
            helper.assertTrue(!service.isSuppressed(overworld, new BlockPos(-1584, 0, -1584)),
                    "adjacent chunk crossed the negative boundary");
            helper.assertTrue(!service.isSuppressed(nether, negativeTarget), "suppression leaked into another dimension");

            service.unregisterSource(overworld, sourceA.id());
            helper.assertTrue(service.isSuppressed(overworld, negativeTarget),
                    "removing one source removed overlapping suppression");
            helper.assertTrue(service.getSources(overworld, negativeTarget).equals(Set.of(sourceB)),
                    "remaining overlap source is incorrect");
        } finally {
            service.unregisterSource(overworld, sourceA.id());
            service.unregisterSource(overworld, sourceB.id());
        }
        helper.assertTrue(!service.isSuppressed(overworld, negativeTarget), "test source was not fully removed");
    }

    private static void restore(
            ServerLevel overworld,
            boolean enabled,
            int radius,
            BlockPos spawn,
            float spawnAngle) {
        FrontierProtocolServerConfig.SPAWN_PROTECTION_ENABLED.set(enabled);
        FrontierProtocolServerConfig.SPAWN_PROTECTION_RADIUS_CHUNKS.set(radius);
        overworld.setDefaultSpawnPos(spawn, spawnAngle);
        SpawnProtectionManager.rebuild(overworld);
    }

    private static SuppressionSource source(String path) {
        return new SuppressionSource(
                new SuppressionSourceId(ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, path)),
                SuppressionSourceType.EXTERNAL);
    }
}
