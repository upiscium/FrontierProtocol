package dev.upiscium.frontierprotocol.stabilizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.electronwill.nightconfig.core.CommentedConfig;
import dev.upiscium.frontierprotocol.cleanup.CleanupSourceProfile;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import java.lang.reflect.Constructor;
import net.neoforged.fml.config.IConfigSpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class StabilizerTierDefinitionTest {
    private static final CleanupSourceProfile VALID_CLEANUP = new CleanupSourceProfile(1, 1, 1);

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
    void resolvesConfiguredDefaultsForEveryTier() {
        assertEquals(
                new StabilizerTierDefinition(
                        StabilizerTier.TIER_1, 0, 32, 16.0, 8, 6000, 6000, new CleanupSourceProfile(20, 128, 4)),
                StabilizerTierDefinitions.resolve(StabilizerTier.TIER_1));
        assertEquals(
                new StabilizerTierDefinition(
                        StabilizerTier.TIER_2,
                        1,
                        64,
                        64.0,
                        32,
                        3000,
                        9000,
                        new CleanupSourceProfile(20, 2048, 64)),
                StabilizerTierDefinitions.resolve(StabilizerTier.TIER_2));
        assertEquals(
                new StabilizerTierDefinition(
                        StabilizerTier.TIER_3,
                        2,
                        128,
                        256.0,
                        64,
                        2000,
                        12000,
                        new CleanupSourceProfile(20, 8192, 256)),
                StabilizerTierDefinitions.resolve(StabilizerTier.TIER_3));
    }

    @Test
    void resolveReadsCurrentConfigValuesForEveryTier() {
        int originalTier1Radius = FrontierProtocolServerConfig.TIER1_CHUNK_RADIUS.get();
        int originalTier2Rpm = FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.get();
        double originalTier3Stress = FrontierProtocolServerConfig.TIER3_STRESS_IMPACT.get();
        int originalTier3Capacity = FrontierProtocolServerConfig.TIER3_CELL_CAPACITY.get();
        int originalTier2Duration = FrontierProtocolServerConfig.TIER2_CELL_DURATION_TICKS.get();
        int originalTier1Grace = FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.get();
        int originalTier2Interval = FrontierProtocolServerConfig.TIER2_CLEANUP_INTERVAL_TICKS.get();
        int originalTier2Inspections =
                FrontierProtocolServerConfig.TIER2_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.get();
        int originalTier2Mutations =
                FrontierProtocolServerConfig.TIER2_CLEANUP_MUTATION_BUDGET_PER_CYCLE.get();
        try {
            FrontierProtocolServerConfig.TIER1_CHUNK_RADIUS.set(3);
            FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.set(70);
            FrontierProtocolServerConfig.TIER3_STRESS_IMPACT.set(300.0);
            FrontierProtocolServerConfig.TIER3_CELL_CAPACITY.set(48);
            FrontierProtocolServerConfig.TIER2_CELL_DURATION_TICKS.set(13000);
            FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.set(7000);
            FrontierProtocolServerConfig.TIER2_CLEANUP_INTERVAL_TICKS.set(11);
            FrontierProtocolServerConfig.TIER2_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.set(600);
            FrontierProtocolServerConfig.TIER2_CLEANUP_MUTATION_BUDGET_PER_CYCLE.set(20);

            StabilizerTierDefinition tier1 = StabilizerTierDefinitions.resolve(StabilizerTier.TIER_1);
            StabilizerTierDefinition tier2 = StabilizerTierDefinitions.resolve(StabilizerTier.TIER_2);
            StabilizerTierDefinition tier3 = StabilizerTierDefinitions.resolve(StabilizerTier.TIER_3);

            assertEquals(3, tier1.chunkRadius());
            assertEquals(7000, tier1.gracePeriodTicks());
            assertEquals(70, tier2.minimumRpm());
            assertEquals(13000, tier2.cellDurationTicks());
            assertEquals(new CleanupSourceProfile(11, 600, 20), tier2.cleanupProfile());
            assertEquals(300.0, tier3.stressImpact());
            assertEquals(48, tier3.cellCapacity());

            FrontierProtocolServerConfig.TIER1_CHUNK_RADIUS.set(4);
            StabilizerTierDefinition refreshed = StabilizerTierDefinitions.resolve(StabilizerTier.TIER_1);
            assertEquals(4, refreshed.chunkRadius());
            assertNotSame(tier1, refreshed);
        } finally {
            FrontierProtocolServerConfig.TIER1_CHUNK_RADIUS.set(originalTier1Radius);
            FrontierProtocolServerConfig.TIER2_MINIMUM_RPM.set(originalTier2Rpm);
            FrontierProtocolServerConfig.TIER3_STRESS_IMPACT.set(originalTier3Stress);
            FrontierProtocolServerConfig.TIER3_CELL_CAPACITY.set(originalTier3Capacity);
            FrontierProtocolServerConfig.TIER2_CELL_DURATION_TICKS.set(originalTier2Duration);
            FrontierProtocolServerConfig.TIER1_GRACE_PERIOD_TICKS.set(originalTier1Grace);
            FrontierProtocolServerConfig.TIER2_CLEANUP_INTERVAL_TICKS.set(originalTier2Interval);
            FrontierProtocolServerConfig.TIER2_CLEANUP_INSPECTION_BUDGET_PER_CYCLE.set(originalTier2Inspections);
            FrontierProtocolServerConfig.TIER2_CLEANUP_MUTATION_BUDGET_PER_CYCLE.set(originalTier2Mutations);
        }
    }

    @Test
    void constructorRejectsInvalidValues() {
        assertInvalid(null, 0, 1, 0.0, 1, 1, 0, VALID_CLEANUP);
        assertInvalid(StabilizerTier.TIER_1, -1, 1, 0.0, 1, 1, 0, VALID_CLEANUP);
        assertInvalid(StabilizerTier.TIER_1, 0, 0, 0.0, 1, 1, 0, VALID_CLEANUP);
        assertInvalid(StabilizerTier.TIER_1, 0, 1, -1.0, 1, 1, 0, VALID_CLEANUP);
        assertInvalid(StabilizerTier.TIER_1, 0, 1, Double.NaN, 1, 1, 0, VALID_CLEANUP);
        assertInvalid(StabilizerTier.TIER_1, 0, 1, Double.POSITIVE_INFINITY, 1, 1, 0, VALID_CLEANUP);
        assertInvalid(StabilizerTier.TIER_1, 0, 1, 0.0, 0, 1, 0, VALID_CLEANUP);
        assertInvalid(StabilizerTier.TIER_1, 0, 1, 0.0, 65, 1, 0, VALID_CLEANUP);
        assertInvalid(StabilizerTier.TIER_1, 0, 1, 0.0, 1, 0, 0, VALID_CLEANUP);
        assertInvalid(StabilizerTier.TIER_1, 0, 1, 0.0, 1, 1, -1, VALID_CLEANUP);
        assertInvalid(StabilizerTier.TIER_1, 0, 1, 0.0, 1, 1, 0, null);
    }

    @Test
    void constructorAcceptsCellCapacityBounds() {
        assertEquals(
                1,
                new StabilizerTierDefinition(
                                StabilizerTier.TIER_1, 0, 1, 0.0, 1, 1, 0, VALID_CLEANUP)
                        .cellCapacity());
        assertEquals(
                64,
                new StabilizerTierDefinition(
                                StabilizerTier.TIER_3, 0, 1, 0.0, 64, 1, 0, VALID_CLEANUP)
                        .cellCapacity());
    }

    private static void assertInvalid(
            StabilizerTier tier,
            int radius,
            int rpm,
            double stress,
            int capacity,
            int duration,
            int grace,
            CleanupSourceProfile cleanup) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StabilizerTierDefinition(
                        tier, radius, rpm, stress, capacity, duration, grace, cleanup));
    }
}
