package gg.kembel.dui.core;

import java.util.*;

/** Explicit pixel font, usable for measured rich text and transformed raster groups. */
public record BitmapFont(String id, int lineHeight, Map<String, Glyph> glyphs) {
  public record Glyph(int width, int height, int advance, List<Integer> pixels) {
    public Glyph {
      pixels = List.copyOf(pixels);
      if (width < 1
          || height < 1
          || width > 128
          || height > 128
          || advance < 0
          || advance > 128
          || pixels.size() != width * height)
        throw new IllegalArgumentException("Font glyph bounds");
    }

    public RasterImage raster() {
      return RasterImage.argb(width, height, pixels.stream().mapToInt(Integer::intValue).toArray());
    }
  }

  public BitmapFont {
    glyphs = Map.copyOf(glyphs);
    if (id == null
        || !id.matches("[a-z][a-z0-9_-]*:[a-z0-9_/-]+")
        || lineHeight < 1
        || lineHeight > 128
        || glyphs.size() > 4096
        || !glyphs.containsKey("?")) throw new IllegalArgumentException("Bitmap font contract");
    for (String key : glyphs.keySet())
      if (key.codePointCount(0, key.length()) != 1)
        throw new IllegalArgumentException("One codepoint per glyph");
  }

  public Glyph glyph(int cp) {
    return glyphs.getOrDefault(Character.toString(cp), glyphs.get("?"));
  }

  public int width(String text) {
    return text.codePoints().map(cp -> glyph(cp).advance()).sum();
  }

  public RasterImage raster(String text, int color) {
    int w = Math.max(1, width(text));
    if (w > 512) throw new IllegalArgumentException("Text raster width");
    int[] pixels = new int[w * lineHeight];
    int x = 0;
    for (int cp : text.codePoints().toArray()) {
      var glyph = glyph(cp);
      for (int yy = 0; yy < Math.min(lineHeight, glyph.height()); yy++)
        for (int xx = 0; xx < glyph.width() && x + xx < w; xx++) {
          int argb = glyph.pixels().get(yy * glyph.width() + xx);
          int rgb = 0;
          for (int shift : new int[] {16, 8, 0})
            rgb |= ((argb >> shift & 255) * (color >> shift & 255) / 255) << shift;
          pixels[yy * w + x + xx] = (argb & 0xff000000) | rgb;
        }
      x += glyph.advance();
    }
    return RasterImage.argb(w, lineHeight, pixels);
  }
}
