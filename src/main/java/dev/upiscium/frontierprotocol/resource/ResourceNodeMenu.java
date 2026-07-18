package dev.upiscium.frontierprotocol.resource;

import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class ResourceNodeMenu extends AbstractContainerMenu {
    private static final int NODE_SLOTS = 1;
    private final ResourceNodeBlockEntity node;
    private final ContainerLevelAccess access;
    private final String requiredTrait;
    private final DataSlot progress;
    private final DataSlot status;
    private final DataSlot workInterval;

    public ResourceNodeMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(id, inventory, findNode(inventory, data.readBlockPos()), data.readUtf());
    }

    public ResourceNodeMenu(int id, Inventory inventory, ResourceNodeBlockEntity node) {
        this(id, inventory, node, node.definitionId()
                .flatMap(dev.upiscium.frontierprotocol.data.ResourceNodeReloadListener::find)
                .map(definition -> definition.requiredTrait().toString()).orElse("none"));
    }

    private ResourceNodeMenu(int id, Inventory inventory, ResourceNodeBlockEntity node, String requiredTrait) {
        super(ModMenus.RESOURCE_NODE.get(), id);
        this.node = node;
        this.requiredTrait = requiredTrait;
        this.access = ContainerLevelAccess.create(inventory.player.level(), node.getBlockPos());
        addSlot(new SlotItemHandler(node.output(), 0, 80, 35));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        progress = addDataSlot(new DataSlot() {
            @Override public int get() { return node.progress(); }
            @Override public void set(int value) { node.setProgressFromMenu(value); }
        });
        status = addDataSlot(new DataSlot() {
            @Override public int get() { return node.status().ordinal(); }
            @Override public void set(int value) { node.setStatusFromMenu(value); }
        });
        workInterval = addDataSlot(new DataSlot() {
            @Override public int get() { return node.workInterval(); }
            @Override public void set(int value) { node.setWorkIntervalFromMenu(value); }
        });
    }

    private static ResourceNodeBlockEntity findNode(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof ResourceNodeBlockEntity node) return node;
        throw new IllegalStateException("Missing resource node at " + pos);
    }

    public ResourceNodeStatus status() {
        return ResourceNodeStatus.values()[Math.max(0, Math.min(ResourceNodeStatus.values().length - 1, status.get()))];
    }

    public String requiredTrait() {
        return requiredTrait;
    }

    public int scaledProgress(int width) {
        return progress.get() * width / Math.max(1, workInterval.get());
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.RESOURCE_NODE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack current = slot.getItem();
        ItemStack copy = current.copy();
        if (index < NODE_SLOTS) {
            if (!moveItemStackTo(current, NODE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (current.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, current);
        return copy;
    }
}
