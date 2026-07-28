package dev.upiscium.frontierprotocol.stabilizer.display;

import dev.upiscium.frontierprotocol.stabilizer.StabilizerStatus;

public enum StabilizerDiagnostic {
    OVERSTRESSED("frontier_protocol.diagnostic.overstressed"),
    NO_ROTATION("frontier_protocol.diagnostic.no_rotation"),
    INSUFFICIENT_RPM("frontier_protocol.diagnostic.insufficient_rpm"),
    NO_CELL("frontier_protocol.diagnostic.no_cell"),
    GRACE("frontier_protocol.diagnostic.grace"),
    OPERATIONAL("frontier_protocol.diagnostic.operational");

    private final String translationKey;

    StabilizerDiagnostic(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public static StabilizerDiagnostic evaluate(
            boolean overStressed,
            float theoreticalRpm,
            int minimumRpm,
            int cellCount,
            int cellRemainingTicks,
            StabilizerStatus status) {
        if (overStressed) return OVERSTRESSED;
        if (status == StabilizerStatus.GRACE_PERIOD) return GRACE;
        float absoluteRpm = Math.abs(theoreticalRpm);
        if (absoluteRpm == 0.0F) return NO_ROTATION;
        if (absoluteRpm < minimumRpm) return INSUFFICIENT_RPM;
        if (cellCount == 0 && cellRemainingTicks == 0) return NO_CELL;
        return OPERATIONAL;
    }
}
