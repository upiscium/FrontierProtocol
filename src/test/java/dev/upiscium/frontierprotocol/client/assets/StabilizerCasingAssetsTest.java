package dev.upiscium.frontierprotocol.client.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class StabilizerCasingAssetsTest {
    private static final String ROOT = "assets/frontier_protocol/";
    private static final List<String> STATES = List.of("offline", "active", "grace_period");
    private static final List<String> TEXTURES =
            List.of("front", "back", "side", "top_offline", "top_active", "top_grace");
    private static final Map<Integer, String> BASES = Map.of(
            1, "create:block/copper_casing",
            2, "create:block/andesite_casing",
            3, "create:block/brass_casing");
    private static final Map<String, Integer> LED_COLORS = Map.of(
            "offline", 0xA83B3B,
            "active", 0x39D47A,
            "grace", 0xF1C840);

    @Test
    void everyTierDefinesTwelveDirectionalStatusVariants() throws Exception {
        for (int tier = 1; tier <= 3; tier++) {
            JsonObject variants = json("blockstates/tier_" + tier + "_stabilizer.json")
                    .getAsJsonObject("variants");
            assertEquals(12, variants.size());
            for (String facing : List.of("north", "east", "south", "west")) {
                for (String state : STATES) {
                    String key = "facing=" + facing + ",status=" + state;
                    assertTrue(variants.has(key), "missing " + key + " for Tier " + tier);
                    assertFalse(key.contains("axis="));
                }
            }
        }
    }

    @Test
    void sharedParentIsOneFullCubeElement() throws Exception {
        JsonObject parent = json("models/block/stabilizer_casing_machine.json");
        assertTrue(parent.get("ambientocclusion").getAsBoolean());
        var elements = parent.getAsJsonArray("elements");
        assertEquals(1, elements.size());
        JsonObject element = elements.get(0).getAsJsonObject();
        assertEquals(List.of(0, 0, 0), ints(element, "from"));
        assertEquals(List.of(16, 16, 16), ints(element, "to"));
        assertFalse(element.has("rotation"));
        assertEquals(Set.of("north", "south", "west", "east", "up", "down"),
                element.getAsJsonObject("faces").keySet());
    }

    @Test
    void stateModelsChangeOnlyTheTopTexture() throws Exception {
        for (int tier = 1; tier <= 3; tier++) {
            JsonObject offline = stateModel(tier, "offline");
            JsonObject active = stateModel(tier, "active");
            JsonObject grace = stateModel(tier, "grace_period");
            assertEquals("frontier_protocol:block/stabilizer_casing_machine", offline.get("parent").getAsString());
            for (String slot : List.of("base", "front", "back", "side")) {
                String expected = offline.getAsJsonObject("textures").get(slot).getAsString();
                assertEquals(expected, active.getAsJsonObject("textures").get(slot).getAsString());
                assertEquals(expected, grace.getAsJsonObject("textures").get(slot).getAsString());
            }
            assertEquals(BASES.get(tier), offline.getAsJsonObject("textures").get("base").getAsString());
            assertEquals(textureReference(tier, "top_offline"), top(offline));
            assertEquals(textureReference(tier, "top_active"), top(active));
            assertEquals(textureReference(tier, "top_grace"), top(grace));
        }
    }

    @Test
    void itemModelsUseStaticOfflineParents() throws Exception {
        for (int tier = 1; tier <= 3; tier++) {
            assertEquals(
                    "frontier_protocol:block/tier_" + tier + "_stabilizer_offline",
                    json("models/item/tier_" + tier + "_stabilizer.json").get("parent").getAsString());
        }
    }

    @Test
    void allEighteenTexturesAreDetailed32PixelRgba() throws Exception {
        int count = 0;
        for (int tier = 1; tier <= 3; tier++) {
            for (String texture : TEXTURES) {
                BufferedImage image = texture(tier, texture);
                Set<Integer> colors = new HashSet<>();
                for (int pixel : pixels(image)) colors.add(pixel);
                assertTrue(colors.size() >= 6, texture + " lacks pixel detail");
                count++;
            }
        }
        assertEquals(18, count);
    }

    @Test
    void topTexturesDifferOnlyInsideLedAndUseContractColors() throws Exception {
        for (int tier = 1; tier <= 3; tier++) {
            BufferedImage offline = texture(tier, "top_offline");
            BufferedImage active = texture(tier, "top_active");
            BufferedImage grace = texture(tier, "top_grace");
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 32; x++) {
                    if (x >= 13 && x <= 18 && y >= 13 && y <= 18) continue;
                    assertEquals(offline.getRGB(x, y), active.getRGB(x, y));
                    assertEquals(offline.getRGB(x, y), grace.getRGB(x, y));
                }
            }
            assertEquals(LED_COLORS.get("offline"), offline.getRGB(14, 14) & 0xFFFFFF);
            assertEquals(LED_COLORS.get("active"), active.getRGB(14, 14) & 0xFFFFFF);
            assertEquals(LED_COLORS.get("grace"), grace.getRGB(14, 14) & 0xFFFFFF);
            assertNotEquals(active.getRGB(15, 15), offline.getRGB(15, 15));
            assertNotEquals(grace.getRGB(15, 15), offline.getRGB(15, 15));
        }
    }

    @Test
    void verifiedCreateCasingTexturesResolveWithoutCopies() {
        ClassLoader loader = getClass().getClassLoader();
        for (String casing : List.of("copper_casing", "andesite_casing", "brass_casing")) {
            assertNotNull(loader.getResource("assets/create/textures/block/" + casing + ".png"));
            assertNull(loader.getResource(ROOT + "textures/block/" + casing + ".png"));
        }
    }

    @Test
    void productionJarContainsExactStaticAssetSet() throws Exception {
        String jarPath = System.getProperty("frontierProtocol.testJar");
        assertNotNull(jarPath);
        try (JarFile jar = new JarFile(Path.of(jarPath).toFile())) {
            assertEntry(jar, ROOT + "models/block/stabilizer_casing_machine.json");
            for (int tier = 1; tier <= 3; tier++) {
                assertEntry(jar, ROOT + "blockstates/tier_" + tier + "_stabilizer.json");
                assertEntry(jar, ROOT + "models/item/tier_" + tier + "_stabilizer.json");
                for (String state : STATES) {
                    assertEntry(jar, ROOT + "models/block/tier_" + tier + "_stabilizer_" + state + ".json");
                }
                for (String texture : TEXTURES) {
                    assertEntry(jar, texturePath(tier, texture));
                }
            }
            long textureCount = jar.stream()
                    .filter(entry -> entry.getName().matches(
                            "assets/frontier_protocol/textures/block/tier_[123]_stabilizer_.*\\.png"))
                    .count();
            assertEquals(18, textureCount);
            assertNull(jar.getJarEntry(ROOT + "models/block/tier_1_stabilizer_core.json"));
            assertNull(jar.getJarEntry(ROOT + "models/block/tier_1_stabilizer_gear.json"));
        }
    }

    private JsonObject stateModel(int tier, String state) throws Exception {
        return json("models/block/tier_" + tier + "_stabilizer_" + state + ".json");
    }

    private JsonObject json(String path) throws Exception {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream(ROOT + path)) {
            assertNotNull(resource, "missing " + path);
            try (var reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }

    private BufferedImage texture(int tier, String name) throws Exception {
        String path = texturePath(tier, name);
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(resource, "missing " + path);
            BufferedImage image = ImageIO.read(resource);
            assertNotNull(image, "unreadable " + path);
            assertEquals(32, image.getWidth());
            assertEquals(32, image.getHeight());
            assertTrue(image.getColorModel().hasAlpha());
            return image;
        }
    }

    private static String texturePath(int tier, String name) {
        return ROOT + "textures/block/tier_" + tier + "_stabilizer_" + name + ".png";
    }

    private static String textureReference(int tier, String name) {
        return "frontier_protocol:block/tier_" + tier + "_stabilizer_" + name;
    }

    private static String top(JsonObject model) {
        return model.getAsJsonObject("textures").get("top").getAsString();
    }

    private static List<Integer> ints(JsonObject object, String field) {
        return object.getAsJsonArray(field).asList().stream().map(value -> value.getAsInt()).toList();
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    private static void assertEntry(JarFile jar, String path) {
        var entry = jar.getJarEntry(path);
        assertNotNull(entry, "production JAR missing " + path);
        assertTrue(entry.getSize() > 0, "production JAR contains empty " + path);
    }
}
