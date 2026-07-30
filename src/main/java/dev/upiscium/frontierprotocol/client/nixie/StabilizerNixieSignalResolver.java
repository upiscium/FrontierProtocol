package dev.upiscium.frontierprotocol.client.nixie;

import com.simibubi.create.content.trains.signal.SignalBlockEntity.SignalState;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerBlock;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerStatus;
import java.util.Optional;
import net.minecraft.world.level.block.state.BlockState;

public final class StabilizerNixieSignalResolver {
    private StabilizerNixieSignalResolver() {}

    public static Optional<SignalState> resolve(BlockState connectedState) {
        if (!(connectedState.getBlock() instanceof StabilizerBlock)) return Optional.empty();
        StabilizerStatus status = connectedState.getValue(StabilizerBlock.STATUS);
        return Optional.of(status.suppressesInfection() ? SignalState.GREEN : SignalState.RED);
    }
}
