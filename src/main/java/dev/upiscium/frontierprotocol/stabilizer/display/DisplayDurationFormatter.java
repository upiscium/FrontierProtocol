package dev.upiscium.frontierprotocol.stabilizer.display;

import java.util.Locale;

public final class DisplayDurationFormatter {
    private DisplayDurationFormatter() {}

    public static String formatTicks(int ticks) {
        if (ticks < 0) throw new IllegalArgumentException("ticks must not be negative");
        long seconds = (ticks + 19L) / 20L;
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remainingSeconds = seconds % 60L;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, remainingSeconds);
        }
        return String.format(Locale.ROOT, "%d:%02d", minutes, remainingSeconds);
    }
}
