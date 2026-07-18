package dev.upiscium.frontierprotocol.nutrition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;

public record FoodHistoryState(List<NutritionEntry> entries) {
    public static final int MAX_PERSISTED_ENTRIES = 64;
    public static final FoodHistoryState EMPTY = new FoodHistoryState(List.of());
    public static final Codec<FoodHistoryState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            NutritionEntry.CODEC.listOf(0, MAX_PERSISTED_ENTRIES).fieldOf("entries").forGetter(FoodHistoryState::entries)
    ).apply(instance, FoodHistoryState::new));

    public FoodHistoryState {
        if (entries.size() > MAX_PERSISTED_ENTRIES) {
            throw new IllegalArgumentException("Nutrition history is too large");
        }
        entries = List.copyOf(entries);
    }

    public FoodHistoryState append(NutritionEntry entry, int maximumLength) {
        int limit = Math.max(1, Math.min(MAX_PERSISTED_ENTRIES, maximumLength));
        ArrayList<NutritionEntry> changed = new ArrayList<>(Math.min(limit, entries.size() + 1));
        int first = Math.max(0, entries.size() - limit + 1);
        changed.addAll(entries.subList(first, entries.size()));
        changed.add(entry);
        return new FoodHistoryState(changed);
    }
}
