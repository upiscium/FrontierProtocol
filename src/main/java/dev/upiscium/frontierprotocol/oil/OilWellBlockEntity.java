package dev.upiscium.frontierprotocol.oil;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.data.OilWellReloadListener;
import dev.upiscium.frontierprotocol.protection.ServerProtectionService;
import dev.upiscium.frontierprotocol.registry.ModBlockEntities;
import dev.upiscium.frontierprotocol.resource.ResourceNodeWorkService;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public final class OilWellBlockEntity extends BlockEntity implements MenuProvider {
    public static final ResourceLocation DEFINITION_ID = ResourceLocation.fromNamespaceAndPath(
            FrontierProtocolMod.MOD_ID, "oil_well");
    private static final String TANK_TAG = "Tank";
    private static final String PROGRESS_TAG = "Progress";

    private final FluidTank tank = new FluidTank(8000) {
        @Override
        protected void onContentsChanged() {
            OilWellBlockEntity.this.setChanged();
        }
    };
    private final IFluidHandler output = new IFluidHandler() {
        @Override public int getTanks() { return tank.getTanks(); }
        @Override public FluidStack getFluidInTank(int index) { return tank.getFluidInTank(index).copy(); }
        @Override public int getTankCapacity(int index) { return tank.getTankCapacity(index); }
        @Override public boolean isFluidValid(int index, FluidStack stack) { return false; }
        @Override public int fill(FluidStack stack, FluidAction action) { return 0; }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) { return tank.drain(stack, action); }
        @Override public FluidStack drain(int amount, FluidAction action) { return tank.drain(amount, action); }
    };
    private int progress;
    private int workInterval = 1;
    private OilWellStatus status = OilWellStatus.MISSING_DEFINITION;

    public OilWellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OIL_WELL.get(), pos, state);
    }

    public IFluidHandler output() {
        return output;
    }

    public FluidTank tank() {
        return tank;
    }

    public int progress() {
        return progress;
    }

    public int workInterval() {
        return workInterval;
    }

    public OilWellStatus status() {
        return status;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OilWellBlockEntity well) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        OilWellDefinition definition = OilWellReloadListener.find(DEFINITION_ID).orElse(null);
        if (definition == null) {
            well.setStatus(OilWellStatus.MISSING_DEFINITION);
            return;
        }
        well.workInterval = definition.workInterval();
        well.tank.setCapacity(definition.capacity());
        if (!ModList.get().isLoaded(definition.requiredMod())) {
            well.setStatus(OilWellStatus.MISSING_MOD);
            return;
        }
        Fluid fluid = BuiltInRegistries.FLUID.getOptional(definition.output().fluid()).orElse(Fluids.EMPTY);
        if (fluid == Fluids.EMPTY) {
            well.setStatus(OilWellStatus.MISSING_FLUID);
            return;
        }
        if (ResourceNodeWorkService.currentTrait(serverLevel, pos).filter(definition.requiredTrait()::equals).isEmpty()) {
            well.setStatus(OilWellStatus.WRONG_TRAIT);
            return;
        }
        if (definition.requiresProtection() && !ServerProtectionService.INSTANCE.isBlockProtected(serverLevel, pos)) {
            well.setStatus(OilWellStatus.PROTECTION_REQUIRED);
            return;
        }
        FluidStack result = new FluidStack(fluid, definition.output().amount());
        if (well.tank.fill(result, IFluidHandler.FluidAction.SIMULATE) != result.getAmount()) {
            well.setStatus(OilWellStatus.OUTPUT_FULL);
            return;
        }
        well.setStatus(OilWellStatus.WORKING);
        well.progress++;
        if (well.progress >= definition.workInterval()) {
            well.progress = 0;
            well.tank.fill(result, IFluidHandler.FluidAction.EXECUTE);
        }
        well.setChanged();
    }

    private void setStatus(OilWellStatus value) {
        if (status != value) {
            status = value;
            setChanged();
        }
    }

    public void writeMenuData(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        OilWellDefinition definition = OilWellReloadListener.find(DEFINITION_ID).orElse(null);
        buffer.writeUtf(definition == null ? "none" : definition.requiredTrait().toString());
        buffer.writeUtf(definition == null ? "none" : definition.requiredMod());
        buffer.writeUtf(definition == null ? "none" : definition.output().fluid().toString());
    }

    void setProgressFromMenu(int value) {
        progress = value;
    }

    void setStatusFromMenu(int value) {
        status = OilWellStatus.values()[Math.max(0, Math.min(OilWellStatus.values().length - 1, value))];
    }

    void setWorkIntervalFromMenu(int value) {
        workInterval = Math.max(1, value);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.frontier_protocol.oil_well");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new OilWellMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(TANK_TAG, tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt(PROGRESS_TAG, progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TANK_TAG, Tag.TAG_COMPOUND)) tank.readFromNBT(registries, tag.getCompound(TANK_TAG));
        progress = Math.max(0, tag.getInt(PROGRESS_TAG));
    }
}
