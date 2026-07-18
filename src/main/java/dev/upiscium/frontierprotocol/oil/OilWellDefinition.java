package dev.upiscium.frontierprotocol.oil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record OilWellDefinition(
        ResourceLocation id,
        ResourceLocation requiredTrait,
        String requiredMod,
        boolean requiresProtection,
        int workInterval,
        int capacity,
        Output output) {
    private static final ResourceLocation UNBOUND = ResourceLocation.fromNamespaceAndPath("frontier_protocol", "unbound");
    private static final Codec<OilWellDefinition> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("required_trait").forGetter(OilWellDefinition::requiredTrait),
            Codec.STRING.fieldOf("required_mod").forGetter(OilWellDefinition::requiredMod),
            Codec.BOOL.fieldOf("requires_protection").forGetter(OilWellDefinition::requiresProtection),
            Codec.intRange(1, 32767).fieldOf("work_interval").forGetter(OilWellDefinition::workInterval),
            Codec.intRange(1, 32767).fieldOf("capacity").forGetter(OilWellDefinition::capacity),
            Output.CODEC.fieldOf("output").forGetter(OilWellDefinition::output)
    ).apply(instance, (trait, mod, protection, interval, capacity, output) ->
            new OilWellDefinition(UNBOUND, trait, mod, protection, interval, capacity, output)));
    public static final Codec<OilWellDefinition> CODEC = RAW_CODEC.validate(definition ->
            definition.output.amount <= definition.capacity
                    ? DataResult.success(definition)
                    : DataResult.error(() -> "output amount exceeds tank capacity"));

    public OilWellDefinition withId(ResourceLocation value) {
        return new OilWellDefinition(value, requiredTrait, requiredMod, requiresProtection, workInterval, capacity, output);
    }

    public record Output(ResourceLocation fluid, int amount) {
        public static final Codec<Output> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("fluid").forGetter(Output::fluid),
                Codec.intRange(1, 32767).fieldOf("amount").forGetter(Output::amount)
        ).apply(instance, Output::new));
    }
}
