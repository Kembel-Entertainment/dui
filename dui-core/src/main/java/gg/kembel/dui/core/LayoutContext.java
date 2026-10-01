package gg.kembel.dui.core;

import java.util.Objects;

public record LayoutContext(LayoutProfile profile, GlyphFont metrics) {
  public LayoutContext {
    Objects.requireNonNull(profile);
    Objects.requireNonNull(metrics);
  }

  public int measured(String text) {
    return metrics.width(text);
  }

  public int constrain(int desired, int min, int max) {
    if (min < 0 || max < min) throw new IllegalArgumentException("Invalid constraints");
    return Math.max(min, Math.min(max, desired));
  }
}
