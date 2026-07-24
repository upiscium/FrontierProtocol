package dev.upiscium.frontierprotocol.stabilizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        StabilizerNbt.write(tag, StabilizerTier.TIER_1, original, originalInventory, registries);
        ItemStackHandler restoredInventory = new ItemStackHandler(1);
        StabilizerStateMachine restored =
                StabilizerNbt.read(tag, StabilizerTier.TIER_1, restoredInventory, registries);

        assertEquals(StabilizerNbt.SCHEMA_VERSION, tag.getInt("schemaVersion"));
        assertEquals("tier_1", tag.getString("tier"));
        assertEquals(StabilizerStatus.GRACE_PERIOD, restored.status());
        assertEquals(77, restored.graceRemainingTicks());
        assertEquals(123, restored.cellRemainingTicks());
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

        StabilizerStateMachine restored = StabilizerNbt.read(
                tag, StabilizerTier.TIER_1, new ItemStackHandler(1), registries);

        assertEquals(StabilizerStatus.OFFLINE, restored.status());
        assertEquals(0, restored.graceRemainingTicks());
        assertEquals(0, restored.cellRemainingTicks());
    }

    @Test
    void blockStateTierWinsWhenStoredTierDoesNotMatch() {
        RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        CompoundTag tag = new CompoundTag();
        tag.putString("tier", StabilizerTier.TIER_3.serializedName());
        tag.putString("status", StabilizerStatus.ACTIVE.getSerializedName());
        tag.putInt("graceRemainingTicks", 9);
        tag.putInt("cellRemainingTicks", 17);

        StabilizerStateMachine restored = StabilizerNbt.read(
                tag, StabilizerTier.TIER_1, new ItemStackHandler(1), registries);

        assertEquals(StabilizerStatus.ACTIVE, restored.status());
        assertEquals(9, restored.graceRemainingTicks());
        assertEquals(17, restored.cellRemainingTicks());
    }

    @Test
    void tierTwoRoundTripPreservesTierLifecycleAndInventory() {
        RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        StabilizerStateMachine original = new StabilizerStateMachine(StabilizerStatus.ACTIVE, 90, 211);
        ItemStackHandler inventory = new ItemStackHandler(1);
        inventory.setStackInSlot(0, new ItemStack(Items.COPPER_INGOT, 12));
        CompoundTag tag = new CompoundTag();

        StabilizerNbt.write(tag, StabilizerTier.TIER_2, original, inventory, registries);
        ItemStackHandler restoredInventory = new ItemStackHandler(1);
        StabilizerStateMachine restored = StabilizerNbt.read(
                tag, StabilizerTier.TIER_2, restoredInventory, registries);

        assertEquals("tier_2", tag.getString("tier"));
        assertEquals(StabilizerStatus.ACTIVE, restored.status());
        assertEquals(90, restored.graceRemainingTicks());
        assertEquals(211, restored.cellRemainingTicks());
        assertEquals(12, restoredInventory.getStackInSlot(0).getCount());
    }
}
