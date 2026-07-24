package dev.upiscium.frontierprotocol.stabilizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.electronwill.nightconfig.core.CommentedConfig;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import java.lang.reflect.Constructor;
import net.neoforged.fml.config.IConfigSpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class StabilizerStateMachineTest {
    @BeforeAll
    static void loadConfig() throws ReflectiveOperationException {
        CommentedConfig config = CommentedConfig.inMemory();
        FrontierProtocolServerConfig.SPEC.correct(config);
        Class<?> loadedConfigClass = Class.forName("net.neoforged.fml.config.LoadedConfig");
        Constructor<?> constructor = loadedConfigClass.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        IConfigSpec.ILoadedConfig loadedConfig =
                (IConfigSpec.ILoadedConfig) constructor.newInstance(config, null, null);
        FrontierProtocolServerConfig.SPEC.acceptConfig(loadedConfig);
    }

    @Test
    void consumesOneItemAndBecomesActiveWhenPowered() {
        StabilizerStateMachine machine = new StabilizerStateMachine();

        StabilizerStateMachine.TickResult result = machine.tick(true, true, 4, 10);

        assertTrue(result.consumeItem());
        assertEquals(StabilizerStatus.ACTIVE, machine.status());
        assertEquals(9, machine.cellRemainingTicks());
        assertEquals(4, machine.graceRemainingTicks());
    }

    @Test
    void activeTransitionsThroughGraceToOffline() {
        StabilizerStateMachine machine =
                new StabilizerStateMachine(StabilizerStatus.ACTIVE, 3, 0);

        machine.tick(false, false, 3, 10);
        assertEquals(StabilizerStatus.GRACE_PERIOD, machine.status());
        assertEquals(2, machine.graceRemainingTicks());

        machine.tick(false, false, 3, 10);
        machine.tick(false, false, 3, 10);
        assertEquals(StabilizerStatus.OFFLINE, machine.status());
        assertEquals(0, machine.graceRemainingTicks());
    }

    @Test
    void graceRecoversToActiveWithoutResettingConsumable() {
        StabilizerStateMachine machine =
                new StabilizerStateMachine(StabilizerStatus.GRACE_PERIOD, 12, 5);

        StabilizerStateMachine.TickResult result = machine.tick(true, false, 20, 10);

        assertFalse(result.consumeItem());
        assertEquals(StabilizerStatus.ACTIVE, machine.status());
        assertEquals(4, machine.cellRemainingTicks());
        assertEquals(20, machine.graceRemainingTicks());
    }

    @Test
    void noConsumableKeepsOfflineMachineOffline() {
        StabilizerStateMachine machine = new StabilizerStateMachine();

        machine.tick(true, false, 20, 10);

        assertEquals(StabilizerStatus.OFFLINE, machine.status());
        assertEquals(0, machine.cellRemainingTicks());
    }

    @Test
    void consumesExactlyOneNewItemOnTickAfterRuntimeExpires() {
        StabilizerStateMachine machine =
                new StabilizerStateMachine(StabilizerStatus.ACTIVE, 4, 1);

        StabilizerStateMachine.TickResult finalRuntimeTick = machine.tick(true, true, 4, 10);
        StabilizerStateMachine.TickResult refillTick = machine.tick(true, true, 4, 10);

        assertFalse(finalRuntimeTick.consumeItem());
        assertTrue(refillTick.consumeItem());
        assertEquals(StabilizerStatus.ACTIVE, machine.status());
        assertEquals(9, machine.cellRemainingTicks());
    }

    @Test
    void resolvedTierDurationsDriveTheSharedMachine() {
        for (StabilizerTier tier : StabilizerTier.values()) {
            StabilizerTierDefinition definition = StabilizerTierDefinitions.resolve(tier);
            StabilizerStateMachine machine = new StabilizerStateMachine();

            StabilizerStateMachine.TickResult activation = machine.tick(
                    true, true, definition.gracePeriodTicks(), definition.cellDurationTicks());

            assertTrue(activation.consumeItem(), tier.serializedName());
            assertEquals(definition.cellDurationTicks() - 1, machine.cellRemainingTicks(), tier.serializedName());
            assertEquals(definition.gracePeriodTicks(), machine.graceRemainingTicks(), tier.serializedName());

            machine.tick(false, false, definition.gracePeriodTicks(), definition.cellDurationTicks());
            StabilizerStatus expected = definition.gracePeriodTicks() == 0
                    ? StabilizerStatus.OFFLINE
                    : StabilizerStatus.GRACE_PERIOD;
            assertEquals(expected, machine.status(), tier.serializedName());
            assertEquals(Math.max(0, definition.gracePeriodTicks() - 1),
                    machine.graceRemainingTicks(), tier.serializedName());
        }
    }
}
