package dev.upiscium.frontierprotocol.tier1;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSource;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.registry.ModBlockEntities;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.registry.ModItemTags;
import dev.upiscium.frontierprotocol.suppression.ServerInfectionSuppressionService;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class Tier1StabilizerBlockEntity extends KineticBlockEntity {
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModItemTags.TIER_1_STABILIZER_CONSUMABLES);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        protected void onContentsChanged(int slot) {
            Tier1StabilizerBlockEntity.this.setChanged();
        }
    };
    private Tier1StabilizerStateMachine machine = new Tier1StabilizerStateMachine();
    private boolean sourceRegistered;
    private boolean needsEvaluation = true;

    public Tier1StabilizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TIER_1_STABILIZER.get(), pos, state);
    }

    public IItemHandler externalInventory() {
        return inventory;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
    }

    @Override
    public void tick() {
        if (!hasLevel()) return;
        super.tick();
        if (!(level instanceof ServerLevel serverLevel) || isRemoved() || isVirtual()) return;
        if (serverLevel.getBlockEntity(worldPosition) != this || !getBlockState().is(ModBlocks.TIER_1_STABILIZER)) {
            unregisterSource(serverLevel);
            return;
        }

        boolean powered = hasNetwork()
                && !isOverStressed()
                && isRpmSufficient(getSpeed(), FrontierProtocolServerConfig.TIER1_MINIMUM_RPM.getAsInt());
        Tier1StabilizerStateMachine.TickResult result = machine.tick(
                powered,
                hasConsumable(),
                FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.getAsInt(),
                FrontierProtocolServerConfig.TIER1_CONSUMABLE_DURATION_TICKS.getAsInt());
        if (result.consumeItem()) consumeOne();
        if (needsEvaluation || result.statusChanged()) updateBlockState();
        if (result.changed()) setChanged();
        if (needsEvaluation || result.statusChanged()) syncSource(serverLevel);
        needsEvaluation = false;
    }

    static boolean isRpmSufficient(float speed, int minimumRpm) {
        return Math.abs(speed) >= minimumRpm;
    }

    Tier1StabilizerStatus status() {
        return machine.status();
    }

    int graceRemainingTicks() {
        return machine.graceRemainingTicks();
    }

    int consumableRemainingTicks() {
        return machine.consumableRemainingTicks();
    }

    private boolean hasConsumable() {
        ItemStack stack = inventory.getStackInSlot(0);
        return !stack.isEmpty() && inventory.isItemValid(0, stack);
    }

    private void consumeOne() {
        ItemStack stack = inventory.getStackInSlot(0).copy();
        if (stack.isEmpty() || !inventory.isItemValid(0, stack)) return;
        stack.shrink(1);
        inventory.setStackInSlot(0, stack);
    }

    private void updateBlockState() {
        BlockState state = getBlockState();
        if (level != null && state.hasProperty(Tier1StabilizerBlock.STATUS)
                && state.getValue(Tier1StabilizerBlock.STATUS) != machine.status()) {
            level.setBlock(worldPosition, state.setValue(Tier1StabilizerBlock.STATUS, machine.status()),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    private void syncSource(ServerLevel serverLevel) {
        if (machine.status().suppressesInfection()) {
            if (!sourceRegistered) {
                SuppressionSource source = Tier1SuppressionSource.at(worldPosition);
                ServerInfectionSuppressionService.INSTANCE.registerOrUpdateSource(
                        serverLevel, source, Set.of(new ChunkPos(worldPosition)));
                sourceRegistered = true;
            }
        } else {
            unregisterSource(serverLevel);
        }
    }

    private void unregisterSource(ServerLevel serverLevel) {
        if (!sourceRegistered) return;
        ServerInfectionSuppressionService.INSTANCE.unregisterSource(
                serverLevel, Tier1SuppressionSource.at(worldPosition).id());
        sourceRegistered = false;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        sourceRegistered = false;
        needsEvaluation = true;
    }

    @Override
    public void onChunkUnloaded() {
        if (level instanceof ServerLevel serverLevel) unregisterSource(serverLevel);
        super.onChunkUnloaded();
    }

    @Override
    public void remove() {
        if (level instanceof ServerLevel serverLevel) unregisterSource(serverLevel);
        super.remove();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    @Override
    public void destroy() {
        super.destroy();
        if (level instanceof ServerLevel serverLevel) {
            unregisterSource(serverLevel);
            ItemStack stack = inventory.getStackInSlot(0);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                inventory.setStackInSlot(0, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        Tier1StabilizerNbt.write(tag, machine, inventory, registries);
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        machine = Tier1StabilizerNbt.read(tag, inventory, registries);
        sourceRegistered = false;
        needsEvaluation = true;
    }
}
