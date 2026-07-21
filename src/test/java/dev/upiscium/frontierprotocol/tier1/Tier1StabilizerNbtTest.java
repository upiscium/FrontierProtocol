package dev.upiscium.frontierprotocol.tier1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;

class Tier1StabilizerNbtTest {
    @Test
    void roundTripPreservesGraceConsumableAndInventory() {
        RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        Tier1StabilizerStateMachine original =
                new Tier1StabilizerStateMachine(Tier1StabilizerStatus.GRACE_PERIOD, 77, 123);
        ItemStackHandler originalInventory = new ItemStackHandler(1);
        originalInventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 3));
        CompoundTag tag = new CompoundTag();

        Tier1StabilizerNbt.write(tag, original, originalInventory, registries);
        ItemStackHandler restoredInventory = new ItemStackHandler(1);
        Tier1StabilizerStateMachine restored = Tier1StabilizerNbt.read(tag, restoredInventory, registries);

        assertEquals(Tier1StabilizerStatus.GRACE_PERIOD, restored.status());
        assertEquals(77, restored.graceRemainingTicks());
        assertEquals(123, restored.consumableRemainingTicks());
        assertEquals(3, restoredInventory.getStackInSlot(0).getCount());
        assertEquals(Items.IRON_INGOT, restoredInventory.getStackInSlot(0).getItem());
    }

    @Test
    void invalidStateAndNegativeCountersAreCorrected() {
        RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        CompoundTag tag = new CompoundTag();
        tag.putString("Status", "future_state");
        tag.putInt("GraceRemainingTicks", -7);
        tag.putInt("ConsumableRemainingTicks", -11);

        Tier1StabilizerStateMachine restored =
                Tier1StabilizerNbt.read(tag, new ItemStackHandler(1), registries);

        assertEquals(Tier1StabilizerStatus.OFFLINE, restored.status());
        assertEquals(0, restored.graceRemainingTicks());
        assertEquals(0, restored.consumableRemainingTicks());
    }
}
