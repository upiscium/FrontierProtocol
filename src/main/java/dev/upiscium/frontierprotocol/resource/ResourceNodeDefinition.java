package dev.upiscium.frontierprotocol.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.DataResult;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record ResourceNodeDefinition(
        ResourceLocation id,
        ResourceLocation requiredTrait,
        boolean requiresProtection,
        int workInterval,
        Output output) {
    private static final ResourceLocation UNBOUND = ResourceLocation.fromNamespaceAndPath("frontier_protocol", "unbound");
    public static final Codec<ResourceNodeDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("required_trait").forGetter(ResourceNodeDefinition::requiredTrait),
            Codec.BOOL.fieldOf("requires_protection").forGetter(ResourceNodeDefinition::requiresProtection),
            Codec.intRange(1, 32767).fieldOf("work_interval").forGetter(ResourceNodeDefinition::workInterval),
            Output.CODEC.fieldOf("output").forGetter(ResourceNodeDefinition::output)
    ).apply(instance, (trait, protection, interval, output) ->
            new ResourceNodeDefinition(UNBOUND, trait, protection, interval, output)));

    public ResourceNodeDefinition withId(ResourceLocation value) {
        return new ResourceNodeDefinition(value, requiredTrait, requiresProtection, workInterval, output);
    }

    public record Output(Holder<Item> item, int count) {
        private static final Codec<Output> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("item").forGetter(Output::item),
                Codec.intRange(1, 64).fieldOf("count").forGetter(Output::count)
        ).apply(instance, Output::new));
        public static final Codec<Output> CODEC = RAW_CODEC.validate(output ->
                output.count <= output.item.value().getDefaultMaxStackSize()
                        ? DataResult.success(output)
                        : DataResult.error(() -> "output count exceeds the item's stack limit"));

        public ItemStack createStack() {
            return new ItemStack(item, count);
        }
    }
}
