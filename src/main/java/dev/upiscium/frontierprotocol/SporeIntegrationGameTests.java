package dev.upiscium.frontierprotocol;

import com.Harbinger.Spore.Sblocks.CDUBlock;
import com.Harbinger.Spore.Sentities.FoliageSpread;
import com.Harbinger.Spore.Sentities.Organoids.HiveTumor;
import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.Harbinger.Spore.core.Sblocks;
import com.Harbinger.Spore.core.Sentities;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSource;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceId;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSourceType;
import dev.upiscium.frontierprotocol.compat.spore.SporeSuppressionQueries;
import dev.upiscium.frontierprotocol.mixin.SporeMoundInvoker;
import dev.upiscium.frontierprotocol.suppression.ServerInfectionSuppressionService;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SporeIntegrationGameTests {
    private static final ChunkPos PROTECTED_CHUNK = new ChunkPos(-120, -120);
    private static final ChunkPos OUTSIDE_CHUNK = new ChunkPos(-119, -120);
    private static final ChunkPos BOUNDARY_BASE_CHUNK = new ChunkPos(-110, -110);
    private static final Set<ChunkPos> PROTECTED_CHUNKS = Set.of(
            PROTECTED_CHUNK,
            new ChunkPos(BOUNDARY_BASE_CHUNK.x, BOUNDARY_BASE_CHUNK.z - 1),
            new ChunkPos(BOUNDARY_BASE_CHUNK.x, BOUNDARY_BASE_CHUNK.z + 1),
            new ChunkPos(BOUNDARY_BASE_CHUNK.x - 1, BOUNDARY_BASE_CHUNK.z),
            new ChunkPos(BOUNDARY_BASE_CHUNK.x + 1, BOUNDARY_BASE_CHUNK.z));
    private static final SuppressionSource SOURCE = new SuppressionSource(
            new SuppressionSourceId(ResourceLocation.fromNamespaceAndPath(
                    FrontierProtocolMod.MOD_ID, "spore_integration_gametest")),
            SuppressionSourceType.EXTERNAL);

    private SporeIntegrationGameTests() {}

    @GameTest(template = "empty", batch = "spore_integration", timeoutTicks = 100)
    public static void foliageSpreadUsesActualMutationTargets(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel().getServer().overworld();
        ServerInfectionSuppressionService service = ServerInfectionSuppressionService.INSTANCE;
        BlockPos protectedPos = center(PROTECTED_CHUNK);
        BlockPos outsidePos = center(OUTSIDE_CHUNK);

        try {
            service.registerOrUpdateSource(overworld, SOURCE, PROTECTED_CHUNKS);
            helper.assertTrue(service.isSuppressed(overworld, protectedPos), "test target is not suppressed");
            helper.assertFalse(service.isSuppressed(overworld, outsidePos), "outside target is unexpectedly suppressed");

            assertSourceTargetSemantics(helper, overworld, protectedPos, outsidePos);
            assertOverrideTargets(helper, overworld, protectedPos, outsidePos);
            assertMoundTargets(helper, overworld, protectedPos, outsidePos);
            assertBranchBoundary(helper, overworld);
            assertExistingInfectionRemains(helper, overworld, protectedPos);
        } finally {
            service.unregisterSource(overworld, SOURCE.id());
            cleanup(overworld, protectedPos, outsidePos);
        }
        helper.succeed();
    }

    private static void assertSourceTargetSemantics(
            GameTestHelper helper, ServerLevel level, BlockPos protectedSource, BlockPos outsideSource) {
        Mound mound = new Mound(Sentities.MOUND.get(), level);
        StructureSpread spread = new StructureSpread((SporeMoundInvoker) mound);

        level.setBlock(spreadTarget(outsideSource), Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
        spread.target = spreadTarget(outsideSource);
        spread.SpreadInfection(level, 0.0, protectedSource);
        helper.assertTrue(!level.getBlockState(spread.target).isAir(),
                "protected source incorrectly cancelled an unprotected target");

        level.setBlock(spreadTarget(protectedSource), Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
        spread.target = spreadTarget(protectedSource);
        spread.SpreadInfection(level, 0.0, outsideSource);
        helper.assertTrue(level.getBlockState(spread.target).isAir(),
                "unprotected source mutated a protected target");

        spread.SpreadInfection(level, 0.0, protectedSource);
        helper.assertTrue(level.getBlockState(spread.target).isAir(),
                "protected source mutated a protected target");
        helper.assertTrue(spread.additionCalls == 3, "SpreadInfection was cancelled by its source position");
    }

    private static void assertOverrideTargets(
            GameTestHelper helper, ServerLevel level, BlockPos protectedPos, BlockPos outsidePos) {
        HiveTumor hiveTumor = new HiveTumor(Sentities.HIVETUMOR.get(), level);
        Proto proto = new Proto(Sentities.PROTO.get(), level);

        assertCduBlocked(helper, level, protectedPos, hiveTumor, "HiveTumor");
        assertCduBlocked(helper, level, protectedPos, proto, "Proto");
        assertCduAllowed(helper, level, outsidePos, hiveTumor, "HiveTumor");
        assertCduAllowed(helper, level, outsidePos, proto, "Proto");
    }

    private static void assertCduBlocked(
            GameTestHelper helper, ServerLevel level, BlockPos target, FoliageSpread spread, String path) {
        BlockState cdu = Sblocks.CDU.get().defaultBlockState();
        level.setBlock(target, cdu, Block.UPDATE_NONE);
        spread.SpreadFoliageAndConvert(level, cdu, target);
        helper.assertFalse(level.getBlockState(target).getValue(CDUBlock.LIT),
                path + " replaced CDU at a protected target");
    }

    private static void assertCduAllowed(
            GameTestHelper helper, ServerLevel level, BlockPos target, FoliageSpread spread, String path) {
        BlockState cdu = Sblocks.CDU.get().defaultBlockState();
        level.setBlock(target, cdu, Block.UPDATE_NONE);
        spread.SpreadFoliageAndConvert(level, cdu, target);
        helper.assertTrue(level.getBlockState(target).getValue(CDUBlock.LIT),
                path + " did not replace CDU at an unprotected target");
    }

    private static void assertMoundTargets(
            GameTestHelper helper, ServerLevel level, BlockPos protectedPos, BlockPos outsidePos) {
        SporeMoundInvoker mound = (SporeMoundInvoker) new Mound(Sentities.MOUND.get(), level);
        level.setBlock(protectedPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
        mound.frontierProtocol$invokePlaceStructureBlock(level, protectedPos);
        helper.assertTrue(level.getBlockState(protectedPos).isAir(),
                "Mound placed a structure at a protected target");

        level.setBlock(outsidePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
        mound.frontierProtocol$invokePlaceStructureBlock(level, outsidePos);
        helper.assertTrue(!level.getBlockState(outsidePos).isAir(),
                "Mound did not place a structure at an unprotected target");
    }

    private static void assertBranchBoundary(GameTestHelper helper, ServerLevel level) {
        for (Direction protectedDirection : Direction.Plane.HORIZONTAL) {
            assertBranchBoundary(helper, level, protectedDirection);
        }
    }

    private static void assertBranchBoundary(
            GameTestHelper helper, ServerLevel level, Direction protectedDirection) {
        BlockPos base = boundaryBase(protectedDirection);
        BlockPos protectedNeighbor = base.relative(protectedDirection);
        BlockPos outsideNeighbor = base.relative(protectedDirection.getOpposite());
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState();
        BlockState baseState = Blocks.STONE.defaultBlockState();
        FoliageSpread spread = new PlainSpread();

        level.setBlock(base, baseState, Block.UPDATE_NONE);
        level.setBlock(protectedNeighbor, leaves, Block.UPDATE_NONE);
        level.setBlock(outsideNeighbor, leaves, Block.UPDATE_NONE);
        for (int i = 0; i < 256 && level.getBlockState(outsideNeighbor).equals(leaves); i++) {
            spread.placeBranches(level, base, baseState);
        }

        helper.assertTrue(level.getBlockState(protectedNeighbor).equals(leaves),
                "branch placement crossed into the protected chunk toward " + protectedDirection);
        helper.assertTrue(level.getBlockState(outsideNeighbor).is(Sblocks.ROTTEN_BRANCH.get()),
                "branch guard unnecessarily blocked the unprotected neighbor opposite " + protectedDirection);
        helper.assertFalse(SporeSuppressionQueries.setBlock(
                        level, protectedNeighbor, Blocks.DIRT.defaultBlockState(), Block.UPDATE_NONE),
                "internal mutation guard accepted a protected actual target");
        helper.assertTrue(SporeSuppressionQueries.setBlock(
                        level, outsideNeighbor, Blocks.DIRT.defaultBlockState(), Block.UPDATE_NONE),
                "internal mutation guard rejected an unprotected actual target");
        level.setBlock(base, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
        level.setBlock(protectedNeighbor, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
        level.setBlock(outsideNeighbor, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
    }

    private static void assertExistingInfectionRemains(
            GameTestHelper helper, ServerLevel level, BlockPos protectedPos) {
        BlockState existingInfection = Sblocks.ROTTEN_CROPS.get().defaultBlockState();
        level.setBlock(protectedPos, existingInfection, Block.UPDATE_NONE);
        new PlainSpread().SpreadFoliageAndConvert(level, existingInfection, protectedPos);
        helper.assertTrue(level.getBlockState(protectedPos).equals(existingInfection),
                "suppression removed existing Spore infection");
    }

    private static BlockPos center(ChunkPos chunk) {
        return new BlockPos(chunk.getMinBlockX() + 8, 64, chunk.getMinBlockZ() + 8);
    }

    private static BlockPos spreadTarget(BlockPos source) {
        return source.above(8);
    }

    private static BlockPos boundaryBase(Direction protectedDirection) {
        int x = BOUNDARY_BASE_CHUNK.getMinBlockX() + 8;
        int z = BOUNDARY_BASE_CHUNK.getMinBlockZ() + 8;
        return switch (protectedDirection) {
            case NORTH -> new BlockPos(x, 64, BOUNDARY_BASE_CHUNK.getMinBlockZ());
            case SOUTH -> new BlockPos(x, 64, BOUNDARY_BASE_CHUNK.getMaxBlockZ());
            case WEST -> new BlockPos(BOUNDARY_BASE_CHUNK.getMinBlockX(), 64, z);
            case EAST -> new BlockPos(BOUNDARY_BASE_CHUNK.getMaxBlockX(), 64, z);
            default -> throw new IllegalArgumentException("horizontal direction required");
        };
    }

    private static void cleanup(ServerLevel level, BlockPos protectedPos, BlockPos outsidePos) {
        for (BlockPos pos : Set.of(
                protectedPos,
                outsidePos,
                spreadTarget(protectedPos),
                spreadTarget(outsidePos))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
        }
    }

    private static final class StructureSpread implements FoliageSpread {
        private final SporeMoundInvoker mound;
        private BlockPos target;
        private int additionCalls;

        private StructureSpread(SporeMoundInvoker mound) {
            this.mound = mound;
        }

        @Override
        public void additionPlacers(Level level, BlockPos pos, double range) {
            place(level);
        }

        @Override
        public void additionIgnoreConfigPlacers(Level level, BlockPos pos, double range) {
            place(level);
        }

        private void place(Level level) {
            additionCalls++;
            mound.frontierProtocol$invokePlaceStructureBlock(level, target);
        }
    }

    private static final class PlainSpread implements FoliageSpread {}
}
