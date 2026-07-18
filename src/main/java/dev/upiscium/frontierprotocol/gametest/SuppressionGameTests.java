package dev.upiscium.frontierprotocol.gametest;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
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
import net.minecraft.resources.ResourceLocation;
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
        ServerLevel overworld = helper.getLevel().getServer().overworld();
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "Nether level is unavailable");
        ServerInfectionSuppressionService service = ServerInfectionSuppressionService.INSTANCE;

        service.clear(overworld);
        SpawnProtectionManager.rebuild(overworld);
        SpawnProtectionSavedData spawnData = SpawnProtectionSavedData.get(overworld);
        ChunkPos center = spawnData.centerChunk();
        helper.assertTrue(spawnData.enabled(), "spawn protection should be enabled by default");
        helper.assertTrue(spawnData.radiusChunks() == 2, "spawn protection should default to radius 2");

        ChunkPos corner = new ChunkPos(center.x + 2, center.z - 2);
        BlockPos bottom = new BlockPos(corner.getMinBlockX(), overworld.getMinBuildHeight(), corner.getMinBlockZ());
        BlockPos top = new BlockPos(corner.getMaxBlockX(), overworld.getMaxBuildHeight() - 1, corner.getMaxBlockZ());
        helper.assertTrue(service.isSuppressed(overworld, bottom), "5x5 corner is not suppressed at minimum height");
        helper.assertTrue(service.isSuppressed(overworld, top), "5x5 corner is not suppressed at maximum height");
        helper.assertTrue(!service.isSuppressed(overworld, new ChunkPos(center.x + 3, center.z)),
                "chunk outside the 5x5 area is suppressed");
        helper.assertTrue(service.getSources(overworld, center).contains(SpawnProtectionManager.SOURCE),
                "spawn suppression source is missing");

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

        boolean originalEnabled = FrontierProtocolServerConfig.SPAWN_PROTECTION_ENABLED.get();
        int originalRadius = FrontierProtocolServerConfig.SPAWN_PROTECTION_RADIUS_CHUNKS.getAsInt();
        try {
            FrontierProtocolServerConfig.SPAWN_PROTECTION_RADIUS_CHUNKS.set(1);
            SpawnProtectionManager.rebuild(overworld);
            helper.assertTrue(service.isSuppressed(overworld, new ChunkPos(center.x + 1, center.z)),
                    "configured radius does not include its boundary");
            helper.assertTrue(!service.isSuppressed(overworld, new ChunkPos(center.x + 2, center.z)),
                    "configured radius did not replace the prior coverage");

            FrontierProtocolServerConfig.SPAWN_PROTECTION_ENABLED.set(false);
            SpawnProtectionManager.rebuild(overworld);
            helper.assertTrue(!service.isSuppressed(overworld, center), "disabled spawn protection remains registered");
        } finally {
            FrontierProtocolServerConfig.SPAWN_PROTECTION_ENABLED.set(originalEnabled);
            FrontierProtocolServerConfig.SPAWN_PROTECTION_RADIUS_CHUNKS.set(originalRadius);
            SpawnProtectionManager.rebuild(overworld);
        }
        helper.succeed();
    }

    private static SuppressionSource source(String path) {
        return new SuppressionSource(
                new SuppressionSourceId(ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, path)),
                SuppressionSourceType.EXTERNAL);
    }
}
