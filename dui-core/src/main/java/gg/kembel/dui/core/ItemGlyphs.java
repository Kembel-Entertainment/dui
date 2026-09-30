package gg.kembel.dui.core;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** References vanilla item textures unchanged, split into two text rows by the bitmap provider. */
public final class ItemGlyphs {
  private static final Map<String, int[]> WIDTHS = new LinkedHashMap<>();

  static {
    try (var in = ItemGlyphs.class.getResourceAsStream("/ui/item-fonts.json")) {
      var root =
          JsonParser.parseReader(
                  new InputStreamReader(Objects.requireNonNull(in), StandardCharsets.UTF_8))
              .getAsJsonObject();
      root.entrySet()
          .forEach(
              e ->
                  WIDTHS.put(
                      e.getKey(),
                      new int[] {
                        e.getValue().getAsJsonArray().get(0).getAsInt(),
                        e.getValue().getAsJsonArray().get(1).getAsInt()
                      }));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Set<String> names() {
    return Collections.unmodifiableSet(WIDTHS.keySet());
  }

  public static char glyph(String name, int half) {
    int index = new ArrayList<>(WIDTHS.keySet()).indexOf(name);
    if (index < 0 || half < 0 || half > 1)
      throw new IllegalArgumentException("Unknown item glyph: " + name);
    return (char) (0xEC00 + index * 2 + half);
  }

  public static int advance(String name, int half) {
    return WIDTHS.get(name)[half];
  }

  public static byte[] font() {
    var providers = new JsonArray();
    for (String name : names()) {
      var p = new JsonObject();
      p.addProperty("type", "bitmap");
      p.addProperty("file", "minecraft:" + name + ".png");
      p.addProperty("height", 9);
      p.addProperty("ascent", 7);
      var chars = new JsonArray();
      chars.add("" + glyph(name, 0));
      chars.add("" + glyph(name, 1));
      p.add("chars", chars);
      providers.add(p);
    }
    var root = new JsonObject();
    root.add("providers", providers);
    return root.toString().getBytes(StandardCharsets.UTF_8);
  }
}
