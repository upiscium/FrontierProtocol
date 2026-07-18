package dev.upiscium.frontierprotocol.registry;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModItemTags {
    public static final TagKey<Item> FOOD_GRAIN = foodCategory("grain");
    public static final TagKey<Item> FOOD_VEGETABLE = foodCategory("vegetable");
    public static final TagKey<Item> FOOD_FRUIT = foodCategory("fruit");
    public static final TagKey<Item> FOOD_MEAT = foodCategory("meat");
    public static final TagKey<Item> FOOD_FISH = foodCategory("fish");
    public static final TagKey<Item> FOOD_DAIRY = foodCategory("dairy");
    public static final TagKey<Item> FOOD_SOUP = foodCategory("soup");
    public static final TagKey<Item> FOOD_DESSERT = foodCategory("dessert");
    public static final TagKey<Item> FOOD_PRESERVED = foodCategory("preserved");
    public static final List<TagKey<Item>> FOOD_CATEGORIES = List.of(
            FOOD_DAIRY, FOOD_DESSERT, FOOD_FISH, FOOD_FRUIT, FOOD_GRAIN,
            FOOD_MEAT, FOOD_PRESERVED, FOOD_SOUP, FOOD_VEGETABLE);

    private ModItemTags() {}

    private static TagKey<Item> foodCategory(String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
                FrontierProtocolMod.MOD_ID, "food_categories/" + name));
    }
}
