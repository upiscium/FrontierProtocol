package dev.upiscium.frontierprotocol;

import dev.upiscium.frontierprotocol.api.ore.OreGenerationSuppressionApi;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.ore.InitialSpawnOreSuppressionManager;
import dev.upiscium.frontierprotocol.spawnprotection.SpawnProtectionSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InitialSpawnOreSuppressionGameTests {
    private static final int TEST_Y = 32;
    private static final int FILL_RADIUS = 8;
    private static final ChunkPos PROTECTED_CHUNK = new ChunkPos(-100, -100);

    private InitialSpawnOreSuppressionGameTests() {}

    @GameTest(template = "empty", batch = "ore_suppression", timeoutTicks = 100)
    public static void standardOreFeaturesRespectInitialSpawnSnapshot(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel().getServer().overworld();
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "Nether level is unavailable");

        TestContext context = new TestContext(
                helper,
                overworld,
                nether,
                FrontierProtocolServerConfig.INITIAL_SPAWN_ORE_SUPPRESSION_ENABLED.get(),
                FrontierProtocolServerConfig.INITIAL_SPAWN_ORE_SUPPRESSION_RADIUS_CHUNKS.getAsInt());
        try {
            FrontierProtocolServerConfig.INITIAL_SPAWN_ORE_SUPPRESSION_ENABLED.set(true);
            FrontierProtocolServerConfig.INITIAL_SPAWN_ORE_SUPPRESSION_RADIUS_CHUNKS.set(1);
            assertFeaturePlacementPolicy(context);

            SuppressionConfigEvents.queueRebuild(overworld.getServer());
            helper.runAfterDelay(1, () -> verifyReloadedRadius(context));
        } catch (RuntimeException error) {
            restore(context);
            throw error;
        }
    }

    private static void assertFeaturePlacementPolicy(TestContext context) {
        BlockPos oreOrigin = origin(PROTECTED_CHUNK, TEST_Y);
        BlockPos scatteredOrigin = origin(PROTECTED_CHUNK, TEST_Y + 24);
        BlockPos outsideOrigin = origin(outsideChunk(), TEST_Y);
        InitialSpawnOreSuppressionManager.publishProvisional(context.overworld(), PROTECTED_CHUNK, 0);

        prepare(context.overworld(), oreOrigin, Blocks.STONE);
        prepare(context.overworld(), scatteredOrigin, Blocks.STONE);
        prepare(context.overworld(), outsideOrigin, Blocks.STONE);
        prepare(context.nether(), oreOrigin, Blocks.NETHERRACK);

        boolean protectedOrePlaced = place(
                Feature.ORE, oreConfig(Blocks.STONE, Blocks.DIAMOND_ORE), context.overworld(), oreOrigin, 1L);
        context.helper().assertFalse(protectedOrePlaced, "OreFeature was not cancelled in the protected chunk");
        context.helper().assertTrue(count(context.overworld(), oreOrigin, Blocks.DIAMOND_ORE) == 0,
                "OreFeature mutated the protected chunk");

        boolean protectedScatteredOrePlaced = place(
                Feature.SCATTERED_ORE,
                oreConfig(Blocks.STONE, Blocks.EMERALD_ORE),
                context.overworld(),
                scatteredOrigin,
                2L);
        context.helper().assertFalse(
                protectedScatteredOrePlaced, "ScatteredOreFeature was not cancelled in the protected chunk");
        context.helper().assertTrue(count(context.overworld(), scatteredOrigin, Blocks.EMERALD_ORE) == 0,
                "ScatteredOreFeature mutated the protected chunk");

        boolean outsideOrePlaced = place(
                Feature.ORE, oreConfig(Blocks.STONE, Blocks.DIAMOND_ORE), context.overworld(), outsideOrigin, 3L);
        context.helper().assertTrue(outsideOrePlaced, "OreFeature was cancelled outside the protected radius");
        context.helper().assertTrue(count(context.overworld(), outsideOrigin, Blocks.DIAMOND_ORE) > 0,
                "OreFeature did not place outside the protected radius");

        boolean netherOrePlaced = place(
                Feature.ORE, oreConfig(Blocks.NETHERRACK, Blocks.NETHER_GOLD_ORE), context.nether(), oreOrigin, 4L);
        context.helper().assertTrue(netherOrePlaced, "OreFeature was cancelled outside the Overworld");
        context.helper().assertTrue(count(context.nether(), oreOrigin, Blocks.NETHER_GOLD_ORE) > 0,
                "OreFeature did not place outside the Overworld");

        boolean noOpPlaced = Feature.NO_OP.place(
                NoneFeatureConfiguration.INSTANCE,
                context.overworld(),
                context.overworld().getChunkSource().getGenerator(),
                RandomSource.create(5L),
                oreOrigin);
        context.helper().assertTrue(noOpPlaced, "non-ore Feature placement was affected");
    }

    private static void verifyReloadedRadius(TestContext context) {
        try {
            ChunkPos center = SpawnProtectionSavedData.get(context.overworld()).centerChunk();
            context.helper().assertTrue(isSuppressed(context.overworld(), new ChunkPos(center.x + 1, center.z)),
                    "config reload did not publish the new radius boundary");
            context.helper().assertFalse(isSuppressed(context.overworld(), new ChunkPos(center.x + 2, center.z)),
                    "config reload retained the old radius");

            FrontierProtocolServerConfig.INITIAL_SPAWN_ORE_SUPPRESSION_ENABLED.set(false);
            SuppressionConfigEvents.queueRebuild(context.overworld().getServer());
            context.helper().runAfterDelay(1, () -> verifyDisabled(context));
        } catch (RuntimeException error) {
            restore(context);
            throw error;
        }
    }

    private static void verifyDisabled(TestContext context) {
        try {
            BlockPos origin = origin(PROTECTED_CHUNK, TEST_Y);
            context.helper().assertFalse(isSuppressed(context.overworld(), PROTECTED_CHUNK),
                    "disabled config did not fail open");
            prepare(context.overworld(), origin, Blocks.STONE);
            boolean placed = place(
                    Feature.ORE, oreConfig(Blocks.STONE, Blocks.DIAMOND_ORE), context.overworld(), origin, 6L);
            context.helper().assertTrue(placed, "disabled config still cancelled OreFeature");
            context.helper().assertTrue(count(context.overworld(), origin, Blocks.DIAMOND_ORE) > 0,
                    "disabled config still prevented ore placement");
        } finally {
            restore(context);
        }
        context.helper().succeed();
    }

    private static OreConfiguration oreConfig(Block target, Block ore) {
        return new OreConfiguration(new BlockMatchTest(target), ore.defaultBlockState(), 12);
    }

    private static boolean place(
            Feature<OreConfiguration> feature,
            OreConfiguration config,
            ServerLevel level,
            BlockPos origin,
            long seed) {
        return feature.place(config, level, level.getChunkSource().getGenerator(), RandomSource.create(seed), origin);
    }

    private static void prepare(ServerLevel level, BlockPos center, Block block) {
        BlockPos.betweenClosedStream(center.offset(-FILL_RADIUS, -FILL_RADIUS, -FILL_RADIUS),
                        center.offset(FILL_RADIUS, FILL_RADIUS, FILL_RADIUS))
                .forEach(pos -> level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_NONE));
    }

    private static long count(ServerLevel level, BlockPos center, Block block) {
        return BlockPos.betweenClosedStream(center.offset(-FILL_RADIUS, -FILL_RADIUS, -FILL_RADIUS),
                        center.offset(FILL_RADIUS, FILL_RADIUS, FILL_RADIUS))
                .filter(pos -> level.getBlockState(pos).is(block))
                .count();
    }

    private static boolean isSuppressed(ServerLevel level, ChunkPos chunk) {
        return OreGenerationSuppressionApi.isSuppressed(
                level, new BlockPos(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ()));
    }

    private static BlockPos origin(ChunkPos chunk, int y) {
        return new BlockPos(chunk.getMinBlockX() + 8, y, chunk.getMinBlockZ() + 8);
    }

    private static ChunkPos outsideChunk() {
        return new ChunkPos(PROTECTED_CHUNK.x + 2, PROTECTED_CHUNK.z);
    }

    private static void restore(TestContext context) {
        FrontierProtocolServerConfig.INITIAL_SPAWN_ORE_SUPPRESSION_ENABLED.set(context.originalEnabled());
        FrontierProtocolServerConfig.INITIAL_SPAWN_ORE_SUPPRESSION_RADIUS_CHUNKS.set(context.originalRadius());
        InitialSpawnOreSuppressionManager.rebuildSnapshot(context.overworld());
        cleanup(context.overworld(), origin(PROTECTED_CHUNK, TEST_Y));
        cleanup(context.overworld(), origin(PROTECTED_CHUNK, TEST_Y + 24));
        cleanup(context.overworld(), origin(outsideChunk(), TEST_Y));
        cleanup(context.nether(), origin(PROTECTED_CHUNK, TEST_Y));
    }

    private static void cleanup(ServerLevel level, BlockPos center) {
        BlockPos.betweenClosedStream(center.offset(-FILL_RADIUS, -FILL_RADIUS, -FILL_RADIUS),
                        center.offset(FILL_RADIUS, FILL_RADIUS, FILL_RADIUS))
                .forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE));
    }

    private record TestContext(
            GameTestHelper helper,
            ServerLevel overworld,
            ServerLevel nether,
            boolean originalEnabled,
            int originalRadius) {}
}
