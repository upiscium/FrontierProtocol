package dev.upiscium.frontierprotocol.stabilizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;

class StabilizerNbtTest {
    @Test
    void roundTripPreservesSchemaTierGraceCellAndInventory() {
        RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        StabilizerStateMachine original =
                new StabilizerStateMachine(StabilizerStatus.GRACE_PERIOD, 77, 123);
        ItemStackHandler originalInventory = new ItemStackHandler(1);
        originalInventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 3));
        CompoundTag tag = new CompoundTag();

        StabilizerNbt.write(tag, StabilizerTier.TIER_1, original, 2, originalInventory, registries);
        ItemStackHandler restoredInventory = new ItemStackHandler(1);
        StabilizerNbt.ReadResult restored =
                StabilizerNbt.read(tag, StabilizerTier.TIER_1, 1200, restoredInventory, registries);

        assertEquals(StabilizerNbt.SCHEMA_VERSION, tag.getInt("schemaVersion"));
        assertEquals("tier_1", tag.getString("tier"));
        assertEquals(StabilizerStatus.GRACE_PERIOD, restored.machine().status());
        assertEquals(77, restored.machine().graceRemainingTicks());
        assertEquals(123, restored.machine().cellRemainingTicks());
        assertEquals(2, restored.registeredChunkRadius());
        assertEquals(3, restoredInventory.getStackInSlot(0).getCount());
        assertEquals(Items.IRON_INGOT, restoredInventory.getStackInSlot(0).getItem());
    }

    @Test
    void invalidStateAndNegativeCountersAreCorrected() {
        RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        CompoundTag tag = new CompoundTag();
        tag.putString("tier", StabilizerTier.TIER_1.serializedName());
        tag.putString("status", "future_state");
        tag.putInt("graceRemainingTicks", -7);
        tag.putInt("cellRemainingTicks", -11);

        StabilizerNbt.ReadResult restored = StabilizerNbt.read(
                tag, StabilizerTier.TIER_1, 1200, new ItemStackHandler(1), registries);

        assertEquals(StabilizerStatus.OFFLINE, restored.machine().status());
        assertEquals(0, restored.machine().graceRemainingTicks());
        assertEquals(0, restored.machine().cellRemainingTicks());
        assertNull(restored.registeredChunkRadius());
    }

    @Test
    void blockStateTierWinsWhenStoredTierDoesNotMatch() {
        RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        CompoundTag tag = new CompoundTag();
        tag.putString("tier", StabilizerTier.TIER_3.serializedName());
        tag.putString("status", StabilizerStatus.ACTIVE.getSerializedName());
        tag.putInt("graceRemainingTicks", 9);
        tag.putInt("cellRemainingTicks", 17);

        StabilizerNbt.ReadResult restored = StabilizerNbt.read(
                tag, StabilizerTier.TIER_1, 1200, new ItemStackHandler(1), registries);

        assertEquals(StabilizerStatus.ACTIVE, restored.machine().status());
        assertEquals(9, restored.machine().graceRemainingTicks());
        assertEquals(17, restored.machine().cellRemainingTicks());
    }

    @Test
    void tierTwoRoundTripPreservesTierLifecycleAndInventory() {
        RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        StabilizerStateMachine original = new StabilizerStateMachine(StabilizerStatus.ACTIVE, 90, 211);
        ItemStackHandler inventory = new ItemStackHandler(1);
        inventory.setStackInSlot(0, new ItemStack(Items.COPPER_INGOT, 12));
        CompoundTag tag = new CompoundTag();

        StabilizerNbt.write(tag, StabilizerTier.TIER_2, original, 1, inventory, registries);
        ItemStackHandler restoredInventory = new ItemStackHandler(1);
        StabilizerNbt.ReadResult restored = StabilizerNbt.read(
                tag, StabilizerTier.TIER_2, 1800, restoredInventory, registries);

        assertEquals("tier_2", tag.getString("tier"));
        assertEquals(StabilizerStatus.ACTIVE, restored.machine().status());
        assertEquals(90, restored.machine().graceRemainingTicks());
        assertEquals(211, restored.machine().cellRemainingTicks());
        assertEquals(1, restored.registeredChunkRadius());
        assertEquals(12, restoredInventory.getStackInSlot(0).getCount());
    }

    @Test
    void tierThreeRoundTripPreservesTierLifecycleAndInventory() {
        RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        StabilizerStateMachine original = new StabilizerStateMachine(StabilizerStatus.GRACE_PERIOD, 240, 511);
        ItemStackHandler inventory = new ItemStackHandler(1);
        inventory.setStackInSlot(0, new ItemStack(Items.NETHERITE_INGOT, 64));
        CompoundTag tag = new CompoundTag();

        StabilizerNbt.write(tag, StabilizerTier.TIER_3, original, 2, inventory, registries);
        ItemStackHandler restoredInventory = new ItemStackHandler(1);
        StabilizerNbt.ReadResult restored = StabilizerNbt.read(
                tag, StabilizerTier.TIER_3, 2400, restoredInventory, registries);

        assertEquals("tier_3", tag.getString("tier"));
        assertEquals(StabilizerStatus.GRACE_PERIOD, restored.machine().status());
        assertEquals(240, restored.machine().graceRemainingTicks());
        assertEquals(511, restored.machine().cellRemainingTicks());
        assertEquals(2, restored.registeredChunkRadius());
        assertEquals(64, restoredInventory.getStackInSlot(0).getCount());
    }

    @Test
    void negativeRegisteredChunkRadiusIsIgnored() {
        RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        CompoundTag tag = new CompoundTag();
        tag.putString("tier", StabilizerTier.TIER_1.serializedName());
        tag.putString("status", StabilizerStatus.GRACE_PERIOD.getSerializedName());
        tag.putInt("registeredChunkRadius", -1);

        StabilizerNbt.ReadResult restored = StabilizerNbt.read(
                tag, StabilizerTier.TIER_1, 1200, new ItemStackHandler(1), registries);

        assertNull(restored.registeredChunkRadius());
    }

    @Test
    void savedGraceIsClampedButStartedCellTimeIsPreserved() {
        RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        CompoundTag tag = new CompoundTag();
        tag.putString("tier", StabilizerTier.TIER_1.serializedName());
        tag.putString("status", StabilizerStatus.ACTIVE.getSerializedName());
        tag.putInt("graceRemainingTicks", 12000);
        tag.putInt("cellRemainingTicks", 9000);

        StabilizerNbt.ReadResult restored = StabilizerNbt.read(
                tag, StabilizerTier.TIER_1, 1200, new ItemStackHandler(1), registries);

        assertEquals(1200, restored.machine().graceRemainingTicks());
        assertEquals(9000, restored.machine().cellRemainingTicks());
    }
}
