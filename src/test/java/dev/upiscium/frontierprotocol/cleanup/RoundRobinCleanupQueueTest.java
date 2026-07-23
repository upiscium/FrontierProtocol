package dev.upiscium.frontierprotocol.cleanup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoundRobinCleanupQueueTest {
    @Test
    void alternatesTwoTasks() {
        RoundRobinCleanupQueue<String> queue = new RoundRobinCleanupQueue<>();
        queue.add("overworld");
        queue.add("nether");
        assertEquals(List.of("overworld", "nether", "overworld", "nether"), take(queue, 4));
    }

    @Test
    void tenTasksDoNotStarveAndDuplicatesDoNotCreateExtraTurns() {
        RoundRobinCleanupQueue<Integer> queue = new RoundRobinCleanupQueue<>();
        for (int i = 0; i < 10; i++) queue.add(i);
        queue.add(0);
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), take(queue, 10));
        assertEquals(10, queue.size());
    }

    @Test
    void removalDoesNotDisruptRemainingOrder() {
        RoundRobinCleanupQueue<String> queue = new RoundRobinCleanupQueue<>();
        queue.add("a");
        queue.add("b");
        queue.add("c");
        assertEquals("a", queue.next());
        queue.remove("b");
        assertEquals(List.of("c", "a", "c"), take(queue, 3));
        queue.remove("a");
        queue.remove("c");
        assertNull(queue.next());
    }

    private static <T> List<T> take(RoundRobinCleanupQueue<T> queue, int count) {
        List<T> values = new ArrayList<>();
        for (int i = 0; i < count; i++) values.add(queue.next());
        return values;
    }
}
