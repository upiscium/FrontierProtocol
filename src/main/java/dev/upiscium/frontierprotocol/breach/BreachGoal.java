package dev.upiscium.frontierprotocol.breach;

import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.protection.ServerProtectionService;
import dev.upiscium.frontierprotocol.registry.ModBlockTags;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;

public final class BreachGoal extends Goal {
    private final Mob mob;
    private int noPathTicks;
    private BlockPos breakingPos;
    private BlockState breakingState;
    private int elapsedBreakTicks;
    private int breakDurationTicks;
    private UUID observedTarget;

    public BreachGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        return mob.level() instanceof ServerLevel && mob.getTarget() instanceof Player player && player.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!(mob.level() instanceof ServerLevel level) || !(mob.getTarget() instanceof Player target)) {
            reset();
            return;
        }
        if (!target.getUUID().equals(observedTarget)) {
            if (breakingPos != null) clearBreaking(level);
            observedTarget = target.getUUID();
            noPathTicks = 0;
        }
        if (breakingPos != null) {
            tickBreaking(level);
            return;
        }
        var path = mob.getNavigation().getPath();
        if (path != null && !path.isDone()) {
            noPathTicks = 0;
            return;
        }
        if (++noPathTicks < FrontierProtocolServerConfig.BREACH_NO_PATH_TICKS.getAsInt()) return;
        noPathTicks = 0;
        selectCandidate(level, target);
    }

    @Override
    public void stop() {
        reset();
    }

    public BlockPos breakingPos() {
        return breakingPos;
    }

    public int breakProgress() {
        return breakingPos == null ? -1 : BreachRules.progressStage(elapsedBreakTicks, breakDurationTicks);
    }

    private void selectCandidate(ServerLevel level, Player target) {
        for (BlockPos candidate : obstructionCandidates(target)) {
            BlockState state = loadedState(level, candidate);
            if (state != null && canBreak(level, candidate, state)) {
                breakingPos = candidate;
                breakingState = state;
                elapsedBreakTicks = 0;
                breakDurationTicks = BreachRules.breakDurationTicks(
                        state.getDestroySpeed(level, candidate),
                        FrontierProtocolServerConfig.BREACH_TIME_MULTIPLIER.getAsDouble());
                mob.getNavigation().stop();
                return;
            }
        }
    }

    private void tickBreaking(ServerLevel level) {
        BlockState current = loadedState(level, breakingPos);
        if (current == null || current != breakingState || !canBreak(level, breakingPos, current)) {
            clearBreaking(level);
            return;
        }
        mob.getNavigation().stop();
        elapsedBreakTicks++;
        int progress = BreachRules.progressStage(elapsedBreakTicks, breakDurationTicks);
        level.destroyBlockProgress(mob.getId(), breakingPos, progress);
        if (elapsedBreakTicks >= breakDurationTicks) {
            BlockPos completed = breakingPos;
            clearBreaking(level);
            level.destroyBlock(completed, FrontierProtocolServerConfig.BREACH_DROPS.get(), mob);
        }
    }

    private boolean canBreak(ServerLevel level, BlockPos pos, BlockState state) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                || !state.is(ModBlockTags.MOB_BREAKABLE)
                || !BreachRules.hasNoBlockEntity(state)
                || !BreachRules.hardnessAllowed(state.getDestroySpeed(level, pos),
                        FrontierProtocolServerConfig.BREACH_MAX_HARDNESS.getAsDouble())
                || ServerProtectionService.INSTANCE.isBlockProtected(level, pos)) {
            return false;
        }
        double reach = FrontierProtocolServerConfig.BREACH_REACH.getAsDouble();
        if (mob.distanceToSqr(Vec3.atCenterOf(pos)) > reach * reach) return false;
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && chunk.getBlockEntity(pos) == null && CommonHooks.canEntityDestroy(level, pos, mob);
    }

    private List<BlockPos> obstructionCandidates(Player target) {
        BlockPos origin = mob.blockPosition();
        Direction forward = Direction.getNearest(
                target.getX() - (origin.getX() + 0.5), 0.0,
                target.getZ() - (origin.getZ() + 0.5));
        Direction sideways = forward.getClockWise();
        int height = Math.max(1, Mth.ceil(mob.getBbHeight()));
        int maximum = FrontierProtocolServerConfig.BREACH_MAX_CANDIDATES.getAsInt();
        List<BlockPos> result = new ArrayList<>(maximum);
        int[] lateralOrder = {0, -1, 1};
        for (int depth = 1; depth <= 2; depth++) {
            BlockPos center = origin.relative(forward, depth);
            for (int y = 0; y < height; y++) {
                for (int lateral : lateralOrder) {
                    if (result.size() >= maximum) return result;
                    result.add(center.relative(sideways, lateral).above(y));
                }
            }
        }
        return result;
    }

    private static BlockState loadedState(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockState(pos);
    }

    private void reset() {
        if (mob.level() instanceof ServerLevel level) clearBreaking(level);
        else {
            breakingPos = null;
            breakingState = null;
            elapsedBreakTicks = 0;
        }
        noPathTicks = 0;
        observedTarget = null;
    }

    private void clearBreaking(ServerLevel level) {
        if (breakingPos != null) level.destroyBlockProgress(mob.getId(), breakingPos, -1);
        breakingPos = null;
        breakingState = null;
        elapsedBreakTicks = 0;
        breakDurationTicks = 0;
    }
}
