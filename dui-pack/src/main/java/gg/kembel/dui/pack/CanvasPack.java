package gg.kembel.dui.pack;

import com.google.gson.*;
import gg.kembel.dui.core.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;

/** A tiny reusable rectangle/icon atlas. Everything else is layout, text and server state. */
public final class CanvasPack {
  public static final List<String> ICONS =
      List.of(
          "settings",
          "grid",
          "diamond",
          "star",
          "users",
          "book",
          "lock",
          "check",
          "coin",
          "leaf",
          "sun",
          "moon",
          "previous",
          "next");
  private static final String[] PIXELS = {
    "...###.../..#####../.##.#.##./##..#..##/#########/##..#..##/.##.#.##./..#####../...###...",
    "####.####/####.####/####.####/####.####/........./####.####/####.####/####.####/####.####",
    "...###.../..#####../.#######./#########/.#######./..#####../...###.../....#..../.........",
    "....#..../...###.../#########/..#####../...###.../..#####../.###.###./.##...##./.........",
    "..###..../..###..../..###.##./......##./.#####.../#######../#######../#######../.........",
    "########./##....###/##....#.#/##....#.#/##....#.#/##....#.#/##....#.#/#########/..#######",
    "..#####../.##...##./.##...##./.##...##./#########/####.####/####.####/#########/#########",
    "........./.......##/......##./.....##../##..##.../.####..../..##...../........./.........",
    "..#####../.#######./###...###/###.#####/###...###/#####.###/###...###/.#######./..#####..",
    ".......##/....#####/..#######/.#######./.###.###./.##.###../..####.../.##....../##.......",
    "....#..../.#.....#./...###.../..#####../#.#####.#/..#####../...###.../.#.....#./....#....",
    "...###.../..###..../.###...../.###...../.###...../.####..../..#####../...#####./....###..",
    "......#../.....#.../....#..../...#...../..#....../...#...../....#..../.....#.../......#..",
    "..#....../...#...../....#..../.....#.../......#../.....#.../....#..../...#...../..#......"
  };

  public static char rectangle(int bit, int height) {
    return (char) (0xEA00 + (height - 1) * 9 + bit);
  }

  public static char icon(String name) {
    int index = ICONS.indexOf(name);
    if (index < 0) throw new IllegalArgumentException("Unknown icon: " + name);
    return (char) (0xEB00 + index);
  }

  public static char icon(String name, int band) {
    return (char) (icon(name) + (band == 0 ? 0 : 0x40));
  }

  public static byte[] build(VanillaAssets assets, Map<String, byte[]> additions)
      throws IOException {
    return build(assets, additions, List.of());
  }

  public static byte[] build(
      VanillaAssets assets, Map<String, byte[]> additions, List<PackContribution.Effect> effects)
      throws IOException {
    var bytes = new ByteArrayOutputStream();
    try (var zip = new ZipOutputStream(bytes)) {
      put(
          zip,
          "pack.mcmeta",
          "{\"pack\":{\"description\":\"dui / Vanilla UI\",\"min_format\":88,\"max_format\":88}}"
              .getBytes(StandardCharsets.UTF_8));
      put(zip, "assets/dui/font/text.json", assets.fontDefinition());
      for (int h = 1; h <= 9; h++)
        for (int bit = 0; bit <= 8; bit++) {
          var image = new BufferedImage(1 << bit, Math.max(8, h), BufferedImage.TYPE_INT_ARGB);
          for (int x = 0; x < image.getWidth(); x++)
            for (int y = 0; y < h; y++) image.setRGB(x, y, 0xFFFFFFFF);
          png(zip, "rect_" + bit + "_" + h, image);
        }
      for (int i = 0; i < ICONS.size(); i++) {
        var image = new BufferedImage(9, 9, BufferedImage.TYPE_INT_ARGB);
        String[] rows = PIXELS[i].split("/");
        for (int y = 0; y < 9; y++)
          for (int x = 0; x < 9; x++) if (rows[y].charAt(x) == '#') image.setRGB(x, y, 0xFFFFFFFF);
        image.setRGB(8, 8, 0x01FFFFFF); // Fixed 10 px advance; effectively transparent last pixel.
        png(zip, "icon_" + ICONS.get(i), image);
        for (int offset = 1; offset < 9; offset++) {
          var shifted = new BufferedImage(9, 18, BufferedImage.TYPE_INT_ARGB);
          for (int y = 0; y < 9; y++)
            for (int x = 0; x < 9; x++) shifted.setRGB(x, y + offset, image.getRGB(x, y));
          for (int y : List.of(8, 17))
            if ((shifted.getRGB(8, y) >>> 24) == 0) shifted.setRGB(8, y, 0x01FFFFFF);
          png(zip, "icon_" + ICONS.get(i) + "_" + offset, shifted);
        }
      }
      for (int offset = 0; offset < 9; offset++) {
        JsonArray providers = new JsonArray();
        JsonObject space = new JsonObject(), advances = new JsonObject();
        space.addProperty("type", "space");
        for (int b = 0; b <= 10; b++) {
          advances.addProperty("" + (char) (0xE800 + b), 1 << b);
          advances.addProperty("" + (char) (0xE900 + b), -(1 << b));
        }
        space.add("advances", advances);
        providers.add(space);
        for (int h = 1; h <= 9; h++)
          for (int b = 0; b <= 8; b++)
            providers.add(
                bitmap("rect_" + b + "_" + h, rectangle(b, h), Math.max(8, h), 7 - offset));
        for (String name : ICONS) {
          var provider =
              bitmap("icon_" + name + (offset == 0 ? "" : "_" + offset), icon(name), 9, 7);
          if (offset != 0) provider.getAsJsonArray("chars").add("" + icon(name, 1));
          providers.add(provider);
        }
        JsonObject font = new JsonObject();
        font.add("providers", providers);
        put(
            zip,
            "assets/dui/font/canvas_" + offset + ".json",
            font.toString().getBytes(StandardCharsets.UTF_8));
      }
      put(zip, "assets/dui/font/items.json", ItemGlyphs.font());
      ShiftedText.write(zip, assets);
      ShaderItemPack.write(zip, assets, additions, effects);
      FocusGuard.write(zip);
      for (var entry : new TreeMap<>(additions).entrySet())
        if (!entry.getKey().equals("assets/minecraft/atlases/items.json"))
          put(zip, entry.getKey(), entry.getValue());
    }
    return bytes.toByteArray();
  }

  private static JsonObject bitmap(String name, char glyph, int height, int ascent) {
    JsonObject p = new JsonObject();
    p.addProperty("type", "bitmap");
    p.addProperty("file", "dui:gui/canvas/" + name + ".png");
    p.addProperty("height", height);
    p.addProperty("ascent", ascent);
    JsonArray chars = new JsonArray();
    chars.add("" + glyph);
    p.add("chars", chars);
    return p;
  }

  private static void png(ZipOutputStream zip, String name, BufferedImage image)
      throws IOException {
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(image, "png", bytes);
    put(zip, "assets/dui/textures/gui/canvas/" + name + ".png", bytes.toByteArray());
  }

  private static void put(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
    var e = new ZipEntry(name);
    e.setTime(0);
    zip.putNextEntry(e);
    zip.write(bytes);
    zip.closeEntry();
  }
}
