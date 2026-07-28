package dev.upiscium.frontierprotocol.stabilizer;

import net.minecraft.util.StringRepresentable;

public enum StabilizerStatus implements StringRepresentable {
    OFFLINE("offline"),
    GRACE_PERIOD("grace_period"),
    ACTIVE("active");

    private final String serializedName;

    StabilizerStatus(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    static StabilizerStatus fromSerializedName(String value) {
        for (StabilizerStatus status : values()) {
            if (status.serializedName.equals(value)) return status;
        }
        return OFFLINE;
    }

    public boolean suppressesInfection() {
        return this != OFFLINE;
    }
}
