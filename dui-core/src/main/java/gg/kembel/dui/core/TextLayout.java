package gg.kembel.dui.core;

import java.util.*;

/** Font-metric layout; the caller supplies colour, alignment, extent and wrapping policy. */
public final class TextLayout {
  private TextLayout() {}

  public static void draw(
      Canvas canvas,
      String label,
      int x,
      int y,
      int width,
      int height,
      String align,
      boolean wrap,
      int maxLines,
      int color) {
    int lineHeight = canvas.textLineHeight();
    var metrics = canvas.metrics();
    if (height < lineHeight)
      throw new IllegalArgumentException("Text allocation is shorter than its font line height");
    var lines =
        wrap
            ? metrics.wrap(label, width, Math.min(maxLines, height / lineHeight))
            : List.of(metrics.fit(label, width));
    int yy = wrap ? y : y + (height - lineHeight) / 2;
    for (var line : lines) {
      int dx =
          switch (align) {
            case "left" -> 0;
            case "center" -> (width - metrics.width(line)) / 2;
            case "right" -> width - metrics.width(line);
            default -> throw new IllegalArgumentException("Text alignment");
          };
      canvas.text(x + dx, yy, width - dx, line, color);
      yy += lineHeight;
    }
  }
}
