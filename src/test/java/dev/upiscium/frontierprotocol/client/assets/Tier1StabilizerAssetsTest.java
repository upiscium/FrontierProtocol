package dev.upiscium.frontierprotocol.client.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.Set;
import java.util.jar.JarFile;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class Tier1StabilizerAssetsTest {
    private static final String ROOT = "assets/frontier_protocol/";
    private static final List<String> MODELS = List.of(
            "tier_1_stabilizer_base",
            "tier_1_stabilizer_offline",
            "tier_1_stabilizer_active",
            "tier_1_stabilizer_grace_period");
    private static final List<String> REMOVED_DYNAMIC_MODELS = List.of(
            "tier_1_stabilizer_core",
            "tier_1_stabilizer_gear",
            "tier_1_stabilizer_light_overlay");
    private static final List<String> TEXTURES = List.of(
            "front", "back", "side", "top", "bottom",
            "front_offline", "side_offline", "top_offline",
            "front_active", "side_active", "top_active",
            "front_grace", "side_grace", "top_grace",
            "casing", "armor", "metal", "plate", "warning", "window",
            "core", "gear", "light_mask", "ring_offline", "ring_active", "ring_grace");

    @Test
    void blockstateDefinesEveryFacingAndStatusCombination() throws Exception {
        JsonObject variants = json(ROOT + "blockstates/tier_1_stabilizer.json").getAsJsonObject("variants");
        assertEquals(12, variants.size());
        for (String facing : List.of("north", "east", "south", "west")) {
            for (String status : List.of("offline", "active", "grace_period")) {
                assertTrue(variants.has("facing=" + facing + ",status=" + status));
            }
        }
    }

    @Test
    void finalModelsExistWithoutVanillaPlaceholderReferences() throws Exception {
        String forbidden = String.join("|", List.of(
                "minecraft:block/iron_block",
                "minecraft:block/copper_block",
                "minecraft:block/exposed_copper",
                "minecraft:block/redstone_block",
                "minecraft:block/deepslate_tiles",
                "minecraft:block/copper_grate"));
        for (String model : MODELS) {
            String text = resourceText(ROOT + "models/block/" + model + ".json");
            for (String reference : forbidden.split("\\|")) assertFalse(text.contains(reference), model + " retained " + reference);
        }
        assertEquals(
                "frontier_protocol:block/tier_1_stabilizer_offline",
                json(ROOT + "models/item/tier_1_stabilizer.json").get("parent").getAsString());
    }

    @Test
    void baseModelDefinesOpenFrameAndRearBearingContract() throws Exception {
        JsonObject base = json(ROOT + "models/block/tier_1_stabilizer_base.json");
        JsonObject textures = base.getAsJsonObject("textures");
        for (String texture : List.of("front", "back", "side", "top", "bottom", "metal")) {
            assertTrue(textures.has(texture), "missing texture slot " + texture);
        }

        boolean oldSolidCore = false;
        boolean rearBearingReachesBoundary = false;
        for (var element : base.getAsJsonArray("elements")) {
            var object = element.getAsJsonObject();
            var from = object.getAsJsonArray("from");
            var to = object.getAsJsonArray("to");
            oldSolidCore |= from.toString().equals("[2,2,2]") && to.toString().equals("[14,14,14]");
            rearBearingReachesBoundary |= from.get(2).getAsFloat() >= 14.0F && to.get(2).getAsFloat() == 16.0F;
        }
        assertFalse(oldSolidCore, "central solid cuboid would block the four core windows");
        assertTrue(rearBearingReachesBoundary, "rear bearing must reach the shaft connection plane");
        for (String model : REMOVED_DYNAMIC_MODELS) {
            assertNull(loader().getResource(ROOT + "models/block/" + model + ".json"));
        }
    }

    @Test
    void everyFinalTextureIsNonBlank32PixelRgba() throws Exception {
        for (String texture : TEXTURES) {
            String path = ROOT + "textures/block/tier_1_stabilizer_" + texture + ".png";
            try (InputStream resource = loader().getResourceAsStream(path)) {
                assertNotNull(resource, "missing " + path);
                BufferedImage image = ImageIO.read(resource);
                assertNotNull(image, "unreadable " + path);
                assertEquals(32, image.getWidth());
                assertEquals(32, image.getHeight());
                assertTrue(image.getColorModel().hasAlpha(), path + " requires alpha");
                int transparent = 0;
                int visible = 0;
                Set<Integer> colors = new HashSet<>();
                for (int pixel : image.getRGB(0, 0, 32, 32, null, 0, 32)) {
                    if ((pixel >>> 24) == 0) transparent++;
                    else {
                        visible++;
                        colors.add(pixel & 0x00FF_FFFF);
                    }
                }
                if (Set.of("core", "gear", "light_mask").contains(texture)) {
                    assertTrue(transparent > 0, path + " needs transparent background");
                }
                assertTrue(visible >= 32, path + " is blank or sparse");
                assertTrue(colors.size() >= (texture.equals("light_mask") ? 1 : 3), path + " lacks color detail");
            }
        }
    }

    @Test
    void productionJarContainsAllFinalAssets() throws Exception {
        try (JarFile jar = new JarFile(Path.of(System.getProperty("frontierProtocol.testJar")).toFile())) {
            assertNotNull(jar.getJarEntry(ROOT + "blockstates/tier_1_stabilizer.json"));
            assertNotNull(jar.getJarEntry(ROOT + "models/item/tier_1_stabilizer.json"));
            for (String model : MODELS) assertNotNull(jar.getJarEntry(ROOT + "models/block/" + model + ".json"));
            for (String model : REMOVED_DYNAMIC_MODELS) {
                assertNull(jar.getJarEntry(ROOT + "models/block/" + model + ".json"));
            }
            for (String texture : TEXTURES) {
                assertNotNull(jar.getJarEntry(ROOT + "textures/block/tier_1_stabilizer_" + texture + ".png"));
            }
        }
    }

    private JsonObject json(String path) throws Exception {
        try (InputStream resource = loader().getResourceAsStream(path)) {
            assertNotNull(resource, "missing " + path);
            return JsonParser.parseReader(new InputStreamReader(resource, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private String resourceText(String path) throws Exception {
        try (InputStream resource = loader().getResourceAsStream(path)) {
            assertNotNull(resource, "missing " + path);
            return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private ClassLoader loader() {
        return getClass().getClassLoader();
    }
}
