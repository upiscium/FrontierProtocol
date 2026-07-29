package dev.upiscium.frontierprotocol.stabilizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Tier1StabilizerAnimationTest {
    @Test
    void stateColorsMatchLifecycleContract() {
        assertEquals(0x39D47A, Tier1StabilizerAnimation.stateColor(StabilizerStatus.ACTIVE));
        assertEquals(0xF1C840, Tier1StabilizerAnimation.stateColor(StabilizerStatus.GRACE_PERIOD));
        assertEquals(0xA83B3B, Tier1StabilizerAnimation.stateColor(StabilizerStatus.OFFLINE));
    }

    @Test
    void rotationRequiresActiveAndFollowsClampedSignedSpeed() {
        assertEquals(0.0F, Tier1StabilizerAnimation.coreRotationDelta(128.0F, false));
        assertEquals(3.2F, Tier1StabilizerAnimation.coreRotationDelta(32.0F, true));
        assertEquals(-6.4F, Tier1StabilizerAnimation.coreRotationDelta(-64.0F, true));
        assertEquals(9.0F, Tier1StabilizerAnimation.coreRotationDelta(1000.0F, true));
    }

    @Test
    void continuousAngleInterpolationDoesNotReverseAtEitherWrap() {
        assertEquals(359.0F, Tier1StabilizerAnimation.interpolateAngle(359.0F, 362.0F, 0.0F));
        assertEquals(360.5F, Tier1StabilizerAnimation.interpolateAngle(359.0F, 362.0F, 0.5F));
        assertEquals(362.0F, Tier1StabilizerAnimation.interpolateAngle(359.0F, 362.0F, 1.0F));
        assertEquals(-0.5F, Tier1StabilizerAnimation.interpolateAngle(1.0F, -2.0F, 0.5F));
        assertEquals(0.5F, Tier1StabilizerAnimation.renderedAngle(360.5F));
        assertEquals(359.5F, Tier1StabilizerAnimation.renderedAngle(-0.5F));
    }

    @Test
    void rebasingPreservesPositiveAndNegativeRotationDeltas() {
        float positiveTurns = Tier1StabilizerAnimation.rebaseTurns(36_002.0F);
        assertEquals(3.0F, (36_002.0F - positiveTurns) - (35_999.0F - positiveTurns));

        float negativeTurns = Tier1StabilizerAnimation.rebaseTurns(-36_002.0F);
        assertEquals(-3.0F, (-36_002.0F - negativeTurns) - (-35_999.0F - negativeTurns));
        assertEquals(0.0F, Tier1StabilizerAnimation.rebaseTurns(36_000.0F));
    }

    @Test
    void graceBlinkAcceleratesAtThresholdsAndStaysBounded() {
        assertEquals(0.75, Tier1StabilizerAnimation.graceBlinkFrequency(60, 100));
        assertEquals(1.5, Tier1StabilizerAnimation.graceBlinkFrequency(30, 100));
        assertEquals(3.0, Tier1StabilizerAnimation.graceBlinkFrequency(10, 100));
        assertEquals(0.0, Tier1StabilizerAnimation.graceBlinkFrequency(0, 0));
        for (int tick = 0; tick < 100; tick++) {
            float alpha = Tier1StabilizerAnimation.graceLightAlpha(tick / 20.0, 10, 100);
            assertTrue(alpha >= 0.25F && alpha <= 1.0F);
        }
    }
}
