package dev.upiscium.frontierprotocol.gametest;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.breach.BreachGoal;
import dev.upiscium.frontierprotocol.protection.ProtectionIndex;
import dev.upiscium.frontierprotocol.protection.ServerProtectionService;
import dev.upiscium.frontierprotocol.protection.StabilizationBeaconBlockEntity;
import dev.upiscium.frontierprotocol.registry.ModBlockTags;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BreachGameTests {
    private BreachGameTests() {}

    @GameTest(template = "empty", batch = "breachUnprotected")
    public static void taggedZombieReceivesBreachGoal(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, BlockPos.ZERO);
        boolean installed = zombie.goalSelector.getAvailableGoals().stream()
                .anyMatch(goal -> goal.getGoal() instanceof BreachGoal);
        helper.assertTrue(installed, "tagged zombie did not receive BreachGoal");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "breachUnprotected")
    public static void failedPathBreaksAllowlistedWall(GameTestHelper helper) {
        Fixture fixture = fixture(helper);
        helper.assertTrue(helper.getBlockState(fixture.wallRelative()).is(ModBlockTags.MOB_BREAKABLE),
                "fixture wall was not allowlisted");
        helper.assertTrue(!ServerProtectionService.INSTANCE.isBlockProtected(helper.getLevel(), fixture.wallAbsolute()),
                "breach fixture was unexpectedly protected");

        tick(fixture.goal(), 160);

        helper.assertBlockNotPresent(Blocks.OAK_PLANKS, fixture.wallRelative());
        helper.assertTrue(fixture.goal().breakingPos() == null, "Goal retained completed break state");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "breachUnprotected")
    public static void mobGriefingFalseRejectsBreach(GameTestHelper helper) {
        Fixture fixture = fixture(helper);
        var rule = helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        rule.set(false, helper.getLevel().getServer());
        try {
            tick(fixture.goal(), 200);
            helper.assertBlockPresent(Blocks.OAK_PLANKS, fixture.wallRelative());
            helper.assertTrue(fixture.goal().breakingPos() == null, "Goal began breaking with mobGriefing disabled");
        } finally {
            rule.set(true, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "breachProtectedInitial")
    public static void protectionRejectsBreach(GameTestHelper helper) {
        Fixture fixture = fixture(helper);
        StabilizationBeaconBlockEntity beacon = installProtectingBeacon(helper, new BlockPos(0, 1, 1));
        try {
            helper.assertTrue(ServerProtectionService.INSTANCE.isBlockProtected(
                    helper.getLevel(), fixture.wallAbsolute()), "wall was not protected by test beacon");
            tick(fixture.goal(), 200);
            helper.assertBlockPresent(Blocks.OAK_PLANKS, fixture.wallRelative());
            helper.assertTrue(fixture.goal().breakingPos() == null, "Goal began breaking a protected block");
        } finally {
            ProtectionIndex.get(helper.getLevel()).unregister(beacon);
            helper.setBlock(new BlockPos(0, 1, 1), Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "breachProtectedActivation")
    public static void protectionActivationAbortsActiveBreach(GameTestHelper helper) {
        ProtectionIndex.clear(helper.getLevel());
        Fixture fixture = fixture(helper);
        tick(fixture.goal(), 60);
        helper.assertTrue(fixture.goal().breakingPos() != null, "breach did not begin before protection activation");
        StabilizationBeaconBlockEntity beacon = installProtectingBeacon(helper, new BlockPos(0, 1, 1));
        try {
            tick(fixture.goal(), 1);
            helper.assertTrue(fixture.goal().breakingPos() == null, "active breach did not abort after protection activation");
            helper.assertBlockPresent(Blocks.OAK_PLANKS, fixture.wallRelative());
        } finally {
            ProtectionIndex.get(helper.getLevel()).unregister(beacon);
            helper.setBlock(new BlockPos(0, 1, 1), Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "breachProtectedExplosion")
    public static void hostileExplosionKeepsProtectedBlocks(GameTestHelper helper) {
        BlockPos wallRelative = new BlockPos(1, 1, 0);
        BlockPos wallAbsolute = helper.absolutePos(wallRelative);
        helper.setBlock(wallRelative, Blocks.OAK_PLANKS);
        StabilizationBeaconBlockEntity beacon = installProtectingBeacon(helper, new BlockPos(0, 1, 1));
        Creeper creeper = helper.spawn(EntityType.CREEPER, new BlockPos(2, 1, 0));
        try {
            helper.getLevel().explode(creeper, wallAbsolute.getX() + 0.5, wallAbsolute.getY() + 0.5,
                    wallAbsolute.getZ() + 0.5, 3.0F, Level.ExplosionInteraction.MOB);
            helper.assertBlockPresent(Blocks.OAK_PLANKS, wallRelative);
        } finally {
            ProtectionIndex.get(helper.getLevel()).unregister(beacon);
            helper.setBlock(new BlockPos(0, 1, 1), Blocks.AIR);
        }
        helper.succeed();
    }

    private static Fixture fixture(GameTestHelper helper) {
        BlockPos zombieRelative = new BlockPos(0, 1, 0);
        BlockPos wallRelative = new BlockPos(1, 1, 0);
        helper.setBlock(new BlockPos(0, 0, 0), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 0, 0), Blocks.STONE);
        helper.setBlock(wallRelative, Blocks.OAK_PLANKS);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, zombieRelative);
        var target = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos targetAbsolute = helper.absolutePos(new BlockPos(3, 1, 0));
        target.moveTo(targetAbsolute.getX() + 0.5, targetAbsolute.getY(), targetAbsolute.getZ() + 0.5);
        zombie.setTarget(target);
        zombie.getNavigation().stop();
        return new Fixture(wallRelative, helper.absolutePos(wallRelative), new BreachGoal(zombie));
    }

    private static void tick(BreachGoal goal, int ticks) {
        for (int tick = 0; tick < ticks; tick++) goal.tick();
    }

    private static StabilizationBeaconBlockEntity installProtectingBeacon(GameTestHelper helper, BlockPos relative) {
        helper.setBlock(relative, ModBlocks.STABILIZATION_BEACON.get());
        StabilizationBeaconBlockEntity beacon = helper.getBlockEntity(relative);
        CompoundTag state = new CompoundTag();
        state.putInt("FuelTicks", 400);
        beacon.loadCustomOnly(state, helper.getLevel().registryAccess());
        ProtectionIndex.get(helper.getLevel()).register(beacon);
        return beacon;
    }

    private record Fixture(BlockPos wallRelative, BlockPos wallAbsolute, BreachGoal goal) {}
}
