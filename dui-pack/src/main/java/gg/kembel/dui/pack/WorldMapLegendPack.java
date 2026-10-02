package gg.kembel.dui.pack;

import com.google.gson.*;
import gg.kembel.dui.core.GlyphAtlas;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;

/** Shared stamped paints/text/icons. All screen geometry is supplied by runtime templates. */
final class WorldMapLegendPack {
  private record Glyph(int cp, String file, int height) {}

  static Map<String, Integer> generate(
      VanillaAssets vanilla,
      Map<String, byte[]> out,
      List<PackContribution.Glyph> contributions,
      Map<String, gg.kembel.dui.core.GlyphBinding> bindings)
      throws IOException {
    var glyphs = new ArrayList<Glyph>();
    var metrics = new TreeMap<String, Integer>();
    var root =
        JsonParser.parseString(new String(vanilla.fontDefinition(), StandardCharsets.UTF_8))
            .getAsJsonObject();
    var ascii =
        root.getAsJsonArray("providers").asList().stream()
            .map(JsonElement::getAsJsonObject)
            .filter(
                p ->
                    p.has("file") && p.get("file").getAsString().equals("minecraft:font/ascii.png"))
            .findFirst()
            .orElseThrow();
    var sheet =
        ImageIO.read(
            new ByteArrayInputStream(vanilla.read("assets/minecraft/textures/font/ascii.png")));
    var rows = ascii.getAsJsonArray("chars");
    int columns = rows.get(0).getAsString().codePointCount(0, rows.get(0).getAsString().length());
    int cw = sheet.getWidth() / columns, ch = sheet.getHeight() / rows.size();
    int height = ascii.has("height") ? ascii.get("height").getAsInt() : 8;
    for (int y = 0; y < rows.size(); y++) {
      int[] cps = rows.get(y).getAsString().codePoints().toArray();
      for (int x = 0; x < cps.length; x++)
        if (cps[x] > 32 && cps[x] < 127) {
          int ink = 0;
          for (int dx = 0; dx < cw; dx++)
            for (int dy = 0; dy < ch; dy++)
              if ((sheet.getRGB(x * cw + dx, y * ch + dy) >>> 24) != 0) ink = Math.max(ink, dx + 1);
          ink = Math.max(1, Math.round(ink * (float) height / ch));
          var image = new BufferedImage(ink, height, BufferedImage.TYPE_INT_ARGB);
          var g = image.createGraphics();
          g.drawImage(
              sheet,
              0,
              0,
              ink,
              height,
              x * cw,
              y * ch,
              x * cw + Math.round(ink * (float) ch / height),
              y * ch + ch,
              null);
          g.dispose();
          add(out, glyphs, cps[x], image);
          metrics.put(Character.toString(cps[x]), ink + 1);
        }
    }
    for (var contribution :
        contributions.stream().sorted(Comparator.comparing(g -> g.specification().id())).toList()) {
      var binding =
          gg.kembel.dui.core.GlyphRegistry.require(bindings, contribution.specification().id());
      add(
          out,
          glyphs,
          binding.codePoint(),
          ImageIO.read(new ByteArrayInputStream(contribution.png())));
      metrics.put(Character.toString(binding.codePoint()), binding.advance());
    }
    for (int h = 1; h <= 9; h++)
      for (int bit = 0; bit <= 7; bit++) {
        var image = new BufferedImage(1 << bit, 9, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < image.getWidth(); x++)
          for (int y = 0; y < h; y++) image.setRGB(x, y, -1);
        add(out, glyphs, GlyphAtlas.rectangle(bit, h), image);
      }
    var mask = new BufferedImage(1, 9, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < 9; y++) mask.setRGB(0, y, -1);
    add(out, glyphs, 0xE085, mask);
    // The mask has its own stamp; its bounds are the native HUD's GUI footprint.
    var bottomMask = stamp(mask, 213);
    for (int y = 0; y < 2; y++)
      for (int x = 0; x < 2; x++)
        bottomMask.setRGB(
            x * (bottomMask.getWidth() - 1),
            y * (bottomMask.getHeight() - 1),
            ((128 + x + 2 * y) << 24) | (213 << 16) | 9);
    out.put("assets/dui/textures/world-hud/glyph_" + 0xE085 + ".png", WorldMapPack.png(bottomMask));
    add(out, glyphs, 0xE086, mask);
    var barMask = stamp(mask, 213);
    for (int y = 0; y < 2; y++)
      for (int x = 0; x < 2; x++)
        barMask.setRGB(
            x * (barMask.getWidth() - 1),
            y * (barMask.getHeight() - 1),
            ((128 + x + 2 * y) << 24) | (213 << 16) | (1 << 8) | 9);
    out.put("assets/dui/textures/world-hud/glyph_" + 0xE086 + ".png", WorldMapPack.png(barMask));
    metrics.put(" ", 4);
    var advances = new JsonObject();
    advances.addProperty(" ", 4);
    for (int bit = 0; bit <= 22; bit++) {
      advances.addProperty(Character.toString(0xE800 + bit), 1 << bit);
      advances.addProperty(Character.toString(0xE900 + bit), -(1 << bit));
    }
    var baseProviders = new JsonArray();
    baseProviders.add(WorldMapPack.JSON.toJsonTree(Map.of("type", "space", "advances", advances)));
    for (var glyph : glyphs)
      baseProviders.add(
          WorldMapPack.JSON.toJsonTree(
              Map.of(
                  "type",
                  "bitmap",
                  "file",
                  glyph.file(),
                  "height",
                  glyph.height(),
                  "ascent",
                  8,
                  "chars",
                  List.of(Character.toString(glyph.cp())))));
    baseProviders = BitmapFontAtlas.compact(baseProviders, out, "dui:world-hud/atlas");
    for (int y = 0; y <= gg.kembel.dui.core.world.WorldMapProtocol.HUD_MAX_OFFSET_Y; y++) {
      var providers = baseProviders.deepCopy();
      for (var entry : providers) {
        var provider = entry.getAsJsonObject();
        if (provider.get("type").getAsString().equals("bitmap"))
          provider.addProperty("ascent", 8 - y);
      }
      out.put(
          "assets/dui/font/world-hud/y_" + y + ".json",
          WorldMapPack.encode(Map.of("providers", providers)).getBytes(StandardCharsets.UTF_8));
    }
    out.put(
        "assets/dui/font/world-map-legend.json",
        WorldMapPack.encode(
                Map.of(
                    "providers", List.of(Map.of("type", "reference", "id", "dui:world-hud/y_0"))))
            .getBytes(StandardCharsets.UTF_8));
    return Collections.unmodifiableMap(metrics);
  }

  private static void add(Map<String, byte[]> out, List<Glyph> glyphs, int cp, BufferedImage image)
      throws IOException {
    var padded = stamp(image, 212);
    String file = "dui:world-hud/glyph_" + cp + ".png";
    out.put("assets/dui/textures/world-hud/glyph_" + cp + ".png", WorldMapPack.png(padded));
    glyphs.add(new Glyph(cp, file, padded.getHeight()));
  }

  private static BufferedImage stamp(BufferedImage original, int marker) {
    var result = WorldMapPack.stamp(original, 0);
    for (int y = 0; y < 2; y++)
      for (int x = 0; x < 2; x++)
        result.setRGB(
            x * (result.getWidth() - 1),
            y * (result.getHeight() - 1),
            ((128 + x + 2 * y) << 24)
                | (marker << 16)
                | (original.getWidth() << 8)
                | original.getHeight());
    return result;
  }
}
