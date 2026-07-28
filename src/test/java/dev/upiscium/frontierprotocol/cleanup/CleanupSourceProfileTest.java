package dev.upiscium.frontierprotocol.cleanup;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CleanupSourceProfileTest {
    @Test
    void acceptsPositiveValues() {
        assertDoesNotThrow(() -> new CleanupSourceProfile(1, 1, 1));
    }

    @Test
    void rejectsNonPositiveInterval() {
        assertThrows(IllegalArgumentException.class, () -> new CleanupSourceProfile(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new CleanupSourceProfile(-1, 1, 1));
    }

    @Test
    void rejectsNonPositiveInspectionBudget() {
        assertThrows(IllegalArgumentException.class, () -> new CleanupSourceProfile(1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new CleanupSourceProfile(1, -1, 1));
    }

    @Test
    void rejectsNonPositiveMutationBudget() {
        assertThrows(IllegalArgumentException.class, () -> new CleanupSourceProfile(1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new CleanupSourceProfile(1, 1, -1));
    }
}
