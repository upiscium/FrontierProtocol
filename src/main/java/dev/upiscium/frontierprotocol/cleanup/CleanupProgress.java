package dev.upiscium.frontierprotocol.cleanup;

public record CleanupProgress(
        CleanupCursor cursor, boolean restartRequired, int minSection, int sectionCount) {
    public CleanupProgress {
        if (sectionCount <= 0) throw new IllegalArgumentException("Cleanup section count must be positive");
        if (cursor.sectionIndex() >= sectionCount) {
            throw new IllegalArgumentException("Cleanup cursor section is outside the saved dimension height");
        }
    }

    public static CleanupProgress start(int minSection, int sectionCount, boolean restartRequired) {
        return new CleanupProgress(CleanupCursor.start(), restartRequired, minSection, sectionCount);
    }

    public CleanupProgress withCursor(CleanupCursor nextCursor) {
        return new CleanupProgress(nextCursor, false, minSection, sectionCount);
    }

    public CleanupProgress withRestartRequired(boolean required) {
        return new CleanupProgress(cursor, required, minSection, sectionCount);
    }

    public CleanupProgress activate(CleanupActivationMode mode) {
        return mode == CleanupActivationMode.NEW_PASS || restartRequired
                ? start(minSection, sectionCount, false)
                : withRestartRequired(false);
    }
}
