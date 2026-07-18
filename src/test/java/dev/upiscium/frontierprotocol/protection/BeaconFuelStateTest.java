package dev.upiscium.frontierprotocol.protection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BeaconFuelStateTest {
    @Test
    void fuelTransitionsThroughActiveGraceAndOffline() {
        BeaconFuelState state = new BeaconFuelState();
        assertEquals(BeaconStatus.OFFLINE, state.status(true));

        state.addFuel(2);
        assertEquals(BeaconStatus.ACTIVE, state.status(true));
        state.tick(true, 3);
        assertEquals(1, state.fuelTicks());
        state.tick(true, 3);
        assertEquals(BeaconStatus.GRACE, state.status(true));
        assertEquals(3, state.graceTicksRemaining());

        state.tick(true, 3);
        state.tick(true, 3);
        state.tick(true, 3);
        assertEquals(BeaconStatus.OFFLINE, state.status(true));
    }

    @Test
    void redstoneDisablePausesFuelAndRemovesProtection() {
        BeaconFuelState state = new BeaconFuelState();
        state.addFuel(20);

        assertFalse(state.tick(false, 100));
        assertEquals(20, state.fuelTicks());
        assertEquals(BeaconStatus.OFFLINE, state.status(false));

        assertTrue(state.tick(true, 100));
        assertEquals(19, state.fuelTicks());
    }

    @Test
    void refuelingDuringGraceRestoresActiveState() {
        BeaconFuelState state = new BeaconFuelState(0, 10);
        state.addFuel(40);

        assertEquals(40, state.fuelTicks());
        assertEquals(0, state.graceTicksRemaining());
        assertEquals(BeaconStatus.ACTIVE, state.status(true));
    }
}
