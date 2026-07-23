package dev.upiscium.frontierprotocol.cleanup;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

final class RoundRobinCleanupQueue<T> {
    private final ArrayDeque<T> queue = new ArrayDeque<>();
    private final Set<T> entries = new HashSet<>();

    public void add(T entry) {
        if (entries.add(entry)) queue.addLast(entry);
    }

    public void remove(T entry) {
        if (entries.remove(entry)) queue.remove(entry);
    }

    public T next() {
        T entry = queue.pollFirst();
        if (entry == null) return null;
        queue.addLast(entry);
        return entry;
    }

    public int size() {
        return queue.size();
    }

    public boolean contains(T entry) {
        return entries.contains(entry);
    }

    public void clear() {
        queue.clear();
        entries.clear();
    }
}
