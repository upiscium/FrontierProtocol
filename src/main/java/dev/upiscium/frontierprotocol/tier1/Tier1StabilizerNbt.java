package dev.upiscium.frontierprotocol.tier1;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.items.ItemStackHandler;

final class Tier1StabilizerNbt {
    private static final String STATUS = "Status";
    private static final String GRACE = "GraceRemainingTicks";
    private static final String CONSUMABLE = "ConsumableRemainingTicks";
    private static final String INVENTORY = "Inventory";

    private Tier1StabilizerNbt() {}

    static void write(
            CompoundTag tag,
            Tier1StabilizerStateMachine state,
            ItemStackHandler inventory,
            HolderLookup.Provider registries) {
        tag.putString(STATUS, state.status().getSerializedName());
        tag.putInt(GRACE, state.graceRemainingTicks());
        tag.putInt(CONSUMABLE, state.consumableRemainingTicks());
        tag.put(INVENTORY, inventory.serializeNBT(registries));
    }

    static Tier1StabilizerStateMachine read(
            CompoundTag tag, ItemStackHandler inventory, HolderLookup.Provider registries) {
        if (tag.contains(INVENTORY, Tag.TAG_COMPOUND)) {
            inventory.deserializeNBT(registries, tag.getCompound(INVENTORY));
        }
        return new Tier1StabilizerStateMachine(
                Tier1StabilizerStatus.fromSerializedName(tag.getString(STATUS)),
                Math.max(0, tag.getInt(GRACE)),
                Math.max(0, tag.getInt(CONSUMABLE)));
    }
}
