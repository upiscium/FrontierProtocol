package dev.upiscium.frontierprotocol.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class ResourceNodeDefinitionTest {
    @Test
    void validDefinitionDecodes() {
        var result = ResourceNodeDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"required_trait":"frontier_protocol:ferrous_strata","requires_protection":true,
                 "work_interval":200,"output":{"item":"minecraft:raw_iron","count":1}}
                """)).getOrThrow();
        assertEquals(200, result.workInterval());
        assertEquals(1, result.output().createStack().getCount());
    }

    @Test
    void nonPositiveIntervalAndCountFailValidation() {
        assertTrue(ResourceNodeDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"required_trait":"frontier_protocol:ferrous_strata","requires_protection":false,
                 "work_interval":0,"output":{"item":"minecraft:raw_iron","count":0}}
                """)).error().isPresent());
    }
}
