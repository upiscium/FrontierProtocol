package dev.upiscium.frontierprotocol.cleanup;

public record CleanupSourceProfile(int intervalTicks, int inspectionBudget, int mutationBudget) {
    public CleanupSourceProfile {
        if (intervalTicks < 1) throw new IllegalArgumentException("intervalTicks must be at least 1");
        if (inspectionBudget < 1) throw new IllegalArgumentException("inspectionBudget must be at least 1");
        if (mutationBudget < 1) throw new IllegalArgumentException("mutationBudget must be at least 1");
    }
}
