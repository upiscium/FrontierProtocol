package dev.upiscium.frontierprotocol;

import com.Harbinger.Spore.Sblocks.CDUBlock;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.Harbinger.Spore.core.Sblocks;
import com.Harbinger.Spore.core.Sentities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class SporeGameTestAssertions {
    private SporeGameTestAssertions() {}

    public static void assertProtoMutationBlocked(
            GameTestHelper helper, ServerLevel level, BlockPos target, String message) {
        assertProtoMutation(helper, level, target, false, message);
    }

    public static void assertProtoMutationAllowed(
            GameTestHelper helper, ServerLevel level, BlockPos target, String message) {
        assertProtoMutation(helper, level, target, true, message);
    }

    private static void assertProtoMutation(
            GameTestHelper helper, ServerLevel level, BlockPos target, boolean expectedLit, String message) {
        BlockState cdu = Sblocks.CDU.get().defaultBlockState();
        level.setBlock(target, cdu, Block.UPDATE_NONE);
        new Proto(Sentities.PROTO.get(), level).SpreadFoliageAndConvert(level, cdu, target);
        helper.assertTrue(level.getBlockState(target).getValue(CDUBlock.LIT) == expectedLit, message);
        level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
    }
}
