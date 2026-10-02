package gg.kembel.dui.core;

public record GlyphSpec(String id, int width, int height, boolean tintable) {
  public GlyphSpec(String id, int width, int height) {
    this(id, width, height, true);
  }

  public GlyphSpec {
    if (id == null
        || !id.matches("[a-z][a-z0-9_-]*:[a-z0-9_/-]+")
        || width < 1
        || width > 32
        || height < 1
        || height > 18)
      throw new IllegalArgumentException("Glyph id/dimensions (GUI font bands support 1..18 high)");
  }
}
