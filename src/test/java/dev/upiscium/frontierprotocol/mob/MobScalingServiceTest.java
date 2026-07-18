package dev.upiscium.frontierprotocol.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MobScalingServiceTest {
    private static final int[] THRESHOLDS = {0, 16, 40, 80, 128};

    @Test
    void tierBoundariesAreInclusiveAtTheirLowerBound() {
        assertEquals(0, MobScalingService.tierForDistance(0, THRESHOLDS));
        assertEquals(0, MobScalingService.tierForDistance(15, THRESHOLDS));
        assertEquals(1, MobScalingService.tierForDistance(16, THRESHOLDS));
        assertEquals(1, MobScalingService.tierForDistance(39, THRESHOLDS));
        assertEquals(2, MobScalingService.tierForDistance(40, THRESHOLDS));
        assertEquals(2, MobScalingService.tierForDistance(79, THRESHOLDS));
        assertEquals(3, MobScalingService.tierForDistance(80, THRESHOLDS));
        assertEquals(3, MobScalingService.tierForDistance(127, THRESHOLDS));
        assertEquals(4, MobScalingService.tierForDistance(128, THRESHOLDS));
        assertEquals(4, MobScalingService.tierForDistance(Long.MAX_VALUE, THRESHOLDS));
    }
}
