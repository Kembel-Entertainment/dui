package gg.kembel.dui.pack;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.world.*;
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

class WorldMapPackTest {
  @TempDir Path directory;

  private WorldMapDefinition map(String id, int size) {
    return new WorldMapDefinition(
        id,
        size,
        size / 2,
        5,
        100,
        10,
        2,
        10,
        List.of(
            new WorldMapDefinition.Layer(
                "photo",
                "photo",
                size / 2.0,
                size / 4.0,
                32,
                16,
                WorldMapDefinition.Space.MAP,
                .9,
                1,
                1,
                0)),
        new WorldMapDefinition.Opening(12, .9, 0, 1, gg.kembel.dui.core.Motion.Easing.LINEAR));
  }

  private VanillaAssets vanilla() throws Exception {
    var image = new BufferedImage(16, 8, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < 7; y++)
      for (int x = 0; x < 5; x++) {
        image.setRGB(x, y, -1);
        image.setRGB(x + 8, y, -1);
      }
    var png = new ByteArrayOutputStream();
    ImageIO.write(image, "png", png);
    var files =
        Map.of(
            "assets/minecraft/font/default.json",
            "{\"providers\":[{\"type\":\"bitmap\",\"file\":\"minecraft:font/ascii.png\",\"ascent\":7,\"chars\":[\"A?\"]}]}"
                .getBytes(StandardCharsets.UTF_8),
            "assets/minecraft/textures/font/ascii.png",
            png.toByteArray());
    var path = directory.resolve("synthetic.jar");
    try (var zip = new ZipOutputStream(Files.newOutputStream(path))) {
      for (var e : files.entrySet()) {
        zip.putNextEntry(new ZipEntry(e.getKey()));
        zip.write(e.getValue());
        zip.closeEntry();
      }
    }
    return new VanillaAssets(
        path,
        HexFormat.of()
            .formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(path))));
  }

  @Test
  void videoOnlyPackKeepsHudAssetsAndMetricsWithoutInventingAMap() throws Exception {
    try (var vanilla = vanilla()) {
      var result = WorldMapPack.prepare(vanilla, Map.of(), List.of());
      assertFalse(result.enabled());
      assertTrue(result.maps().isEmpty());
      assertDoesNotThrow(() -> new gg.kembel.dui.core.GlyphFont(result.legendMetrics()));
      assertTrue(result.generated().containsKey("assets/dui/font/world-hud/y_0.json"));
      assertTrue(result.generated().containsKey("assets/dui/shaders/include/world-map-hud.glsl"));
      var metadata = PackGenerator.metadata(
          Set.of(gg.kembel.dui.core.video.MapVideoCodec.CAPABILITY), "a".repeat(40),
          Set.of(), Map.of("?", 6), result, Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
      assertEquals(result.legendMetrics(), metadata.get("worldMapLegendMetrics"));
      assertFalse(metadata.containsKey("worldMaps"));
    }
  }

  @Test
  void twoDifferentMapsHaveSeparateGeometryStampsAndExactAdvances() throws Exception {
    var a = map("example:first", 600);
    var b = map("example:second", 960);
    var image = new BufferedImage(32, 16, BufferedImage.TYPE_INT_ARGB);
    var ca = WorldMapPack.contribution("example", a, Map.of("photo", image));
    var cb = WorldMapPack.contribution("example", b, Map.of("photo", image));
    var assets = new TreeMap<String, byte[]>();
    PackContribution.merge(List.of(ca, cb), assets);
    try (var vanilla = vanilla()) {
      var small = new BufferedImage(9, 9, BufferedImage.TYPE_INT_ARGB);
      small.setRGB(4, 4, 0xFFFFFFFF);
      var png = new ByteArrayOutputStream();
      ImageIO.write(small, "png", png);
      var g1 =
          new PackContribution.Glyph(
              new gg.kembel.dui.core.GlyphSpec("example:one", 9, 9), png.toByteArray());
      var g2 =
          new PackContribution.Glyph(
              new gg.kembel.dui.core.GlyphSpec("example:two", 9, 9, false), png.toByteArray());
      var binding =
          gg.kembel.dui.core.GlyphRegistry.bind(List.of(g1.specification(), g2.specification()));
      var first = WorldMapPack.prepare(vanilla, assets, List.of(b, a), List.of(g2, g1), binding);
      var second = WorldMapPack.prepare(vanilla, assets, List.of(a, b), List.of(g1, g2), binding);
      var legend0 =
          com.google.gson.JsonParser.parseString(
                  new String(
                      first.generated().get("assets/dui/font/world-hud/y_0.json"),
                      StandardCharsets.UTF_8))
              .getAsJsonObject();
      var legend360 =
          com.google.gson.JsonParser.parseString(
                  new String(
                      first.generated().get("assets/dui/font/world-hud/y_360.json"),
                      StandardCharsets.UTF_8))
              .getAsJsonObject();
      for (var entry : legend0.getAsJsonArray("providers")) {
        var provider = entry.getAsJsonObject();
        if (provider.has("height"))
          assertTrue(provider.get("ascent").getAsInt() <= provider.get("height").getAsInt());
      }
      assertEquals(
          legend0.getAsJsonArray("providers").size(), legend360.getAsJsonArray("providers").size());
      assertTrue(
          legend0.getAsJsonArray("providers").size() <= 20,
          "HUD fonts must share atlases instead of allocating a provider per glyph per row");
      var bottomMask =
          ImageIO.read(
              new ByteArrayInputStream(
                  first.generated().get("assets/dui/textures/world-hud/glyph_" + 0xE085 + ".png")));
      var topMask =
          ImageIO.read(
              new ByteArrayInputStream(
                  first.generated().get("assets/dui/textures/world-hud/glyph_" + 0xE086 + ".png")));
      assertEquals(0, (bottomMask.getRGB(0, 0) >> 8) & 255);
      assertEquals(1, (topMask.getRGB(0, 0) >> 8) & 255);
      assertEquals(213, (bottomMask.getRGB(0, 0) >> 16) & 255);
      assertEquals(first.maps(), second.maps());
      assertEquals(first.geometry(), second.geometry());
      assertTrue(first.geometry().contains("vec2(600,300)"));
      assertTrue(first.geometry().contains("vec2(960,480)"));
      assertEquals(35, first.maps().get(a.id()).glyphs().get("photo").advance());
      for (var e : first.generated().entrySet())
        assertArrayEquals(e.getValue(), second.generated().get(e.getKey()));
      var stamped =
          ImageIO.read(
              new ByteArrayInputStream(
                  first
                      .generated()
                      .get("assets/example/textures/world-map/second/stamped/photo.png")));
      assertEquals(130, (stamped.getRGB(0, 0) >> 8) & 255);
      assertEquals(131, (stamped.getRGB(33, 0) >> 8) & 255);
      var shader = new ByteArrayOutputStream();
      try (var zip = new ZipOutputStream(shader)) {
        FocusGuard.write(zip, true);
      }
      String vsh = "";
      try (var zip = new ZipInputStream(new ByteArrayInputStream(shader.toByteArray()))) {
        for (var e = zip.getNextEntry(); e != null; e = zip.getNextEntry())
          if (e.getName().endsWith("text.vsh"))
            vsh = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
      }
      assertTrue(vsh.contains("if(duiWorldScene()) return;"));
      assertTrue(vsh.contains("if(duiWorldHud()) return;"));
      assertTrue(vsh.contains("focusGuard=1"));
      assertTrue(vsh.contains("playerFlags=int(playerCode&255u)"));
      assertTrue(vsh.indexOf("playerFlags=-1") < vsh.indexOf("if(duiWorldHud())"));
      assertThrows(
          IllegalArgumentException.class,
          () -> WorldMapPack.prepare(vanilla, assets, List.of(a, a)));
    }
  }

  @Test
  void tilingDoesNotRequireTheOriginalMapSizeAndOversizeImagesFail() throws Exception {
    var atlas = new BufferedImage(320, 192, BufferedImage.TYPE_INT_ARGB);
    var tiles = WorldMapPack.tiles(atlas, 160, 96);
    assertEquals(4, tiles.layers().size());
    assertEquals(240, tiles.layers().get(3).x());
    assertEquals(144, tiles.layers().get(3).y());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            WorldMapPack.contribution(
                "example", map("example:map", 600), Map.of("photo", new BufferedImage(256, 1, 2))));
  }
}
