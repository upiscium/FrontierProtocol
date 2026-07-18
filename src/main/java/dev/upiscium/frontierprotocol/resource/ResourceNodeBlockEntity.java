package dev.upiscium.frontierprotocol.resource;

import dev.upiscium.frontierprotocol.data.ResourceNodeReloadListener;
import dev.upiscium.frontierprotocol.data.TraitReloadListener;
import dev.upiscium.frontierprotocol.registry.ModBlockEntities;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class ResourceNodeBlockEntity extends BlockEntity implements MenuProvider {
    private static final String OUTPUT_TAG = "Output";
    private static final String PROGRESS_TAG = "Progress";
    private static final String DEFINITION_TAG = "Definition";

    private final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            ResourceNodeBlockEntity.this.setChanged();
        }
    };
    private ResourceLocation definitionId;
    private int progress;
    private ResourceNodeStatus status = ResourceNodeStatus.UNBOUND;
    private int workInterval = 1;

    public ResourceNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESOURCE_NODE.get(), pos, state);
    }

    public ItemStackHandler output() {
        return output;
    }

    public int progress() {
        return progress;
    }

    public int workInterval() {
        return workInterval;
    }

    public ResourceNodeStatus status() {
        return status;
    }

    public Optional<ResourceLocation> definitionId() {
        return Optional.ofNullable(definitionId);
    }

    public void setDefinitionId(ResourceLocation id) {
        definitionId = id;
        progress = 0;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResourceNodeBlockEntity node) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (node.definitionId == null) node.tryBind(serverLevel);
        Optional<ResourceNodeDefinition> definition = node.definitionId == null
                ? Optional.empty() : ResourceNodeReloadListener.find(node.definitionId);
        if (definition.isEmpty()) {
            node.setStatus(node.definitionId == null ? ResourceNodeStatus.UNBOUND : ResourceNodeStatus.MISSING_DEFINITION);
            return;
        }
        ResourceNodeDefinition value = definition.get();
        node.workInterval = value.workInterval();
        WorkResult work = ResourceNodeWorkService.requirement(value).evaluate(serverLevel, pos);
        if (!work.satisfied()) {
            node.setStatus(work.blockingReason().filter(ResourceNodeWorkService.PROTECTION_REQUIRED::equals).isPresent()
                    ? ResourceNodeStatus.PROTECTION_REQUIRED : ResourceNodeStatus.WRONG_TRAIT);
            return;
        }
        ItemStack result = value.output().createStack();
        if (!node.canAccept(result)) {
            node.setStatus(ResourceNodeStatus.OUTPUT_FULL);
            return;
        }
        node.setStatus(ResourceNodeStatus.WORKING);
        node.progress++;
        if (node.progress >= value.workInterval()) {
            node.progress = 0;
            node.produce(result);
        }
        node.setChanged();
    }

    private boolean canAccept(ItemStack result) {
        ItemStack stored = output.getStackInSlot(0);
        if (stored.isEmpty()) return result.getCount() <= result.getMaxStackSize();
        return ItemStack.isSameItemSameComponents(stored, result)
                && stored.getCount() + result.getCount() <= Math.min(output.getSlotLimit(0), stored.getMaxStackSize());
    }

    private void produce(ItemStack result) {
        ItemStack stored = output.getStackInSlot(0);
        if (stored.isEmpty()) {
            output.setStackInSlot(0, result.copy());
        } else {
            ItemStack changed = stored.copy();
            changed.grow(result.getCount());
            output.setStackInSlot(0, changed);
        }
    }

    private void tryBind(ServerLevel level) {
        ResourceNodeWorkService.currentTrait(level, worldPosition).flatMap(trait ->
                TraitReloadListener.definitions().stream().filter(definition -> definition.id().equals(trait)).findFirst())
                .flatMap(trait -> trait.resourceNodes().stream().filter(id -> ResourceNodeReloadListener.find(id).isPresent()).findFirst())
                .ifPresent(this::setDefinitionId);
    }

    private void setStatus(ResourceNodeStatus value) {
        if (status != value) {
            status = value;
            setChanged();
        }
    }

    public void writeMenuData(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        ResourceNodeDefinition definition = definitionId == null ? null : ResourceNodeReloadListener.find(definitionId).orElse(null);
        buffer.writeUtf(definition == null ? "none" : definition.requiredTrait().toString());
    }

    void setProgressFromMenu(int value) {
        progress = value;
    }

    void setStatusFromMenu(int value) {
        status = ResourceNodeStatus.values()[Math.max(0, Math.min(ResourceNodeStatus.values().length - 1, value))];
    }

    void setWorkIntervalFromMenu(int value) {
        workInterval = Math.max(1, value);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.frontier_protocol.resource_node");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ResourceNodeMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(OUTPUT_TAG, output.serializeNBT(registries));
        tag.putInt(PROGRESS_TAG, progress);
        if (definitionId != null) tag.putString(DEFINITION_TAG, definitionId.toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(OUTPUT_TAG, Tag.TAG_COMPOUND)) output.deserializeNBT(registries, tag.getCompound(OUTPUT_TAG));
        progress = Math.max(0, tag.getInt(PROGRESS_TAG));
        if (tag.contains(DEFINITION_TAG, Tag.TAG_STRING)) definitionId = ResourceLocation.tryParse(tag.getString(DEFINITION_TAG));
    }
}
