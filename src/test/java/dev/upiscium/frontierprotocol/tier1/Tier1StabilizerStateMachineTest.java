package dev.upiscium.frontierprotocol.tier1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Tier1StabilizerStateMachineTest {
    @Test
    void consumesOneItemAndBecomesActiveWhenPowered() {
        Tier1StabilizerStateMachine machine = new Tier1StabilizerStateMachine();

        Tier1StabilizerStateMachine.TickResult result = machine.tick(true, true, 4, 10);

        assertTrue(result.consumeItem());
        assertEquals(Tier1StabilizerStatus.ACTIVE, machine.status());
        assertEquals(9, machine.consumableRemainingTicks());
        assertEquals(4, machine.graceRemainingTicks());
    }

    @Test
    void activeTransitionsThroughGraceToOffline() {
        Tier1StabilizerStateMachine machine =
                new Tier1StabilizerStateMachine(Tier1StabilizerStatus.ACTIVE, 3, 0);

        machine.tick(false, false, 3, 10);
        assertEquals(Tier1StabilizerStatus.GRACE_PERIOD, machine.status());
        assertEquals(2, machine.graceRemainingTicks());

        machine.tick(false, false, 3, 10);
        machine.tick(false, false, 3, 10);
        assertEquals(Tier1StabilizerStatus.OFFLINE, machine.status());
        assertEquals(0, machine.graceRemainingTicks());
    }

    @Test
    void graceRecoversToActiveWithoutResettingConsumable() {
        Tier1StabilizerStateMachine machine =
                new Tier1StabilizerStateMachine(Tier1StabilizerStatus.GRACE_PERIOD, 12, 5);

        Tier1StabilizerStateMachine.TickResult result = machine.tick(true, false, 20, 10);

        assertFalse(result.consumeItem());
        assertEquals(Tier1StabilizerStatus.ACTIVE, machine.status());
        assertEquals(4, machine.consumableRemainingTicks());
        assertEquals(20, machine.graceRemainingTicks());
    }

    @Test
    void noConsumableKeepsOfflineMachineOffline() {
        Tier1StabilizerStateMachine machine = new Tier1StabilizerStateMachine();

        machine.tick(true, false, 20, 10);

        assertEquals(Tier1StabilizerStatus.OFFLINE, machine.status());
        assertEquals(0, machine.consumableRemainingTicks());
    }

    @Test
    void consumesExactlyOneNewItemOnTickAfterRuntimeExpires() {
        Tier1StabilizerStateMachine machine =
                new Tier1StabilizerStateMachine(Tier1StabilizerStatus.ACTIVE, 4, 1);

        Tier1StabilizerStateMachine.TickResult finalRuntimeTick = machine.tick(true, true, 4, 10);
        Tier1StabilizerStateMachine.TickResult refillTick = machine.tick(true, true, 4, 10);

        assertFalse(finalRuntimeTick.consumeItem());
        assertTrue(refillTick.consumeItem());
        assertEquals(Tier1StabilizerStatus.ACTIVE, machine.status());
        assertEquals(9, machine.consumableRemainingTicks());
    }
}
