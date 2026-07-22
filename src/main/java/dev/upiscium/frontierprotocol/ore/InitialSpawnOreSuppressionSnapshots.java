package dev.upiscium.frontierprotocol.ore;

import java.util.concurrent.ConcurrentHashMap;

final class InitialSpawnOreSuppressionSnapshots<K> {
    private final ConcurrentHashMap<K, InitialSpawnOreSuppressionSnapshot> snapshots = new ConcurrentHashMap<>();

    InitialSpawnOreSuppressionSnapshot get(K key) {
        return snapshots.getOrDefault(key, InitialSpawnOreSuppressionSnapshot.uninitialized());
    }

    void put(K key, InitialSpawnOreSuppressionSnapshot snapshot) {
        snapshots.put(key, snapshot);
    }

    void clear(K key) {
        snapshots.remove(key);
    }
}
