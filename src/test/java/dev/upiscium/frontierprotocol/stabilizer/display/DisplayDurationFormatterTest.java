package dev.upiscium.frontierprotocol.stabilizer.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DisplayDurationFormatterTest {
    @Test
    void roundsTicksUpAndFormatsWithoutLocaleDependence() {
        assertEquals("0:00", DisplayDurationFormatter.formatTicks(0));
        assertEquals("0:01", DisplayDurationFormatter.formatTicks(1));
        assertEquals("0:01", DisplayDurationFormatter.formatTicks(20));
        assertEquals("0:02", DisplayDurationFormatter.formatTicks(21));
        assertEquals("1:00", DisplayDurationFormatter.formatTicks(1200));
        assertEquals("1:00:00", DisplayDurationFormatter.formatTicks(72000));
        assertThrows(IllegalArgumentException.class, () -> DisplayDurationFormatter.formatTicks(-1));
    }
}
