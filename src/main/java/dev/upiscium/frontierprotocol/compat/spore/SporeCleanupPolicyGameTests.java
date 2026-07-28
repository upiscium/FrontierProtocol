package dev.upiscium.frontierprotocol.compat.spore;

import com.Harbinger.Spore.Sblocks.GenericFoliageBlock;
import com.Harbinger.Spore.core.Sblocks;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.registry.ModBlockTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SporeCleanupPolicyGameTests {
    private SporeCleanupPolicyGameTests() {}

    @GameTest(template = "empty", batch = "spore_cleanup_policy")
    public static void auditedTagsAndPolicyResolve(GameTestHelper helper) {
        assertTagContract(helper);

        BlockState removable = Sblocks.GROWTHS_BIG.get().defaultBlockState();
        helper.assertTrue(removable.is(ModBlockTags.CLEANUP_REMOVABLE), "audited foliage is not removable");
        helper.assertTrue(
                SporeCleanupPolicy.replacementFor(removable).orElseThrow().isAir(),
                "dry audited foliage does not resolve to air");

        BlockState waterlogged = Sblocks.GROWTHS_SMALL
                .get()
                .defaultBlockState()
                .setValue(GenericFoliageBlock.WATERLOGGED, true);
        helper.assertTrue(
                SporeCleanupPolicy.replacementFor(waterlogged).orElseThrow().is(Blocks.WATER),
                "waterlogged audited foliage does not resolve to water");

        assertKept(helper, Sblocks.CDU.get().defaultBlockState(), "CDU");
        assertKept(helper, Sblocks.CONTAINER.get().defaultBlockState(), "container");
        assertKept(helper, Sblocks.HIVE_SPAWN.get().defaultBlockState(), "hive spawn");
        assertKept(helper, Sblocks.INFESTED_STONE.get().defaultBlockState(), "infected structure block");
        assertKept(helper, Sblocks.BIOMASS_BLOCK.get().defaultBlockState(), "biomass structure block");
        assertHazardKept(helper, Sblocks.BLOOM_G.get().defaultBlockState(), "blomfung");
        assertHazardKept(helper, Sblocks.BLOOM_GG.get().defaultBlockState(), "bloomfung2");
        assertHazardKept(helper, Sblocks.FUNGAL_CLAMP.get().defaultBlockState(), "fungal clamp");

        BlockState unlistedSporeBlock = Sblocks.ACID.get().defaultBlockState();
        helper.assertFalse(
                unlistedSporeBlock.is(ModBlockTags.CLEANUP_REMOVABLE),
                "Spore namespace alone made an unlisted block removable");
        helper.assertTrue(
                SporeCleanupPolicy.replacementFor(unlistedSporeBlock).isEmpty(),
                "Spore namespace alone produced a cleanup replacement");
        helper.succeed();
    }

    private static void assertTagContract(GameTestHelper helper) {
        for (Block block : BuiltInRegistries.BLOCK) {
            BlockState state = block.defaultBlockState();
            boolean removable = state.is(ModBlockTags.CLEANUP_REMOVABLE);
            boolean never = state.is(ModBlockTags.CLEANUP_NEVER);
            String id = BuiltInRegistries.BLOCK.getKey(block).toString();

            helper.assertFalse(removable && never, id + " is in both cleanup tags");
            if (removable) {
                helper.assertFalse(state.hasBlockEntity(), id + " declares a Block Entity but is removable");
            }
        }
    }

    private static void assertHazardKept(GameTestHelper helper, BlockState state, String description) {
        helper.assertFalse(
                state.is(ModBlockTags.CLEANUP_REMOVABLE), description + " remains in cleanup/removable");
        assertKept(helper, state, description);
    }

    private static void assertKept(GameTestHelper helper, BlockState state, String description) {
        helper.assertTrue(state.is(ModBlockTags.CLEANUP_NEVER), description + " is missing from cleanup/never");
        helper.assertTrue(
                SporeCleanupPolicy.replacementFor(state).isEmpty(), description + " received a cleanup replacement");
    }
}
