package dev.upiscium.frontierprotocol.stabilizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.electronwill.nightconfig.core.CommentedConfig;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import java.lang.reflect.Constructor;
import java.util.List;
import net.neoforged.fml.config.IConfigSpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class StabilizerBalanceContractTest {
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
    void r9TierDefaultsAndDerivedCapacityRemainOrdered() {
        List<Balance> balances = List.of(
                balance(StabilizerTier.TIER_1, 1, 6000, 6000, 48000, 1200),
                balance(StabilizerTier.TIER_2, 9, 3000, 27000, 96000, 1800),
                balance(StabilizerTier.TIER_3, 25, 2000, 50000, 128000, 2400));

        for (int index = 0; index < balances.size(); index++) {
            Balance actual = balances.get(index);
            StabilizerTierDefinition definition = StabilizerTierDefinitions.resolve(actual.tier());
            assertEquals(actual.coverageChunks(), coverageChunks(definition));
            assertEquals(actual.cellDurationTicks(), definition.cellDurationTicks());
            assertEquals(actual.protectedChunkTicksPerCell(),
                    actual.coverageChunks() * definition.cellDurationTicks());
            assertEquals(actual.fullBufferTicks(), definition.cellCapacity() * definition.cellDurationTicks());
            assertEquals(actual.graceTicks(), definition.gracePeriodTicks());
            assertTrue(definition.gracePeriodTicks() >= 0);

            if (index == 0) continue;
            StabilizerTierDefinition previous = StabilizerTierDefinitions.resolve(balances.get(index - 1).tier());
            assertTrue(coverageChunks(definition) > coverageChunks(previous));
            assertTrue(definition.minimumRpm() > previous.minimumRpm());
            assertTrue(definition.stressImpact() > previous.stressImpact());
            assertTrue(actual.protectedChunkTicksPerCell()
                    > balances.get(index - 1).protectedChunkTicksPerCell());
            assertTrue(definition.gracePeriodTicks() > previous.gracePeriodTicks());
        }
    }

    @Test
    void sourceCleanupBudgetsDoNotReplaceGlobalHardCaps() {
        assertEquals(512, FrontierProtocolServerConfig.CLEANUP_GLOBAL_INSPECTION_BUDGET_PER_TICK.getAsInt());
        assertEquals(16, FrontierProtocolServerConfig.CLEANUP_GLOBAL_MUTATION_BUDGET_PER_TICK.getAsInt());
        assertTrue(StabilizerTierDefinitions.resolve(StabilizerTier.TIER_3)
                        .cleanupProfile().inspectionBudget()
                > FrontierProtocolServerConfig.CLEANUP_GLOBAL_INSPECTION_BUDGET_PER_TICK.getAsInt());
        assertTrue(StabilizerTierDefinitions.resolve(StabilizerTier.TIER_3)
                        .cleanupProfile().mutationBudget()
                > FrontierProtocolServerConfig.CLEANUP_GLOBAL_MUTATION_BUDGET_PER_TICK.getAsInt());
    }

    private static Balance balance(
            StabilizerTier tier,
            int coverageChunks,
            int cellDurationTicks,
            int protectedChunkTicksPerCell,
            int fullBufferTicks,
            int graceTicks) {
        return new Balance(
                tier,
                coverageChunks,
                cellDurationTicks,
                protectedChunkTicksPerCell,
                fullBufferTicks,
                graceTicks);
    }

    private static int coverageChunks(StabilizerTierDefinition definition) {
        int diameter = definition.chunkRadius() * 2 + 1;
        return diameter * diameter;
    }

    private record Balance(
            StabilizerTier tier,
            int coverageChunks,
            int cellDurationTicks,
            int protectedChunkTicksPerCell,
            int fullBufferTicks,
            int graceTicks) {}
}
