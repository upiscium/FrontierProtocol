package dev.upiscium.frontierprotocol.cleanup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CleanupBudgetTest {
    @Test
    void neverExceedsInspectionOrMutationLimits() {
        CleanupBudget budget = new CleanupBudget(2, 1);
        budget.consumeInspection();
        budget.consumeInspection();
        budget.consumeMutation();

        assertFalse(budget.canInspect());
        assertFalse(budget.canMutate());
        assertThrows(IllegalStateException.class, budget::consumeInspection);
        assertThrows(IllegalStateException.class, budget::consumeMutation);
    }

    @Test
    void globalAndPerCycleBudgetsRemainIndependent() {
        CleanupBudget global = new CleanupBudget(3, 2);
        CleanupBudget source = new CleanupBudget(1, 1);
        global.consumeInspection();
        source.consumeInspection();
        global.consumeMutation();
        source.consumeMutation();

        assertEquals(2, global.inspectionsRemaining());
        assertEquals(1, global.mutationsRemaining());
        assertFalse(source.canInspect());
        assertFalse(source.canMutate());
    }

    @Test
    void zeroMutationBudgetCanStillInspect() {
        CleanupBudget budget = new CleanupBudget(1, 0);
        assertTrue(budget.canInspect());
        assertFalse(budget.canMutate());
    }

    @Test
    void rejectsNegativeBudgets() {
        assertThrows(IllegalArgumentException.class, () -> new CleanupBudget(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new CleanupBudget(0, -1));
    }
}
