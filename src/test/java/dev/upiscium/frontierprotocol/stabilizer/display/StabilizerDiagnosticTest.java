package dev.upiscium.frontierprotocol.stabilizer.display;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.upiscium.frontierprotocol.stabilizer.StabilizerStatus;
import org.junit.jupiter.api.Test;

class StabilizerDiagnosticTest {
    @Test
    void evaluatesReasonsInPriorityOrder() {
        assertEquals(StabilizerDiagnostic.OVERSTRESSED, evaluate(true, 0, 0, 0, StabilizerStatus.GRACE_PERIOD));
        assertEquals(StabilizerDiagnostic.NO_ROTATION, evaluate(false, 0, 0, 0, StabilizerStatus.GRACE_PERIOD));
        assertEquals(StabilizerDiagnostic.INSUFFICIENT_RPM, evaluate(false, 31, 1, 1, StabilizerStatus.GRACE_PERIOD));
        assertEquals(StabilizerDiagnostic.NO_CELL, evaluate(false, 32, 0, 0, StabilizerStatus.GRACE_PERIOD));
        assertEquals(StabilizerDiagnostic.GRACE, evaluate(false, 32, 0, 1, StabilizerStatus.GRACE_PERIOD));
        assertEquals(StabilizerDiagnostic.OPERATIONAL, evaluate(false, 32, 0, 1, StabilizerStatus.ACTIVE));
    }

    private static StabilizerDiagnostic evaluate(
            boolean overStressed, float rpm, int cells, int remainingTicks, StabilizerStatus status) {
        return StabilizerDiagnostic.evaluate(overStressed, rpm, 32, cells, remainingTicks, status);
    }
}
