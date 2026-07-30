package dev.upiscium.frontierprotocol.stabilizer;

public final class StabilizerComparatorSignal {
    private StabilizerComparatorSignal() {}

    public static int calculate(int count, int capacity) {
        if (count <= 0 || capacity <= 0) return 0;
        int boundedCount = Math.min(count, capacity);
        return Math.min(15, 1 + (int) (14L * boundedCount / capacity));
    }
}
