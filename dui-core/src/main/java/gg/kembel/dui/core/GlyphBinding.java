package gg.kembel.dui.core;

public record GlyphBinding(GlyphSpec specification, int codePoint) {
  public GlyphBinding {
    if (specification == null || codePoint < 0xEB00 || codePoint > 0xEBFF)
      throw new IllegalArgumentException("Glyph code");
  }

  public char character(int band) {
    if (band < 0 || band > 2) throw new IllegalArgumentException("Glyph band");
    return (char) (codePoint + band * 256);
  }

  public int advance() {
    return specification.width() + 1;
  }
}
