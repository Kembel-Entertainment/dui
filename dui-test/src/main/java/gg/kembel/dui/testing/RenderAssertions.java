package gg.kembel.dui.testing;

import gg.kembel.dui.core.*;

public final class RenderAssertions {
  private RenderAssertions() {}

  public static Canvas.Hit hit(Canvas c, String id) {
    return c.hits.stream()
        .filter(h -> h.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Missing hit: " + id));
  }

  public static void visibleHits(Canvas c) {
    for (var h : c.hits) {
      if (h.y() % 9 != 0
          || h.height() % 9 != 0
          || h.x() < 0
          || h.y() < 0
          || h.x() + h.width() > c.width
          || h.y() + h.height() > c.height) throw new AssertionError("Invalid hit: " + h);
    }
  }

  public static void budget(Canvas c) {
    var report = RenderReport.of(c);
    if (report.remainingEffects() < 0 || report.remainingImagePixels() < 0)
      throw new AssertionError("Render budget exceeded: " + report);
  }

  public static void action(Canvas c, String id, String action, String value) {
    var h = hit(c, id);
    if (!h.action().equals(action) || !h.value().equals(value))
      throw new AssertionError("Unexpected action: " + h);
  }
}
