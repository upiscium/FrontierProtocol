package dev.upiscium.frontierprotocol.stabilizer;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.api.suppression.SuppressionSource;
import dev.upiscium.frontierprotocol.cleanup.CleanupActivationMode;
import dev.upiscium.frontierprotocol.cleanup.CleanupSourceProfile;
import dev.upiscium.frontierprotocol.cleanup.ServerInfectionCleanupService;
import dev.upiscium.frontierprotocol.registry.ModBlockEntities;
import dev.upiscium.frontierprotocol.registry.ModItemTags;
import dev.upiscium.frontierprotocol.registry.ModItems;
import dev.upiscium.frontierprotocol.config.FrontierProtocolClientConfig;
import dev.upiscium.frontierprotocol.stabilizer.display.StabilizerDisplayNbt;
import dev.upiscium.frontierprotocol.stabilizer.display.StabilizerDisplaySnapshot;
import dev.upiscium.frontierprotocol.stabilizer.display.StabilizerDisplaySyncPolicy;
import dev.upiscium.frontierprotocol.stabilizer.display.StabilizerGoggleTooltip;
import dev.upiscium.frontierprotocol.suppression.ServerInfectionSuppressionService;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
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
            displaySyncPolicy.markDirty();
        }
    };
    private StabilizerStateMachine machine = new StabilizerStateMachine();
    private boolean sourceRegistered;
    private CleanupRegistration cleanupRegistration = CleanupRegistration.NONE;
    private boolean resumeCleanupOnActivation;
    private boolean needsEvaluation = true;
    private Set<ChunkPos> registeredCoverage;
    private CleanupSourceProfile registeredProfile;
    private int lastRegisteredChunkRadius;
    private boolean hasRegisteredChunkRadius;
    private final StabilizerDisplaySyncPolicy displaySyncPolicy = new StabilizerDisplaySyncPolicy();
    private StabilizerDisplaySnapshot lastObservedDisplaySnapshot;
    private StabilizerDisplaySnapshot clientDisplaySnapshot;
    private boolean displaySnapshotInvalid;
    private float previousClientCoreAngle;
    private float clientCoreAngle;

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
        if (!(level instanceof ServerLevel serverLevel)) {
            previousClientCoreAngle = clientCoreAngle;
            boolean active = tier == StabilizerTier.TIER_1
                    && clientDisplaySnapshot != null
                    && clientDisplaySnapshot.status() == StabilizerStatus.ACTIVE
                    && isRpmSufficient(getSpeed(), clientDisplaySnapshot.minimumRpm());
            clientCoreAngle += Tier1StabilizerAnimation.coreRotationDelta(getSpeed(), active);
            if (clientCoreAngle >= 360.0F) clientCoreAngle -= 360.0F;
            if (clientCoreAngle < 0.0F) clientCoreAngle += 360.0F;
            return;
        }
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
        int currentRadius = definition.chunkRadius();
        Set<ChunkPos> coverage = StabilizerCoverage.coveredChunks(worldPosition, currentRadius);
        boolean registrationChanged = !coverage.equals(registeredCoverage)
                || !definition.cleanupProfile().equals(registeredProfile);
        if (needsEvaluation || result.statusChanged() || registrationChanged) {
            syncSource(serverLevel, coverage, definition.cleanupProfile(), registrationChanged);
            lastRegisteredChunkRadius = currentRadius;
            hasRegisteredChunkRadius = true;
        }
        syncDisplaySnapshot(serverLevel, definition);
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

    public StabilizerDisplaySnapshot displaySnapshot() {
        if (level instanceof ServerLevel) {
            StabilizerDisplaySnapshot current = createDisplaySnapshot(definition());
            return current == null ? lastObservedDisplaySnapshot : current;
        }
        return clientDisplaySnapshot;
    }

    public StabilizerTier tier() {
        return tier;
    }

    public float clientCoreAngle(float partialTick) {
        return previousClientCoreAngle + (clientCoreAngle - previousClientCoreAngle) * partialTick;
    }

    @Override
    public boolean addToGoggleTooltip(List<net.minecraft.network.chat.Component> tooltip, boolean isPlayerSneaking) {
        boolean customAdded = false;
        if (FrontierProtocolClientConfig.SHOW_STABILIZER_GOGGLE_DETAILS.get()) {
            StabilizerDisplaySnapshot snapshot = displaySnapshot();
            customAdded = snapshot == null
                    ? StabilizerGoggleTooltip.addSynchronizing(tooltip)
                    : StabilizerGoggleTooltip.add(
                            tooltip,
                            isPlayerSneaking,
                            snapshot,
                            new ChunkPos(worldPosition),
                            getTheoreticalSpeed(),
                            isOverStressed());
        }
        return super.addToGoggleTooltip(tooltip, isPlayerSneaking) || customAdded;
    }

    @Override
    public ItemStack getIcon(boolean isPlayerSneaking) {
        return new ItemStack(switch (tier) {
            case TIER_1 -> ModItems.TIER_1_STABILIZER.get();
            case TIER_2 -> ModItems.TIER_2_STABILIZER.get();
            case TIER_3 -> ModItems.TIER_3_STABILIZER.get();
        });
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

    private StabilizerDisplaySnapshot createDisplaySnapshot(StabilizerTierDefinition definition) {
        try {
            StabilizerDisplaySnapshot snapshot = new StabilizerDisplaySnapshot(
                    tier,
                    machine.status(),
                    definition.minimumRpm(),
                    definition.stressImpact(),
                    inventory.getStackInSlot(0).getCount(),
                    definition.cellCapacity(),
                    machine.cellRemainingTicks(),
                    definition.cellDurationTicks(),
                    machine.graceRemainingTicks(),
                    definition.gracePeriodTicks(),
                    definition.chunkRadius());
            displaySnapshotInvalid = false;
            return snapshot;
        } catch (IllegalArgumentException exception) {
            if (!displaySnapshotInvalid) {
                FrontierProtocolMod.LOGGER.warn(
                        "Rejecting invalid Stabilizer display snapshot at {}: {}",
                        worldPosition,
                        exception.getMessage());
                displaySnapshotInvalid = true;
            }
            return null;
        }
    }

    private void syncDisplaySnapshot(ServerLevel serverLevel, StabilizerTierDefinition definition) {
        StabilizerDisplaySnapshot snapshot = createDisplaySnapshot(definition);
        if (snapshot == null) return;
        if (!snapshot.operationallyEquals(lastObservedDisplaySnapshot)) displaySyncPolicy.markDirty();
        lastObservedDisplaySnapshot = snapshot;
        long gameTick = serverLevel.getGameTime();
        if (!displaySyncPolicy.shouldSync(gameTick, snapshot)) return;
        sendData();
        displaySyncPolicy.recordSync(gameTick, snapshot);
    }

    boolean dropInventory(ServerLevel serverLevel) {
        ItemStack stack = inventory.getStackInSlot(0);
        if (stack.isEmpty()) return true;
        ItemEntity dropped = new ItemEntity(
                serverLevel,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5,
                stack.copy());
        dropped.setDeltaMovement(0, 0, 0);
        if (!serverLevel.addFreshEntity(dropped)) return false;
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        return true;
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
            case ACTIVE -> {
                restoreCleanupBaseline(serverLevel, coverage, profile);
                activateCleanup(serverLevel, coverage, profile, registrationChanged);
            }
            case GRACE_PERIOD -> {
                restoreCleanupBaseline(serverLevel, coverage, profile);
                pauseCleanup(serverLevel, coverage, profile, registrationChanged);
            }
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
        if (cleanupRegistration == CleanupRegistration.PAUSED && !coverage.equals(registeredCoverage)) {
            ServerInfectionCleanupService.INSTANCE.registerPausedSource(
                    serverLevel,
                    StabilizerSuppressionSource.at(tier, worldPosition).id(),
                    coverage,
                    CleanupActivationMode.NEW_PASS,
                    profile);
            registeredCoverage = coverage;
            registeredProfile = profile;
        }
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
        CleanupActivationMode mode = coverage.equals(registeredCoverage)
                ? CleanupActivationMode.RESUME
                : CleanupActivationMode.NEW_PASS;
        ServerInfectionCleanupService.INSTANCE.registerPausedSource(
                serverLevel,
                StabilizerSuppressionSource.at(tier, worldPosition).id(),
                coverage,
                mode,
                profile);
        cleanupRegistration = CleanupRegistration.PAUSED;
        resumeCleanupOnActivation = false;
    }

    private void restoreCleanupBaseline(
            ServerLevel serverLevel, Set<ChunkPos> currentCoverage, CleanupSourceProfile profile) {
        if (cleanupRegistration != CleanupRegistration.NONE || !resumeCleanupOnActivation) return;

        Set<ChunkPos> previousCoverage = null;
        if (hasRegisteredChunkRadius) {
            try {
                previousCoverage = StabilizerCoverage.coveredChunks(worldPosition, lastRegisteredChunkRadius);
            } catch (ArithmeticException exception) {
                FrontierProtocolMod.LOGGER.warn(
                        "Ignoring invalid saved Stabilizer chunk radius {} at {}",
                        lastRegisteredChunkRadius,
                        worldPosition);
                hasRegisteredChunkRadius = false;
            }
        }
        if (previousCoverage == null) {
            FrontierProtocolMod.LOGGER.warn(
                    "Stabilizer at {} has no valid registered chunk radius; resuming current coverage",
                    worldPosition);
            previousCoverage = currentCoverage;
        }

        ServerInfectionCleanupService.INSTANCE.registerPausedSource(
                serverLevel,
                StabilizerSuppressionSource.at(tier, worldPosition).id(),
                previousCoverage,
                CleanupActivationMode.RESUME,
                profile);
        cleanupRegistration = CleanupRegistration.PAUSED;
        registeredCoverage = previousCoverage;
        registeredProfile = profile;

        if (!currentCoverage.equals(previousCoverage)) {
            ServerInfectionCleanupService.INSTANCE.registerPausedSource(
                    serverLevel,
                    StabilizerSuppressionSource.at(tier, worldPosition).id(),
                    currentCoverage,
                    CleanupActivationMode.NEW_PASS,
                    profile);
            registeredCoverage = currentCoverage;
        }
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
        displaySyncPolicy.markDirty();
        lastObservedDisplaySnapshot = null;
    }

    @Override
    public void onChunkUnloaded() {
        if (level instanceof ServerLevel serverLevel) {
            unregisterSuppressionSource(serverLevel);
            StabilizerTierDefinition definition = definition();
            int currentRadius = definition.chunkRadius();
            Set<ChunkPos> coverage = StabilizerCoverage.coveredChunks(worldPosition, currentRadius);
            pauseCleanup(serverLevel, coverage, definition.cleanupProfile(), true);
            registeredCoverage = coverage;
            registeredProfile = definition.cleanupProfile();
            lastRegisteredChunkRadius = currentRadius;
            hasRegisteredChunkRadius = true;
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
        if (clientPacket) {
            StabilizerDisplaySnapshot snapshot = createDisplaySnapshot(definition());
            if (snapshot != null) StabilizerDisplayNbt.write(tag, snapshot);
        } else {
            int registeredChunkRadius = hasRegisteredChunkRadius
                    ? lastRegisteredChunkRadius
                    : definition().chunkRadius();
            StabilizerNbt.write(tag, tier, machine, registeredChunkRadius, inventory, registries);
        }
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (clientPacket) {
            clientDisplaySnapshot = StabilizerDisplayNbt.readOrRetain(tag, tier, clientDisplaySnapshot);
            return;
        }
        StabilizerNbt.ReadResult restored = StabilizerNbt.read(
                tag, tier, definition().gracePeriodTicks(), inventory, registries);
        machine = restored.machine();
        if (restored.registeredChunkRadius() != null) {
            lastRegisteredChunkRadius = restored.registeredChunkRadius();
            hasRegisteredChunkRadius = true;
        } else {
            hasRegisteredChunkRadius = false;
        }
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
