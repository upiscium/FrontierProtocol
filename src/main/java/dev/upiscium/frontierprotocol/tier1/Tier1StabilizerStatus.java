package dev.upiscium.frontierprotocol.tier1;

import net.minecraft.util.StringRepresentable;

public enum Tier1StabilizerStatus implements StringRepresentable {
    OFFLINE("offline"),
    GRACE_PERIOD("grace_period"),
    ACTIVE("active");

    private final String serializedName;

    Tier1StabilizerStatus(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    static Tier1StabilizerStatus fromSerializedName(String value) {
        for (Tier1StabilizerStatus status : values()) {
            if (status.serializedName.equals(value)) return status;
        }
        return OFFLINE;
    }

    public boolean suppressesInfection() {
        return this != OFFLINE;
    }
}
