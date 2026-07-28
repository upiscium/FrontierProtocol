package dev.upiscium.frontierprotocol.client.ponder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.registry.ModItems;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

class FrontierProtocolPonderResourcesTest {
    private static final List<String> SCENES = List.of("operation", "coverage", "production");

    @Test
    void allSchematicsArePackaged() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        for (String scene : SCENES) {
            try (var resource = loader.getResourceAsStream(
                    "assets/frontier_protocol/ponder/stabilizer/" + scene + ".nbt")) {
                assertNotNull(resource, "missing Ponder schematic " + scene);
                assertTrue(resource.read() >= 0, "empty Ponder schematic " + scene);
            }
        }
    }

    @Test
    void englishAndJapaneseContainMatchingPonderKeys() throws Exception {
        JsonObject english = language("en_us");
        JsonObject japanese = language("ja_jp");
        for (String key : english.keySet()) {
            if (key.startsWith("frontier_protocol.ponder.")) {
                assertTrue(japanese.has(key), "Japanese translation missing " + key);
            }
        }
        for (String key : japanese.keySet()) {
            if (key.startsWith("frontier_protocol.ponder.")) {
                assertTrue(english.has(key), "English translation missing " + key);
            }
        }
    }

    @Test
    void generatedOneBasedSceneTextKeysAreTranslated() throws Exception {
        JsonObject english = language("en_us");
        JsonObject japanese = language("ja_jp");
        for (var scene : java.util.Map.of("stabilizer_operation", 5, "stabilizer_coverage", 5, "stabilizer_production", 6)
                .entrySet()) {
            String prefix = "frontier_protocol.ponder." + scene.getKey() + ".text_";
            assertTrue(!english.has(prefix + "0"), "obsolete zero-based translation " + prefix + "0");
            assertTrue(!japanese.has(prefix + "0"), "obsolete zero-based translation " + prefix + "0");
            for (int index = 1; index <= scene.getValue(); index++) {
                String key = prefix + index;
                assertTrue(english.has(key), "English translation missing " + key);
                assertTrue(japanese.has(key), "Japanese translation missing " + key);
            }
        }
    }

    @Test
    void operationComponentsUseTheirTierAndCellUsesTierOne() {
        List<FrontierProtocolPonderScenes.OperationSceneDefinition> definitions =
                FrontierProtocolPonderScenes.operationSceneDefinitions();
        assertDefinition(definitions.get(0), ModItems.TIER_1_STABILIZER.getId(), ModBlocks.TIER_1_STABILIZER, 32.0F);
        assertDefinition(definitions.get(1), ModItems.TIER_2_STABILIZER.getId(), ModBlocks.TIER_2_STABILIZER, 64.0F);
        assertDefinition(definitions.get(2), ModItems.TIER_3_STABILIZER.getId(), ModBlocks.TIER_3_STABILIZER, 128.0F);
        assertDefinition(definitions.get(3), ModItems.STABILIZATION_CELL.getId(), ModBlocks.TIER_1_STABILIZER, 32.0F);
    }

    @Test
    void productionSceneIncludesRequiredCreateEquipment() {
        FrontierProtocolPonderScenes.ProductionSceneEquipment equipment =
                FrontierProtocolPonderScenes.productionSceneEquipment();
        assertSame(com.simibubi.create.AllBlocks.MECHANICAL_MIXER.get(), equipment.mechanicalMixer());
        assertSame(com.simibubi.create.AllBlocks.BASIN.get(), equipment.basin());
        assertSame(com.simibubi.create.AllBlocks.FLUID_TANK.get(), equipment.fluidTank());
        assertSame(com.simibubi.create.AllBlocks.FLUID_PIPE.get(), equipment.fluidPipe());
        assertSame(com.simibubi.create.AllBlocks.DEPOT.get(), equipment.depot());
        assertSame(com.simibubi.create.AllBlocks.DEPLOYER.get(), equipment.deployer());
        assertSame(com.simibubi.create.AllBlocks.MECHANICAL_CRAFTER.get(), equipment.mechanicalCrafter());
        assertSame(com.simibubi.create.AllBlocks.SHAFT.get(), equipment.shaft());
    }

    @Test
    void productionSchematicContainsDynamicEquipmentPositions() throws Exception {
        try (var resource = getClass()
                .getClassLoader()
                .getResourceAsStream("assets/frontier_protocol/ponder/stabilizer/production.nbt")) {
            assertNotNull(resource, "missing Production Ponder schematic");
            var blocks = NbtIo.readCompressed(resource, NbtAccounter.unlimitedHeap())
                    .getList("blocks", Tag.TAG_COMPOUND);
            int equipmentPositions = 0;
            for (int index = 0; index < blocks.size(); index++) {
                if (blocks.getCompound(index).getList("pos", Tag.TAG_INT).getInt(1) > 0) {
                    equipmentPositions++;
                }
            }
            assertEquals(12, equipmentPositions, "dynamic Production equipment positions");
        }
    }

    private static void assertDefinition(
            FrontierProtocolPonderScenes.OperationSceneDefinition definition,
            net.minecraft.resources.ResourceLocation component,
            net.neoforged.neoforge.registries.DeferredBlock<?> block,
            float kineticSpeed) {
        assertSame(component, definition.component());
        assertSame(block, definition.block());
        org.junit.jupiter.api.Assertions.assertEquals(kineticSpeed, definition.kineticSpeed());
    }

    private JsonObject language(String locale) throws Exception {
        var resource = getClass()
                .getClassLoader()
                .getResourceAsStream("assets/frontier_protocol/lang/" + locale + ".json");
        assertNotNull(resource, "missing language " + locale);
        try (resource;
                var reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
