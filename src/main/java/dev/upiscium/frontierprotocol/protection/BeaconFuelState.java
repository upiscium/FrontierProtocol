package dev.upiscium.frontierprotocol.protection;

public final class BeaconFuelState {
    private int fuelTicks;
    private int graceTicksRemaining;

    public BeaconFuelState() {}

    public BeaconFuelState(int fuelTicks, int graceTicksRemaining) {
        this.fuelTicks = Math.max(0, fuelTicks);
        this.graceTicksRemaining = Math.max(0, graceTicksRemaining);
    }

    public int fuelTicks() {
        return fuelTicks;
    }

    public int graceTicksRemaining() {
        return graceTicksRemaining;
    }

    public BeaconStatus status(boolean enabled) {
        if (!enabled) return BeaconStatus.OFFLINE;
        if (fuelTicks > 0) return BeaconStatus.ACTIVE;
        return graceTicksRemaining > 0 ? BeaconStatus.GRACE : BeaconStatus.OFFLINE;
    }

    public void addFuel(int ticks) {
        if (ticks <= 0) throw new IllegalArgumentException("fuel ticks must be positive");
        fuelTicks = (int) Math.min((long) fuelTicks + ticks, Integer.MAX_VALUE);
        graceTicksRemaining = 0;
    }

    public boolean tick(boolean enabled, int graceDuration) {
        if (!enabled) return false;
        if (fuelTicks > 0) {
            fuelTicks--;
            if (fuelTicks == 0) graceTicksRemaining = Math.max(0, graceDuration);
            return true;
        }
        if (graceTicksRemaining > 0) {
            graceTicksRemaining--;
            return true;
        }
        return false;
    }
}
