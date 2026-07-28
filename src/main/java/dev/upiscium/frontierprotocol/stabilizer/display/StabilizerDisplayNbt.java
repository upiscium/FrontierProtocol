package dev.upiscium.frontierprotocol.stabilizer.display;

import dev.upiscium.frontierprotocol.stabilizer.StabilizerStatus;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerTier;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class StabilizerDisplayNbt {
    public static final int SCHEMA_VERSION = 1;
    public static final String DISPLAY_KEY = "frontierDisplay";
    private static final String SCHEMA_VERSION_KEY = "schemaVersion";
    private static final String TIER = "tier";
    private static final String STATUS = "status";
    private static final String MINIMUM_RPM = "minimumRpm";
    private static final String STRESS_IMPACT = "stressImpact";
    private static final String CELL_COUNT = "cellCount";
    private static final String CELL_CAPACITY = "cellCapacity";
    private static final String CELL_REMAINING_TICKS = "cellRemainingTicks";
    private static final String CELL_DURATION_TICKS = "cellDurationTicks";
    private static final String GRACE_REMAINING_TICKS = "graceRemainingTicks";
    private static final String CHUNK_RADIUS = "chunkRadius";

    private StabilizerDisplayNbt() {}

    public static void write(CompoundTag root, StabilizerDisplaySnapshot snapshot) {
        CompoundTag display = new CompoundTag();
        display.putInt(SCHEMA_VERSION_KEY, SCHEMA_VERSION);
        display.putString(TIER, snapshot.tier().serializedName());
        display.putString(STATUS, snapshot.status().getSerializedName());
        display.putInt(MINIMUM_RPM, snapshot.minimumRpm());
        display.putDouble(STRESS_IMPACT, snapshot.stressImpact());
        display.putInt(CELL_COUNT, snapshot.cellCount());
        display.putInt(CELL_CAPACITY, snapshot.cellCapacity());
        display.putInt(CELL_REMAINING_TICKS, snapshot.cellRemainingTicks());
        display.putInt(CELL_DURATION_TICKS, snapshot.cellDurationTicks());
        display.putInt(GRACE_REMAINING_TICKS, snapshot.graceRemainingTicks());
        display.putInt(CHUNK_RADIUS, snapshot.chunkRadius());
        root.put(DISPLAY_KEY, display);
    }

    public static Optional<StabilizerDisplaySnapshot> read(CompoundTag root) {
        if (!root.contains(DISPLAY_KEY, Tag.TAG_COMPOUND)) return Optional.empty();
        CompoundTag display = root.getCompound(DISPLAY_KEY);
        if (!hasRequiredFields(display) || display.getInt(SCHEMA_VERSION_KEY) != SCHEMA_VERSION) {
            return Optional.empty();
        }
        StabilizerTier tier = StabilizerTier.fromSerializedName(display.getString(TIER));
        StabilizerStatus status = parseStatus(display.getString(STATUS));
        if (tier == null || status == null) return Optional.empty();
        try {
            return Optional.of(new StabilizerDisplaySnapshot(
                    tier,
                    status,
                    display.getInt(MINIMUM_RPM),
                    display.getDouble(STRESS_IMPACT),
                    display.getInt(CELL_COUNT),
                    display.getInt(CELL_CAPACITY),
                    display.getInt(CELL_REMAINING_TICKS),
                    display.getInt(CELL_DURATION_TICKS),
                    display.getInt(GRACE_REMAINING_TICKS),
                    display.getInt(CHUNK_RADIUS)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public static StabilizerDisplaySnapshot readOrRetain(
            CompoundTag root, StabilizerTier expectedTier, StabilizerDisplaySnapshot previous) {
        return read(root).filter(snapshot -> snapshot.tier() == expectedTier).orElse(previous);
    }

    private static StabilizerStatus parseStatus(String value) {
        for (StabilizerStatus status : StabilizerStatus.values()) {
            if (status.getSerializedName().equals(value)) return status;
        }
        return null;
    }

    private static boolean hasRequiredFields(CompoundTag tag) {
        return tag.contains(SCHEMA_VERSION_KEY, Tag.TAG_INT)
                && tag.contains(TIER, Tag.TAG_STRING)
                && tag.contains(STATUS, Tag.TAG_STRING)
                && tag.contains(MINIMUM_RPM, Tag.TAG_INT)
                && tag.contains(STRESS_IMPACT, Tag.TAG_DOUBLE)
                && tag.contains(CELL_COUNT, Tag.TAG_INT)
                && tag.contains(CELL_CAPACITY, Tag.TAG_INT)
                && tag.contains(CELL_REMAINING_TICKS, Tag.TAG_INT)
                && tag.contains(CELL_DURATION_TICKS, Tag.TAG_INT)
                && tag.contains(GRACE_REMAINING_TICKS, Tag.TAG_INT)
                && tag.contains(CHUNK_RADIUS, Tag.TAG_INT);
    }
}
