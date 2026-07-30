package dev.upiscium.frontierprotocol.stabilizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StabilizerComparatorSignalTest {
    @ParameterizedTest(name = "count {0} of capacity {1} produces {2}")
    @CsvSource({
        "0, 8, 0",
        "1, 8, 2",
        "2, 8, 4",
        "3, 8, 6",
        "4, 8, 8",
        "8, 8, 15",
        "9, 8, 15",
        "1, 32, 1",
        "8, 32, 4",
        "16, 32, 8",
        "32, 32, 15",
        "1, 64, 1",
        "32, 64, 8",
        "64, 64, 15",
        "1, 0, 0",
        "1, -1, 0",
        "-1, 8, 0"
    })
    void followsNormalizedCellFullnessFormula(int count, int capacity, int expected) {
        assertEquals(expected, StabilizerComparatorSignal.calculate(count, capacity));
    }
}
