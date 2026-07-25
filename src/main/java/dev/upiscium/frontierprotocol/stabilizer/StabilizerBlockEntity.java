package dev.upiscium.frontierprotocol.stabilizer;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSource;
import dev.upiscium.frontierprotocol.cleanup.CleanupActivationMode;
import dev.upiscium.frontierprotocol.cleanup.CleanupSourceProfile;
import dev.upiscium.frontierprotocol.cleanup.ServerInfectionCleanupService;
import dev.upiscium.frontierprotocol.registry.ModBlockEntities;
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

public final class StabilizerBlockEntity extends KineticBlockEntity {
    private final StabilizerTier tier;
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModItemTags.STABILIZER_CONSUMABLES);
        }

        @Override
        public int getSlotLimit(int slot) {
            return definition().cellCapacity();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (getStackInSlot(slot).getCount() > getSlotLimit(slot)) return stack;
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        protected void onContentsChanged(int slot) {
            StabilizerBlockEntity.this.setChanged();
        }
    };
    private StabilizerStateMachine machine = new StabilizerStateMachine();
    private boolean sourceRegistered;
    private CleanupRegistration cleanupRegistration = CleanupRegistration.NONE;
    private boolean resumeCleanupOnActivation;
    private boolean needsEvaluation = true;
    private Set<ChunkPos> registeredCoverage;
    private CleanupSourceProfile registeredProfile;

    public StabilizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STABILIZER.get(), pos, state);
        tier = StabilizerTier.fromBlock(state);
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
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (isRemoved() || isVirtual()) {
            unregisterSource(serverLevel);
            return;
        }
        BlockState state = getBlockState();
        if (serverLevel.getBlockEntity(worldPosition) != this
                || !(state.getBlock() instanceof StabilizerBlock)
                || StabilizerTier.fromBlock(state) != tier) {
            unregisterSource(serverLevel);
            return;
        }

        StabilizerTierDefinition definition = definition();
        boolean powered = hasNetwork()
                && !isOverStressed()
                && isRpmSufficient(getSpeed(), definition.minimumRpm());
        StabilizerStateMachine.TickResult result = machine.tick(
                powered,
                hasCell(),
                definition.gracePeriodTicks(),
                definition.cellDurationTicks());
        if (result.consumeItem()) consumeOne();
        if (needsEvaluation || result.statusChanged()) updateBlockState();
        if (result.changed()) setChanged();
        Set<ChunkPos> coverage = StabilizerCoverage.coveredChunks(worldPosition, definition.chunkRadius());
        boolean registrationChanged = !coverage.equals(registeredCoverage)
                || !definition.cleanupProfile().equals(registeredProfile);
        if (needsEvaluation || result.statusChanged() || registrationChanged) {
            syncSource(serverLevel, coverage, definition.cleanupProfile(), registrationChanged);
        }
        needsEvaluation = false;
    }

    static boolean isRpmSufficient(float speed, int minimumRpm) {
        return Math.abs(speed) >= minimumRpm;
    }

    StabilizerStatus status() {
        return machine.status();
    }

    int graceRemainingTicks() {
        return machine.graceRemainingTicks();
    }

    int cellRemainingTicks() {
        return machine.cellRemainingTicks();
    }

    private StabilizerTierDefinition definition() {
        return StabilizerTierDefinitions.resolve(tier);
    }

    private boolean hasCell() {
        ItemStack stack = inventory.getStackInSlot(0);
        return !stack.isEmpty() && inventory.isItemValid(0, stack);
    }

    private void consumeOne() {
        ItemStack stack = inventory.getStackInSlot(0).copy();
        if (stack.isEmpty() || !inventory.isItemValid(0, stack)) return;
        stack.shrink(1);
        inventory.setStackInSlot(0, stack);
    }

    void dropInventory(ServerLevel serverLevel) {
        ItemStack stack = inventory.getStackInSlot(0);
        if (stack.isEmpty()) return;
        Containers.dropItemStack(
                serverLevel, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
        inventory.setStackInSlot(0, ItemStack.EMPTY);
    }

    private void updateBlockState() {
        BlockState state = getBlockState();
        if (level != null && state.hasProperty(StabilizerBlock.STATUS)
                && state.getValue(StabilizerBlock.STATUS) != machine.status()) {
            level.setBlock(
                    worldPosition,
                    state.setValue(StabilizerBlock.STATUS, machine.status()),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    private void syncSource(
            ServerLevel serverLevel,
            Set<ChunkPos> coverage,
            CleanupSourceProfile profile,
            boolean registrationChanged) {
        if (machine.status().suppressesInfection()) {
            if (!sourceRegistered || !coverage.equals(registeredCoverage)) {
                SuppressionSource source = StabilizerSuppressionSource.at(tier, worldPosition);
                ServerInfectionSuppressionService.INSTANCE.registerOrUpdateSource(serverLevel, source, coverage);
                sourceRegistered = true;
            }
        } else {
            unregisterSuppressionSource(serverLevel);
        }

        switch (machine.status()) {
            case ACTIVE -> activateCleanup(serverLevel, coverage, profile, registrationChanged);
            case GRACE_PERIOD -> pauseCleanup(serverLevel, coverage, profile, registrationChanged);
            case OFFLINE -> deactivateCleanup(serverLevel);
        }
        registeredCoverage = coverage;
        registeredProfile = profile;
    }

    private void unregisterSource(ServerLevel serverLevel) {
        unregisterSuppressionSource(serverLevel);
        deactivateCleanup(serverLevel);
        registeredCoverage = null;
        registeredProfile = null;
    }

    private void unregisterSuppressionSource(ServerLevel serverLevel) {
        if (!sourceRegistered) return;
        ServerInfectionSuppressionService.INSTANCE.unregisterSource(
                serverLevel, StabilizerSuppressionSource.at(tier, worldPosition).id());
        sourceRegistered = false;
    }

    private void activateCleanup(
            ServerLevel serverLevel,
            Set<ChunkPos> coverage,
            CleanupSourceProfile profile,
            boolean registrationChanged) {
        if (cleanupRegistration == CleanupRegistration.ACTIVE && !registrationChanged) return;
        CleanupActivationMode mode;
        if (cleanupRegistration == CleanupRegistration.ACTIVE) {
            mode = coverage.equals(registeredCoverage)
                    ? CleanupActivationMode.RESUME
                    : CleanupActivationMode.NEW_PASS;
        } else {
            mode = cleanupRegistration == CleanupRegistration.PAUSED || resumeCleanupOnActivation
                    ? CleanupActivationMode.RESUME
                    : CleanupActivationMode.NEW_PASS;
        }
        ServerInfectionCleanupService.INSTANCE.registerActiveSource(
                serverLevel,
                StabilizerSuppressionSource.at(tier, worldPosition).id(),
                coverage,
                mode,
                profile);
        cleanupRegistration = CleanupRegistration.ACTIVE;
        resumeCleanupOnActivation = false;
    }

    private void pauseCleanup(
            ServerLevel serverLevel,
            Set<ChunkPos> coverage,
            CleanupSourceProfile profile,
            boolean registrationChanged) {
        if (cleanupRegistration == CleanupRegistration.PAUSED && !registrationChanged) return;
        if (cleanupRegistration == CleanupRegistration.NONE && !resumeCleanupOnActivation) return;
        ServerInfectionCleanupService.INSTANCE.registerPausedSource(
                serverLevel,
                StabilizerSuppressionSource.at(tier, worldPosition).id(),
                coverage,
                CleanupActivationMode.RESUME,
                profile);
        cleanupRegistration = CleanupRegistration.PAUSED;
        resumeCleanupOnActivation = false;
    }

    private void deactivateCleanup(ServerLevel serverLevel) {
        ServerInfectionCleanupService.INSTANCE.deactivateSource(
                serverLevel, StabilizerSuppressionSource.at(tier, worldPosition).id());
        cleanupRegistration = CleanupRegistration.NONE;
        resumeCleanupOnActivation = false;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        sourceRegistered = false;
        cleanupRegistration = CleanupRegistration.NONE;
        registeredCoverage = null;
        registeredProfile = null;
        needsEvaluation = true;
    }

    @Override
    public void onChunkUnloaded() {
        if (level instanceof ServerLevel serverLevel) {
            unregisterSuppressionSource(serverLevel);
            StabilizerTierDefinition definition = definition();
            Set<ChunkPos> coverage = StabilizerCoverage.coveredChunks(worldPosition, definition.chunkRadius());
            pauseCleanup(serverLevel, coverage, definition.cleanupProfile(), true);
            registeredCoverage = coverage;
            registeredProfile = definition.cleanupProfile();
        }
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
            dropInventory(serverLevel);
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        StabilizerNbt.write(tag, tier, machine, inventory, registries);
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        machine = StabilizerNbt.read(tag, tier, inventory, registries);
        sourceRegistered = false;
        cleanupRegistration = CleanupRegistration.NONE;
        resumeCleanupOnActivation = machine.status() != StabilizerStatus.OFFLINE;
        registeredCoverage = null;
        registeredProfile = null;
        needsEvaluation = true;
    }

    private enum CleanupRegistration {
        NONE,
        PAUSED,
        ACTIVE
    }
}
