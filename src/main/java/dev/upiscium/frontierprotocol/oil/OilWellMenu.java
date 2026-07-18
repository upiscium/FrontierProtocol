package dev.upiscium.frontierprotocol.oil;

import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

public final class OilWellMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final String requiredTrait;
    private final String requiredMod;
    private final String outputFluid;
    private final DataSlot progress;
    private final DataSlot status;
    private final DataSlot workInterval;
    private final DataSlot fluidAmount;
    private final DataSlot capacity;

    public OilWellMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(id, inventory, findWell(inventory, data.readBlockPos()), data.readUtf(), data.readUtf(), data.readUtf());
    }

    public OilWellMenu(int id, Inventory inventory, OilWellBlockEntity well) {
        this(id, inventory, well, definitionValue(OilWellDefinition::requiredTrait),
                definitionValue(OilWellDefinition::requiredMod),
                definitionValue(definition -> definition.output().fluid()));
    }

    private OilWellMenu(int id, Inventory inventory, OilWellBlockEntity well,
            String requiredTrait, String requiredMod, String outputFluid) {
        super(ModMenus.OIL_WELL.get(), id);
        this.requiredTrait = requiredTrait;
        this.requiredMod = requiredMod;
        this.outputFluid = outputFluid;
        access = ContainerLevelAccess.create(inventory.player.level(), well.getBlockPos());
        progress = addDataSlot(new DataSlot() {
            @Override public int get() { return well.progress(); }
            @Override public void set(int value) { well.setProgressFromMenu(value); }
        });
        status = addDataSlot(new DataSlot() {
            @Override public int get() { return well.status().ordinal(); }
            @Override public void set(int value) { well.setStatusFromMenu(value); }
        });
        workInterval = addDataSlot(new DataSlot() {
            @Override public int get() { return well.workInterval(); }
            @Override public void set(int value) { well.setWorkIntervalFromMenu(value); }
        });
        fluidAmount = addDataSlot(new DataSlot() {
            private int clientValue = well.tank().getFluidAmount();
            @Override public int get() {
                return inventory.player.level().isClientSide ? clientValue : well.tank().getFluidAmount();
            }
            @Override public void set(int value) { clientValue = value; }
        });
        capacity = addDataSlot(new DataSlot() {
            private int clientValue = well.tank().getCapacity();
            @Override public int get() {
                return inventory.player.level().isClientSide ? clientValue : well.tank().getCapacity();
            }
            @Override public void set(int value) { clientValue = Math.max(1, value); }
        });
    }

    private static String definitionValue(java.util.function.Function<OilWellDefinition, ?> getter) {
        return dev.upiscium.frontierprotocol.data.OilWellReloadListener.find(OilWellBlockEntity.DEFINITION_ID)
                .map(getter).map(Object::toString).orElse("none");
    }

    private static OilWellBlockEntity findWell(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof OilWellBlockEntity well) return well;
        throw new IllegalStateException("Missing oil well at " + pos);
    }

    public OilWellStatus status() {
        return OilWellStatus.values()[Math.max(0, Math.min(OilWellStatus.values().length - 1, status.get()))];
    }

    public String requiredTrait() { return requiredTrait; }
    public String requiredMod() { return requiredMod; }
    public String outputFluid() { return outputFluid; }
    public int fluidAmount() { return fluidAmount.get(); }
    public int capacity() { return capacity.get(); }

    public int scaledProgress(int width) {
        return progress.get() * width / Math.max(1, workInterval.get());
    }

    public int scaledFluid(int height) {
        return fluidAmount.get() * height / Math.max(1, capacity.get());
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.OIL_WELL.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
