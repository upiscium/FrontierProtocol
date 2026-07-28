package dev.upiscium.frontierprotocol.stabilizer;

final class StabilizerStateMachine {
    private StabilizerStatus status;
    private int graceRemainingTicks;
    private int cellRemainingTicks;

    StabilizerStateMachine() {
        this(StabilizerStatus.OFFLINE, 0, 0);
    }

    StabilizerStateMachine(StabilizerStatus status, int graceRemainingTicks, int cellRemainingTicks) {
        this.status = status == null ? StabilizerStatus.OFFLINE : status;
        this.graceRemainingTicks = Math.max(0, graceRemainingTicks);
        this.cellRemainingTicks = Math.max(0, cellRemainingTicks);
    }

    TickResult tick(boolean powered, boolean cellAvailable, int graceDurationTicks, int cellDurationTicks) {
        StabilizerStatus previousStatus = status;
        int previousGrace = graceRemainingTicks;
        int previousCell = cellRemainingTicks;
        boolean consumeItem = false;

        if (powered) {
            if (cellRemainingTicks == 0 && cellAvailable) {
                cellRemainingTicks = Math.max(1, cellDurationTicks);
                consumeItem = true;
            }
            if (cellRemainingTicks > 0) {
                status = StabilizerStatus.ACTIVE;
                cellRemainingTicks--;
                graceRemainingTicks = Math.max(0, graceDurationTicks);
            } else {
                loseActiveConditions();
            }
        } else {
            loseActiveConditions();
        }

        boolean changed = status != previousStatus
                || graceRemainingTicks != previousGrace
                || cellRemainingTicks != previousCell;
        return new TickResult(changed, status != previousStatus, consumeItem);
    }

    private void loseActiveConditions() {
        if (status == StabilizerStatus.ACTIVE) {
            status = graceRemainingTicks > 0 ? StabilizerStatus.GRACE_PERIOD : StabilizerStatus.OFFLINE;
        }
        if (status == StabilizerStatus.GRACE_PERIOD) {
            if (graceRemainingTicks > 0) graceRemainingTicks--;
            if (graceRemainingTicks == 0) status = StabilizerStatus.OFFLINE;
        } else if (status == StabilizerStatus.OFFLINE) {
            graceRemainingTicks = 0;
        }
    }

    StabilizerStatus status() {
        return status;
    }

    int graceRemainingTicks() {
        return graceRemainingTicks;
    }

    int cellRemainingTicks() {
        return cellRemainingTicks;
    }

    record TickResult(boolean changed, boolean statusChanged, boolean consumeItem) {}
}
