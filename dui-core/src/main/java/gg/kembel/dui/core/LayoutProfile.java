package gg.kembel.dui.core;

/** Explicit GUI units, selected by the user; vanilla does not expose its viewport to Paper. */
public record LayoutProfile(String id, int width, int height, int nativeBodies) {
  public static final LayoutProfile COMPACT = new LayoutProfile("compact", 320, 180, 0),
      WIDE = new LayoutProfile("wide", 480, 360, 0);

  public LayoutProfile {
    if (id == null
        || id.isBlank()
        || width < 120
        || width > 480
        || height < 9
        || height > 360
        || height % 9 != 0
        || nativeBodies < 0) throw new IllegalArgumentException("Layout profile bounds");
  }

  public int requiredDialogHeight() {
    return height + nativeBodies * 11;
  }

  public static LayoutProfile choose(boolean compact) {
    return compact ? COMPACT : WIDE;
  }
}
