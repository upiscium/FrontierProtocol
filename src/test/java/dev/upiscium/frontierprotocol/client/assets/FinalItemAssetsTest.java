package dev.upiscium.frontierprotocol.client.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class FinalItemAssetsTest {
    private static final String ASSET_ROOT = "assets/frontier_protocol/";
    private static final List<String> ITEMS = List.of("stabilization_compound", "stabilization_cell");

    @Test
    void finalTexturesAreDistinctNonBlankRgbaImages() throws Exception {
        BufferedImage compound = texture(ITEMS.get(0));
        BufferedImage cell = texture(ITEMS.get(1));

        assertFalse(
                Arrays.equals(pixels(compound), pixels(cell)),
                "Compound and Cell textures must remain visually distinct");
    }

    @Test
    void finalModelsUseTheirCustomTextures() throws Exception {
        for (String item : ITEMS) {
            JsonObject model = model(item);
            assertEquals("minecraft:item/generated", model.get("parent").getAsString(), item + " model parent");
            assertEquals(
                    "frontier_protocol:item/" + item,
                    model.getAsJsonObject("textures").get("layer0").getAsString(),
                    item + " texture reference");
            String json = model.toString();
            assertFalse(json.contains("minecraft:item/blaze_powder"), "obsolete Compound placeholder");
            assertFalse(json.contains("minecraft:item/prismarine_crystals"), "obsolete Cell placeholder");
        }
    }

    @Test
    void finalAssetsArePackagedInProductionJar() throws Exception {
        String jarPath = System.getProperty("frontierProtocol.testJar");
        assertNotNull(jarPath, "production JAR path was not supplied by Gradle");

        try (JarFile jar = new JarFile(Path.of(jarPath).toFile())) {
            for (String item : ITEMS) {
                assertJarEntry(jar, ASSET_ROOT + "models/item/" + item + ".json");
                assertJarEntry(jar, ASSET_ROOT + "textures/item/" + item + ".png");
            }
        }
    }

    private BufferedImage texture(String item) throws Exception {
        String path = ASSET_ROOT + "textures/item/" + item + ".png";
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(resource, "missing texture " + path);
            BufferedImage image = ImageIO.read(resource);
            assertNotNull(image, "unreadable texture " + path);
            assertEquals(32, image.getWidth(), item + " width");
            assertEquals(32, image.getHeight(), item + " height");
            assertTrue(image.getColorModel().hasAlpha(), item + " requires an alpha channel");

            int transparentPixels = 0;
            int visiblePixels = 0;
            Set<Integer> visibleColors = new HashSet<>();
            for (int pixel : pixels(image)) {
                int alpha = pixel >>> 24;
                if (alpha == 0) {
                    transparentPixels++;
                } else {
                    visiblePixels++;
                    visibleColors.add(pixel);
                }
            }

            assertTrue(transparentPixels > 0, item + " needs transparent background pixels");
            assertTrue(visiblePixels >= 32, item + " is blank or too sparse");
            assertTrue(visibleColors.size() >= 4, item + " is a single-color placeholder");
            assertNotEquals(0, visiblePixels, item + " is fully transparent");
            return image;
        }
    }

    private JsonObject model(String item) throws Exception {
        String path = ASSET_ROOT + "models/item/" + item + ".json";
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(resource, "missing model " + path);
            try (var reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    private static void assertJarEntry(JarFile jar, String path) {
        var entry = jar.getJarEntry(path);
        assertNotNull(entry, "production JAR missing " + path);
        assertTrue(entry.getSize() > 0, "production JAR contains empty " + path);
    }
}
