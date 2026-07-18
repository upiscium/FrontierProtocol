package dev.upiscium.frontierprotocol.sector;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class SectorTraitDefinitionTest {
    @Test
    void acceptsExplicitNullMaximumDistance() {
        String json = """
                {
                  "priority": 1,
                  "placement": {
                    "spacing": 8,
                    "separation": 2,
                    "anchor_chance": 0.5,
                    "min_distance": 1,
                    "max_distance": null,
                    "cluster_min": 1,
                    "cluster_max": 2,
                    "salt": 1
                  }
                }
                """;
        var result = SectorTraitDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
        assertTrue(result.result().orElseThrow().placement().maxDistance().isEmpty());
    }
}
