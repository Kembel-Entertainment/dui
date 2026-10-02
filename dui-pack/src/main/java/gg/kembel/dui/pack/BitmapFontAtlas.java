package gg.kembel.dui.pack;

import com.google.gson.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Packs equal-sized cells together to avoid a client-side Unicode table per glyph. */
final class BitmapFontAtlas {
  private record Cell(JsonObject provider, int codepoint, BufferedImage image) {}

  private record Shape(int width, int pixelsHigh, int height, int ascent) {}

  static JsonArray compact(JsonArray providers, Map<String, byte[]> resources, String prefix)
      throws IOException {
    var groups = new LinkedHashMap<Shape, List<Cell>>();
    var retained = new JsonArray();
    for (var entry : providers) {
      var provider = entry.getAsJsonObject();
      if (!provider.get("type").getAsString().equals("bitmap")) {
        retained.add(provider.deepCopy());
        continue;
      }
      var chars = provider.getAsJsonArray("chars");
      if (chars.size() != 1
          || chars.get(0).getAsString().codePointCount(0, chars.get(0).getAsString().length())
              != 1) {
        retained.add(provider.deepCopy());
        continue;
      }
      var file = provider.get("file").getAsString().split(":", 2);
      var data = resources.get("assets/" + file[0] + "/textures/" + file[1]);
      if (data == null) {
        retained.add(provider.deepCopy());
        continue;
      }
      var image = ImageIO.read(new ByteArrayInputStream(data));
      if (image == null) throw new IOException("Invalid font image: " + provider.get("file"));
      var shape =
          new Shape(
              image.getWidth(),
              image.getHeight(),
              provider.has("height") ? provider.get("height").getAsInt() : 8,
              provider.get("ascent").getAsInt());
      groups
          .computeIfAbsent(shape, unused -> new ArrayList<>())
          .add(new Cell(provider, chars.get(0).getAsString().codePointAt(0), image));
    }
    int index = 0;
    for (var entry : groups.entrySet()) {
      var shape = entry.getKey();
      var cells = entry.getValue();
      if (cells.size() == 1) {
        retained.add(cells.getFirst().provider().deepCopy());
        continue;
      }
      int columns = Math.min(cells.size(), Math.max(1, 1024 / shape.width()));
      int rows = (cells.size() + columns - 1) / columns;
      var sheet =
          new BufferedImage(
              columns * shape.width(), rows * shape.pixelsHigh(), BufferedImage.TYPE_INT_ARGB);
      var chars = new JsonArray();
      for (int row = 0; row < rows; row++) {
        var text = new StringBuilder();
        for (int column = 0; column < columns; column++) {
          int cellIndex = row * columns + column;
          if (cellIndex >= cells.size()) {
            text.append('\0');
            continue;
          }
          var cell = cells.get(cellIndex);
          text.appendCodePoint(cell.codepoint());
          // Preserve low-alpha shader stamps exactly; Graphics2D blending can round them.
          var pixels =
              cell.image().getRGB(0, 0, shape.width(), shape.pixelsHigh(), null, 0, shape.width());
          sheet.setRGB(
              column * shape.width(),
              row * shape.pixelsHigh(),
              shape.width(),
              shape.pixelsHigh(),
              pixels,
              0,
              shape.width());
        }
        chars.add(text.toString());
      }
      String name = prefix + "_" + index++ + ".png";
      var destination = name.split(":", 2);
      var bytes = new ByteArrayOutputStream();
      ImageIO.write(sheet, "png", bytes);
      resources.put(
          "assets/" + destination[0] + "/textures/" + destination[1], bytes.toByteArray());
      var provider = cells.getFirst().provider().deepCopy();
      provider.addProperty("file", name);
      provider.add("chars", chars);
      retained.add(provider);
    }
    return retained;
  }

  private BitmapFontAtlas() {}
}
