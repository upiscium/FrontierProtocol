package dev.upiscium.frontierprotocol.oil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class OilWellDefinitionTest {
    @Test
    void validDefinitionDecodesWithoutResolvingOptionalFluid() {
        OilWellDefinition definition = OilWellDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"required_trait":"frontier_protocol:oil_field","required_mod":"tfmg",
                 "requires_protection":false,"work_interval":200,"capacity":8000,
                 "output":{"fluid":"tfmg:crude_oil","amount":250}}
                """)).getOrThrow();

        assertEquals("tfmg", definition.requiredMod());
        assertEquals("tfmg:crude_oil", definition.output().fluid().toString());
        assertEquals(250, definition.output().amount());
    }

    @Test
    void outputAmountCannotExceedCapacity() {
        assertTrue(OilWellDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"required_trait":"frontier_protocol:oil_field","required_mod":"tfmg",
                 "requires_protection":false,"work_interval":200,"capacity":100,
                 "output":{"fluid":"tfmg:crude_oil","amount":250}}
                """)).error().isPresent());
    }

    @Test
    void nonPositiveWorkValuesFailValidation() {
        assertTrue(OilWellDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"required_trait":"frontier_protocol:oil_field","required_mod":"tfmg",
                 "requires_protection":false,"work_interval":0,"capacity":0,
                 "output":{"fluid":"tfmg:crude_oil","amount":0}}
                """)).error().isPresent());
    }
}
