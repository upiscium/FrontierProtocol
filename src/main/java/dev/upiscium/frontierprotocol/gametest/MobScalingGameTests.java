package dev.upiscium.frontierprotocol.gametest;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.mob.MobScalingService;
import dev.upiscium.frontierprotocol.mob.MobScalingState;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.world.FrontierProtocolWorldData;
import dev.upiscium.frontierprotocol.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MobScalingGameTests {
    private MobScalingGameTests() {}

    @GameTest(template = "empty")
    public static void naturalZombieIsScaledExactlyOnce(GameTestHelper helper) {
        Zombie zombie = EntityType.ZOMBIE.spawn(
                helper.getLevel(), helper.absolutePos(BlockPos.ZERO), MobSpawnType.NATURAL);
        helper.assertTrue(zombie != null, "zombie spawn was cancelled");
        MobScalingState state = zombie.getData(ModAttachments.MOB_SCALING);
        helper.assertTrue(state.applied(), "mob scaling attachment was not applied");
        int expectedTier = MobScalingService.configuredTierForDistance(Math.max(
                Math.abs((long) zombie.chunkPosition().x - FrontierProtocolWorldData.get(helper.getLevel()).originChunkX()),
                Math.abs((long) zombie.chunkPosition().z - FrontierProtocolWorldData.get(helper.getLevel()).originChunkZ())));
        helper.assertTrue(state.distanceTier() == expectedTier, "mob received the wrong distance tier");
        var health = zombie.getAttribute(Attributes.MAX_HEALTH);
        var attack = zombie.getAttribute(Attributes.ATTACK_DAMAGE);
        var armor = zombie.getAttribute(Attributes.ARMOR);
        var speed = zombie.getAttribute(Attributes.MOVEMENT_SPEED);
        helper.assertTrue(health.hasModifier(MobScalingService.HEALTH_MODIFIER_ID), "health modifier was missing");
        helper.assertTrue(attack.hasModifier(MobScalingService.ATTACK_MODIFIER_ID), "attack modifier was missing");
        helper.assertTrue(armor.hasModifier(MobScalingService.ARMOR_MODIFIER_ID), "armor modifier was missing");
        helper.assertTrue(speed.hasModifier(MobScalingService.SPEED_MODIFIER_ID), "speed modifier was missing");
        double expectedHealthAmount = FrontierProtocolServerConfig.MOB_HEALTH_MULTIPLIERS.get()
                .get(expectedTier).doubleValue() - 1.0;
        helper.assertTrue(health.getModifier(MobScalingService.HEALTH_MODIFIER_ID).amount() == expectedHealthAmount,
                "health modifier had the wrong amount");
        float scaledHealth = zombie.getMaxHealth();
        helper.assertTrue(zombie.getHealth() == scaledHealth, "spawned mob was not healed to scaled max health");
        health.removeModifier(MobScalingService.HEALTH_MODIFIER_ID);
        MobScalingService.applyIfPrepared(zombie);
        helper.assertTrue(!health.hasModifier(MobScalingService.HEALTH_MODIFIER_ID),
                "applied guard recreated a deliberately removed modifier");
        helper.assertTrue(scaledHealth > zombie.getMaxHealth(), "scaled health was not greater before guard verification");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void commandZombieIsNotScaledByDefault(GameTestHelper helper) {
        Zombie zombie = EntityType.ZOMBIE.spawn(
                helper.getLevel(), helper.absolutePos(BlockPos.ZERO), MobSpawnType.COMMAND);
        helper.assertTrue(zombie != null, "zombie spawn was cancelled");
        helper.assertTrue(!zombie.hasData(ModAttachments.MOB_SCALING), "command-spawned mob received scaling");
        helper.succeed();
    }
}
