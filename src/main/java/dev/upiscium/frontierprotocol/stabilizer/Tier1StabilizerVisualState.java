package dev.upiscium.frontierprotocol.stabilizer;

import dev.upiscium.frontierprotocol.stabilizer.display.StabilizerDisplaySnapshot;
import net.minecraft.world.level.block.state.BlockState;

public final class Tier1StabilizerVisualState {
    private Tier1StabilizerVisualState() {}

    public static StabilizerStatus resolveStatus(StabilizerDisplaySnapshot snapshot, BlockState state) {
        if (snapshot != null) return snapshot.status();
        if (state.hasProperty(StabilizerBlock.STATUS)) return state.getValue(StabilizerBlock.STATUS);
        return StabilizerStatus.OFFLINE;
    }

    public static int resolveMinimumRpm(StabilizerDisplaySnapshot snapshot, int configuredMinimumRpm) {
        return snapshot == null ? configuredMinimumRpm : snapshot.minimumRpm();
    }

    public static float resolveGraceLightAlpha(StabilizerDisplaySnapshot snapshot, double timeSeconds) {
        return snapshot == null
                ? Tier1StabilizerAnimation.graceLightAlpha(timeSeconds, 3, 4)
                : Tier1StabilizerAnimation.graceLightAlpha(
                        timeSeconds, snapshot.graceRemainingTicks(), snapshot.graceDurationTicks());
    }
}
