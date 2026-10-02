package gg.kembel.dui.core;

import java.util.*;

/** Measured text runs. Each run selects a registered font, tint and opacity. */
public record RichText(List<Span> spans) {
  public record Span(String text, String font, int color, double opacity) {
    public Span {
      Objects.requireNonNull(text);
      Objects.requireNonNull(font);
      if (color < 0 || color > 0xffffff || !Double.isFinite(opacity) || opacity < 0 || opacity > 1)
        throw new IllegalArgumentException("Rich text style");
    }
  }

  public RichText {
    spans = List.copyOf(spans);
    if (spans.size() > 512) throw new IllegalArgumentException("Text span budget");
  }

  public Measure.Size measure(Map<String, BitmapFont> fonts) {
    int width = 0, height = 0;
    for (var span : spans) {
      var font = require(fonts, span.font());
      width = Math.addExact(width, font.width(span.text()));
      height = Math.max(height, font.lineHeight());
    }
    return new Measure.Size(width, height);
  }

  static BitmapFont require(Map<String, BitmapFont> fonts, String id) {
    var font = fonts.get(id);
    if (font == null) throw new IllegalArgumentException("Unregistered font: " + id);
    return font;
  }
}
