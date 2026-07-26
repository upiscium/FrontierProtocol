package dev.upiscium.frontierprotocol.stabilizer;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.items.ItemStackHandler;

final class StabilizerNbt {
    static final int SCHEMA_VERSION = 1;
    private static final String SCHEMA_VERSION_KEY = "schemaVersion";
    private static final String TIER = "tier";
    private static final String STATUS = "status";
    private static final String GRACE_REMAINING_TICKS = "graceRemainingTicks";
    private static final String CELL_REMAINING_TICKS = "cellRemainingTicks";
    private static final String REGISTERED_CHUNK_RADIUS = "registeredChunkRadius";
    private static final String INVENTORY = "inventory";

    private StabilizerNbt() {}

    static void write(
            CompoundTag tag,
            StabilizerTier tier,
            StabilizerStateMachine state,
            int registeredChunkRadius,
            ItemStackHandler inventory,
            HolderLookup.Provider registries) {
        tag.putInt(SCHEMA_VERSION_KEY, SCHEMA_VERSION);
        tag.putString(TIER, tier.serializedName());
        tag.putString(STATUS, state.status().getSerializedName());
        tag.putInt(GRACE_REMAINING_TICKS, state.graceRemainingTicks());
        tag.putInt(CELL_REMAINING_TICKS, state.cellRemainingTicks());
        tag.putInt(REGISTERED_CHUNK_RADIUS, registeredChunkRadius);
        tag.put(INVENTORY, inventory.serializeNBT(registries));
    }

    static ReadResult read(
            CompoundTag tag,
            StabilizerTier expectedTier,
            ItemStackHandler inventory,
            HolderLookup.Provider registries) {
        StabilizerTier storedTier = StabilizerTier.fromSerializedName(tag.getString(TIER));
        if (storedTier != expectedTier) {
            FrontierProtocolMod.LOGGER.warn(
                    "Stabilizer NBT tier {} does not match BlockState tier {}; using BlockState tier",
                    tag.getString(TIER),
                    expectedTier.serializedName());
        }
        if (tag.contains(INVENTORY, Tag.TAG_COMPOUND)) {
            inventory.deserializeNBT(registries, tag.getCompound(INVENTORY));
        }
        StabilizerStateMachine machine = new StabilizerStateMachine(
                StabilizerStatus.fromSerializedName(tag.getString(STATUS)),
                Math.max(0, tag.getInt(GRACE_REMAINING_TICKS)),
                Math.max(0, tag.getInt(CELL_REMAINING_TICKS)));
        Integer registeredChunkRadius = null;
        if (tag.contains(REGISTERED_CHUNK_RADIUS, Tag.TAG_INT)) {
            int storedRadius = tag.getInt(REGISTERED_CHUNK_RADIUS);
            if (storedRadius >= 0) {
                registeredChunkRadius = storedRadius;
            } else {
                FrontierProtocolMod.LOGGER.warn(
                        "Ignoring negative Stabilizer registered chunk radius {}", storedRadius);
            }
        }
        return new ReadResult(machine, registeredChunkRadius);
    }

    record ReadResult(StabilizerStateMachine machine, Integer registeredChunkRadius) {}
}
