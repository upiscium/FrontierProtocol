package dev.upiscium.frontierprotocol.nutrition;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.registry.ModItemTags;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class FoodCategoryResolver {
    private static final Set<ResourceLocation> WARNED_ITEMS = new HashSet<>();

    private FoodCategoryResolver() {}

    public static Optional<ResourceLocation> resolve(ItemStack stack) {
        List<ResourceLocation> matches = ModItemTags.FOOD_CATEGORIES.stream()
                .filter(stack::is)
                .map(tag -> tag.location())
                .sorted()
                .toList();
        if (matches.size() > 1) {
            ResourceLocation item = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (WARNED_ITEMS.add(item)) {
                FrontierProtocolMod.LOGGER.warn("Food {} has multiple nutrition categories {}; using {}",
                        item, matches, matches.getFirst());
            }
        }
        return matches.stream().findFirst();
    }
}
