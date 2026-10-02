package gg.kembel.dui.core;

public final class GlyphAtlas {
  private GlyphAtlas() {}

  public static char rectangle(int bit, int height, int alpha) {
    if (alpha < 1 || alpha > 15 || bit < 0 || bit > 8 || height < 1 || height > 9)
      throw new IllegalArgumentException("RGBA glyph bounds");
    return alpha == 15
        ? rectangle(bit, height)
        : (char) (0xf000 + (alpha - 1) * 81 + (height - 1) * 9 + bit);
  }

  public static char rectangle(int bit, int height) {
    return (char) (0xEA00 + (height - 1) * 9 + bit);
  }
}
