package dev.upiscium.frontierprotocol.breach;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.protection.ServerProtectionService;
import dev.upiscium.frontierprotocol.registry.ModEntityTypeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

@EventBusSubscriber(modid = FrontierProtocolMod.MOD_ID)
public final class BreachEventHandlers {
    private BreachEventHandlers() {}

    @SubscribeEvent
    public static void addBreachGoal(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel) || !(event.getEntity() instanceof Mob mob)
                || !mob.getType().is(ModEntityTypeTags.BREACHER_MOBS)) return;
        boolean installed = mob.goalSelector.getAvailableGoals().stream()
                .anyMatch(goal -> goal.getGoal() instanceof BreachGoal);
        if (!installed) mob.goalSelector.addGoal(2, new BreachGoal(mob));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void protectFromHostileExplosions(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var source = event.getExplosion().getIndirectSourceEntity();
        if (!(source instanceof Mob) || !(source instanceof Enemy)) return;
        event.getAffectedBlocks().removeIf(pos ->
                ServerProtectionService.INSTANCE.isChunkProtected(level, new ChunkPos(pos)));
    }
}
