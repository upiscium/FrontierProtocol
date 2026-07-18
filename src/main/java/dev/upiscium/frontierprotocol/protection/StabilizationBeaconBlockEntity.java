package dev.upiscium.frontierprotocol.protection;

import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.data.FuelDefinition;
import dev.upiscium.frontierprotocol.registry.ModBlockEntities;
import dev.upiscium.frontierprotocol.registry.ModDataMaps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class StabilizationBeaconBlockEntity extends BlockEntity {
    private static final String INVENTORY_TAG = "Inventory";
    private static final String FUEL_TICKS_TAG = "FuelTicks";
    private static final String GRACE_TICKS_TAG = "GraceTicksRemaining";

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItemHolder().getData(ModDataMaps.STABILIZATION_FUELS) != null;
        }

        @Override
        protected void onContentsChanged(int slot) {
            StabilizationBeaconBlockEntity.this.setChanged();
        }
    };
    private BeaconFuelState fuelState = new BeaconFuelState();

    public StabilizationBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STABILIZATION_BEACON.get(), pos, state);
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public int fuelTicks() {
        return fuelState.fuelTicks();
    }

    public int graceTicksRemaining() {
        return fuelState.graceTicksRemaining();
    }

    public BeaconStatus status() {
        return fuelState.status(isEnabled());
    }

    public boolean isProtecting() {
        return status() != BeaconStatus.OFFLINE;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, StabilizationBeaconBlockEntity beacon) {
        if (!(level instanceof ServerLevel)) return;
        boolean enabled = state.getValue(StabilizationBeaconBlock.ENABLED);
        boolean changed = enabled && beacon.tryLoadFuel();
        changed |= beacon.fuelState.tick(enabled, FrontierProtocolServerConfig.BEACON_GRACE_TICKS.getAsInt());
        if (changed) beacon.setChanged();
    }

    private boolean tryLoadFuel() {
        if (fuelState.fuelTicks() > 0) return false;
        ItemStack stack = inventory.getStackInSlot(0);
        if (stack.isEmpty()) return false;
        FuelDefinition fuel = stack.getItemHolder().getData(ModDataMaps.STABILIZATION_FUELS);
        if (fuel == null) return false;
        inventory.extractItem(0, 1, false);
        fuelState.addFuel(fuel.ticks());
        return true;
    }

    private boolean isEnabled() {
        return getBlockState().getValue(StabilizationBeaconBlock.ENABLED);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) ProtectionIndex.get(serverLevel).register(this);
    }

    @Override
    public void onChunkUnloaded() {
        if (level instanceof ServerLevel serverLevel) ProtectionIndex.get(serverLevel).unregister(this);
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) ProtectionIndex.get(serverLevel).unregister(this);
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(INVENTORY_TAG, inventory.serializeNBT(registries));
        tag.putInt(FUEL_TICKS_TAG, fuelState.fuelTicks());
        tag.putInt(GRACE_TICKS_TAG, fuelState.graceTicksRemaining());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(INVENTORY_TAG, Tag.TAG_COMPOUND)) {
            inventory.deserializeNBT(registries, tag.getCompound(INVENTORY_TAG));
        }
        fuelState = new BeaconFuelState(tag.getInt(FUEL_TICKS_TAG), tag.getInt(GRACE_TICKS_TAG));
    }
}
