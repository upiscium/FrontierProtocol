package dev.upiscium.frontierprotocol.client.ponder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
