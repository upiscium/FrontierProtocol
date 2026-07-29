package dev.upiscium.frontierprotocol.stabilizer;

public final class Tier1StabilizerAnimation {
    public static final int ACTIVE_COLOR = 0x39D47A;
    public static final int GRACE_COLOR = 0xF1C840;
    public static final int OFFLINE_COLOR = 0xA83B3B;

    private Tier1StabilizerAnimation() {}

    public static int stateColor(StabilizerStatus status) {
        return switch (status) {
            case ACTIVE -> ACTIVE_COLOR;
            case GRACE_PERIOD -> GRACE_COLOR;
            case OFFLINE -> OFFLINE_COLOR;
        };
    }

    public static double graceBlinkFrequency(int remaining, int duration) {
        if (duration <= 0 || remaining <= 0) return 0.0;
        double ratio = Math.min(1.0, (double) remaining / duration);
        if (ratio > 0.5) return 0.75;
        if (ratio > 0.2) return 1.5;
        return 3.0;
    }

    public static float graceLightAlpha(double timeSeconds, int remaining, int duration) {
        double frequency = graceBlinkFrequency(remaining, duration);
        if (frequency == 0.0) return 0.25F;
        return (float) (0.625 + 0.375 * Math.sin(timeSeconds * Math.PI * 2.0 * frequency));
    }

    public static float coreRotationDelta(float speed, boolean active) {
        if (!active || speed == 0.0F) return 0.0F;
        float magnitude = Math.clamp(Math.abs(speed) * 0.10F, 2.0F, 9.0F);
        return Math.copySign(magnitude, speed);
    }

    public static float corePulse(double timeSeconds) {
        return (float) (0.85 + 0.15 * Math.sin(timeSeconds * Math.PI * 2.0 * 0.8));
    }
}
