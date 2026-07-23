package dev.upiscium.frontierprotocol.cleanup;

public final class CleanupBudget {
    private int inspectionsRemaining;
    private int mutationsRemaining;

    public CleanupBudget(int inspections, int mutations) {
        reset(inspections, mutations);
    }

    public int inspectionsRemaining() {
        return inspectionsRemaining;
    }

    public int mutationsRemaining() {
        return mutationsRemaining;
    }

    public boolean canInspect() {
        return inspectionsRemaining > 0;
    }

    public boolean canMutate() {
        return mutationsRemaining > 0;
    }

    public void consumeInspection() {
        if (!canInspect()) throw new IllegalStateException("Cleanup inspection budget exhausted");
        inspectionsRemaining--;
    }

    public void consumeMutation() {
        if (!canMutate()) throw new IllegalStateException("Cleanup mutation budget exhausted");
        mutationsRemaining--;
    }

    public void reset(int inspections, int mutations) {
        if (inspections < 0 || mutations < 0) {
            throw new IllegalArgumentException("Cleanup budgets cannot be negative");
        }
        inspectionsRemaining = inspections;
        mutationsRemaining = mutations;
    }
}
