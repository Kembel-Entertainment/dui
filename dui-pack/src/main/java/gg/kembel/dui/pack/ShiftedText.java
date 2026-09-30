package gg.kembel.dui.pack;

import com.google.gson.*;
import gg.kembel.dui.core.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;

/** Clip shifted vanilla glyphs to text rows so the next row's background cannot cover their ink. */
final class ShiftedText {
  private record Glyph(int codepoint, int advance, int ascent, BufferedImage image) {}

  private ShiftedText() {}

  static void write(ZipOutputStream zip, VanillaAssets assets) throws IOException {
    JsonObject definition, metrics;
    definition =
        JsonParser.parseString(new String(assets.fontDefinition(), StandardCharsets.UTF_8))
            .getAsJsonObject();
    metrics = new Gson().toJsonTree(assets.metrics()).getAsJsonObject();
    var glyphs = new LinkedHashMap<Integer, Glyph>();
    var spaces = new JsonArray();
    for (var entry : definition.getAsJsonArray("providers")) {
      var provider = entry.getAsJsonObject();
      if (provider.get("type").getAsString().equals("space")) {
        spaces.add(provider.deepCopy());
        continue;
      }
      String name = provider.get("file").getAsString();
      name = name.substring(name.lastIndexOf('/') + 1);
      BufferedImage atlas;
      try (var in =
          new ByteArrayInputStream(assets.read("assets/minecraft/textures/font/" + name))) {
        atlas = ImageIO.read(in);
      }
      var rows = provider.getAsJsonArray("chars");
      int columns = rows.get(0).getAsString().codePointCount(0, rows.get(0).getAsString().length());
      int cellWidth = atlas.getWidth() / columns,
          cellHeight = atlas.getHeight() / rows.size(),
          height = provider.has("height") ? provider.get("height").getAsInt() : 8;
      if (cellHeight != height || cellWidth > 16)
        throw new IllegalArgumentException("Unsupported source font dimensions: " + name);
      for (int row = 0; row < rows.size(); row++) {
        int[] chars = rows.get(row).getAsString().codePoints().toArray();
        for (int col = 0; col < chars.length; col++) {
          int cp = chars[col];
          String character = new String(Character.toChars(cp));
          if (cp == 0 || glyphs.containsKey(cp) || !metrics.has(character)) continue;
          int advance = metrics.get(character).getAsInt();
          if (advance < 2) continue;
          glyphs.put(
              cp,
              new Glyph(
                  cp,
                  advance,
                  provider.get("ascent").getAsInt(),
                  atlas.getSubimage(col * cellWidth, row * cellHeight, cellWidth, cellHeight)));
        }
      }
    }
    var list = new ArrayList<>(glyphs.values());
    int columns = 16, rows = (list.size() + columns - 1) / columns;
    var chars = new JsonArray();
    for (int row = 0; row < rows; row++) {
      var line = new StringBuilder();
      for (int col = 0; col < columns; col++) {
        int index = row * columns + col;
        line.appendCodePoint(index < list.size() ? list.get(index).codepoint : 0);
      }
      chars.add(line.toString());
    }
    for (int offset = 1; offset < 9; offset++)
      for (int band = 0; band < 2; band++) {
        var image = new BufferedImage(columns * 16, rows * 9, BufferedImage.TYPE_INT_ARGB);
        for (int index = 0; index < list.size(); index++) {
          var glyph = list.get(index);
          int x0 = index % columns * 16, y0 = index / columns * 9;
          for (int sy = 0; sy < glyph.image.getHeight(); sy++) {
            int dy = offset + 7 - glyph.ascent + sy - band * 9;
            if (dy < 0 || dy >= 9) continue;
            for (int sx = 0; sx < glyph.image.getWidth(); sx++)
              image.setRGB(x0 + sx, y0 + dy, glyph.image.getRGB(sx, sy));
          }
          // Invisible edge pixel preserves the original advance, even for an empty lower band.
          int markerX = x0 + glyph.advance - 2, markerY = y0 + 8;
          if ((image.getRGB(markerX, markerY) >>> 24) == 0)
            image.setRGB(markerX, markerY, 0x01FFFFFF);
        }
        String name = "text_" + offset + "_" + band;
        var png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        put(zip, "assets/dui/textures/font/" + name + ".png", png.toByteArray());
        var providers = spaces.deepCopy();
        var bitmap = new JsonObject();
        bitmap.addProperty("type", "bitmap");
        bitmap.addProperty("file", "dui:font/" + name + ".png");
        bitmap.addProperty("height", 9);
        bitmap.addProperty("ascent", 7);
        bitmap.add("chars", chars);
        providers.add(bitmap);
        var font = new JsonObject();
        font.add("providers", providers);
        put(
            zip,
            "assets/dui/font/" + name + ".json",
            font.toString().getBytes(StandardCharsets.UTF_8));
      }
  }

  private static void put(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
    var entry = new ZipEntry(name);
    entry.setTime(0);
    zip.putNextEntry(entry);
    zip.write(bytes);
    zip.closeEntry();
  }
}
