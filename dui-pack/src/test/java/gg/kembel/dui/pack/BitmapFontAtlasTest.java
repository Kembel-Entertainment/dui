package gg.kembel.dui.pack;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class BitmapFontAtlasTest {
  private byte[] png(BufferedImage image) throws IOException {
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(image, "png", bytes);
    return bytes.toByteArray();
  }

  private JsonObject provider(String file, int codepoint, int height, int ascent) {
    var result = new JsonObject();
    result.addProperty("type", "bitmap");
    result.addProperty("file", file);
    result.addProperty("height", height);
    result.addProperty("ascent", ascent);
    var chars = new JsonArray();
    chars.add(Character.toString(codepoint));
    result.add("chars", chars);
    return result;
  }

  @Test
  void hundredsOfProvidersBecomeOneWithoutChangingPixelsOrGlyphMetrics() throws Exception {
    var resources = new TreeMap<String, byte[]>();
    var providers = new JsonArray();
    var originals = new HashMap<Integer, BufferedImage>();
    for (int i = 0; i < 155; i++) {
      int cp = 0x10000 + i;
      var image = new BufferedImage(7, 10, BufferedImage.TYPE_INT_ARGB);
      image.setRGB(0, 0, 0x80D40508);
      image.setRGB(6, 0, 0x81D40508);
      image.setRGB(0, 9, 0x82D40508);
      image.setRGB(6, 9, 0x83D40508);
      image.setRGB(2, 2, ((i + 1) << 24) | 0xFEDCBA);
      String file = "test:font/glyph_" + i + ".png";
      resources.put("assets/test/textures/font/glyph_" + i + ".png", png(image));
      providers.add(provider(file, cp, 10, -352));
      originals.put(cp, image);
    }
    String before = providers.toString();
    var compact = BitmapFontAtlas.compact(providers, resources, "test:font/atlas");
    assertEquals(1, compact.size(), "Do not allocate a Unicode table for every single glyph");
    assertEquals(before, providers.toString());
    var p = compact.get(0).getAsJsonObject();
    assertEquals(10, p.get("height").getAsInt());
    assertEquals(-352, p.get("ascent").getAsInt());
    var sheet =
        ImageIO.read(
            new ByteArrayInputStream(resources.get("assets/test/textures/font/atlas_0.png")));
    var rows = p.getAsJsonArray("chars");
    int columns = rows.get(0).getAsString().codePointCount(0, rows.get(0).getAsString().length());
    assertTrue(sheet.getWidth() <= 1024);
    assertEquals(7, sheet.getWidth() / columns);
    assertEquals(10, sheet.getHeight() / rows.size());
    int count = 0;
    for (int y = 0; y < rows.size(); y++) {
      int[] chars = rows.get(y).getAsString().codePoints().toArray();
      assertEquals(columns, chars.length);
      for (int x = 0; x < chars.length; x++) {
        if (chars[x] == 0) {
          assertArrayEquals(new int[70], sheet.getRGB(x * 7, y * 10, 7, 10, null, 0, 7));
          continue;
        }
        var original = originals.get(chars[x]);
        assertNotNull(original);
        assertArrayEquals(
            original.getRGB(0, 0, 7, 10, null, 0, 7),
            sheet.getRGB(x * 7, y * 10, 7, 10, null, 0, 7));
        count++;
      }
    }
    assertEquals(originals.size(), count);
  }

  @Test
  void differentCellDimensionsAndBaselinesStaySeparateAndGenerationIsDeterministic()
      throws Exception {
    var resources = new TreeMap<String, byte[]>();
    var providers = new JsonArray();
    for (int group = 0; group < 3; group++) {
      for (int n = 0; n < 3; n++) {
        int width = group == 0 ? 4 : 8;
        var image = new BufferedImage(width, 9, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(width - 1, 8, 0x01FFFFFF);
        String name = "glyph_" + group + "_" + n + ".png";
        resources.put("assets/test/textures/" + name, png(image));
        providers.add(provider("test:" + name, 0xE000 + group * 3 + n, 9, group == 2 ? 4 : 7));
      }
    }
    var firstResources = new TreeMap<>(resources);
    var secondResources = new TreeMap<>(resources);
    var first = BitmapFontAtlas.compact(providers, firstResources, "test:atlas");
    var second = BitmapFontAtlas.compact(providers, secondResources, "test:atlas");
    assertEquals(3, first.size());
    assertEquals(first, second);
    assertEquals(firstResources.keySet(), secondResources.keySet());
    for (var key : firstResources.keySet())
      assertArrayEquals(firstResources.get(key), secondResources.get(key));
  }

  @Test
  void referencesSpacesAndExistingMultiCellProvidersRemainUnchanged() throws Exception {
    var providers =
        JsonParser.parseString(
                """
                [{"type":"reference","id":"minecraft:default"},
                 {"type":"space","advances":{" ":4}},
                 {"type":"bitmap","file":"test:existing.png","height":9,"ascent":7,"chars":["AB"]},
                 {"type":"bitmap","file":"test:external.png","height":9,"ascent":7,"chars":["C"]}]
                """)
            .getAsJsonArray();
    assertEquals(providers, BitmapFontAtlas.compact(providers, new TreeMap<>(), "test:atlas"));
  }
}
