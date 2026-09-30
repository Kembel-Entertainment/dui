package gg.kembel.dui.pack;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackTest {
  @TempDir Path directory;

  private Path fixture() throws Exception {
    var files = new TreeMap<String, byte[]>();
    files.put(
        "assets/minecraft/font/default.json",
        "{\"providers\":[{\"type\":\"bitmap\",\"file\":\"minecraft:font/ascii.png\",\"ascent\":7,\"chars\":[\"A?\"]}]}"
            .getBytes(StandardCharsets.UTF_8));
    files.put(
        "assets/minecraft/items/paper.json",
        "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"minecraft:item/paper\"}}"
            .getBytes(StandardCharsets.UTF_8));
    var image = new BufferedImage(16, 8, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < 7; y++)
      for (int x = 0; x < 5; x++) {
        image.setRGB(x, y, 0xFFFFFFFF);
        image.setRGB(x + 8, y, 0xFFFFFFFF);
      }
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(image, "png", bytes);
    files.put("assets/minecraft/textures/font/ascii.png", bytes.toByteArray());
    Path jar = directory.resolve("synthetic.jar");
    try (var zip = new ZipOutputStream(Files.newOutputStream(jar))) {
      for (var e : files.entrySet()) {
        zip.putNextEntry(new ZipEntry(e.getKey()));
        zip.write(e.getValue());
        zip.closeEntry();
      }
    }
    return jar;
  }

  private VanillaAssets input(Path jar) throws Exception {
    return new VanillaAssets(
        jar,
        HexFormat.of()
            .formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(jar))));
  }

  @Test
  void buildsDeterministicFontsShadersAndNamespacedItemExtensions() throws Exception {
    Path jar = fixture();
    var own =
        Map.of(
            "assets/example/items/gift.json",
            "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"example:gift\"}}"
                .getBytes(StandardCharsets.UTF_8));
    byte[] first, second;
    try (var assets = input(jar)) {
      first = CanvasPack.build(assets, own);
    }
    try (var assets = input(jar)) {
      second = CanvasPack.build(assets, own);
    }
    assertArrayEquals(first, second);
    var entries = new HashMap<String, byte[]>();
    try (var zip = new ZipInputStream(new ByteArrayInputStream(first))) {
      for (var e = zip.getNextEntry(); e != null; e = zip.getNextEntry())
        assertNull(entries.put(e.getName(), zip.readAllBytes()));
    }
    assertTrue(entries.containsKey("assets/dui/font/text_4_1.json"));
    assertTrue(entries.containsKey("assets/dui/items/live/example/gift.json"));
    assertTrue(entries.containsKey("assets/dui/items/live/minecraft/paper.json"));
    assertTrue(entries.containsKey("assets/minecraft/shaders/core/text.fsh"));
    assertFalse(
        entries.keySet().stream().anyMatch(p -> p.startsWith("assets/minecraft/textures/")));
    var model =
        JsonParser.parseString(
                new String(
                    entries.get("assets/dui/items/live/example/gift.json"), StandardCharsets.UTF_8))
            .getAsJsonObject();
    assertEquals("minecraft:composite", model.getAsJsonObject("model").get("type").getAsString());
  }

  @Test
  void publicAssetInputRejectsAnUnverifiedGameJar() throws Exception {
    assertThrows(IOException.class, () -> new VanillaAssets(fixture()));
  }
}
