package dev.upiscium.frontierprotocol.tier1;

final class Tier1StabilizerStateMachine {
    private Tier1StabilizerStatus status;
    private int graceRemainingTicks;
    private int consumableRemainingTicks;

    Tier1StabilizerStateMachine() {
        this(Tier1StabilizerStatus.OFFLINE, 0, 0);
    }

    Tier1StabilizerStateMachine(
            Tier1StabilizerStatus status, int graceRemainingTicks, int consumableRemainingTicks) {
        this.status = status == null ? Tier1StabilizerStatus.OFFLINE : status;
        this.graceRemainingTicks = Math.max(0, graceRemainingTicks);
        this.consumableRemainingTicks = Math.max(0, consumableRemainingTicks);
    }

    TickResult tick(boolean powered, boolean consumableAvailable, int graceDuration, int consumableDuration) {
        Tier1StabilizerStatus previousStatus = status;
        int previousGrace = graceRemainingTicks;
        int previousConsumable = consumableRemainingTicks;
        boolean consumeItem = false;

        if (powered) {
            if (consumableRemainingTicks == 0 && consumableAvailable) {
                consumableRemainingTicks = Math.max(1, consumableDuration);
                consumeItem = true;
            }
            if (consumableRemainingTicks > 0) {
                status = Tier1StabilizerStatus.ACTIVE;
                consumableRemainingTicks--;
                graceRemainingTicks = Math.max(0, graceDuration);
            } else {
                loseActiveConditions();
            }
        } else {
            loseActiveConditions();
        }

        boolean changed = status != previousStatus
                || graceRemainingTicks != previousGrace
                || consumableRemainingTicks != previousConsumable;
        return new TickResult(changed, status != previousStatus, consumeItem);
    }

    private void loseActiveConditions() {
        if (status == Tier1StabilizerStatus.ACTIVE) {
            status = graceRemainingTicks > 0
                    ? Tier1StabilizerStatus.GRACE_PERIOD
                    : Tier1StabilizerStatus.OFFLINE;
        }
        if (status == Tier1StabilizerStatus.GRACE_PERIOD) {
            if (graceRemainingTicks > 0) graceRemainingTicks--;
            if (graceRemainingTicks == 0) status = Tier1StabilizerStatus.OFFLINE;
        } else if (status == Tier1StabilizerStatus.OFFLINE) {
            graceRemainingTicks = 0;
        }
    }

    Tier1StabilizerStatus status() {
        return status;
    }

    int graceRemainingTicks() {
        return graceRemainingTicks;
    }

    int consumableRemainingTicks() {
        return consumableRemainingTicks;
    }

    record TickResult(boolean changed, boolean statusChanged, boolean consumeItem) {}
}
