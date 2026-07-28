package dev.upiscium.frontierprotocol.stabilizer.display;

import dev.upiscium.frontierprotocol.stabilizer.StabilizerStatus;

public final class StabilizerDisplaySyncPolicy {
    public static final int COUNTDOWN_INTERVAL_TICKS = 20;
    private StabilizerDisplaySnapshot lastSent;
    private long lastSentTick = Long.MIN_VALUE;
    private boolean dirty = true;

    public void markDirty() {
        dirty = true;
    }

    public boolean shouldSync(long gameTick, StabilizerDisplaySnapshot snapshot) {
        if (snapshot == null || snapshot.equals(lastSent)) return false;
        if (dirty || lastSent == null) return true;
        StabilizerStatus status = snapshot.status();
        return (status == StabilizerStatus.ACTIVE || status == StabilizerStatus.GRACE_PERIOD)
                && gameTick - lastSentTick >= COUNTDOWN_INTERVAL_TICKS;
    }

    public void recordSync(long gameTick, StabilizerDisplaySnapshot snapshot) {
        lastSent = snapshot;
        lastSentTick = gameTick;
        dirty = false;
    }

    public StabilizerDisplaySnapshot lastSent() {
        return lastSent;
    }
}
