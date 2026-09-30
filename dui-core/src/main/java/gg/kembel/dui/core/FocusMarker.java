package gg.kembel.dui.core;

public final class FocusMarker {
  public static final char GLYPH = '\uECF0';
  public static final int ADVANCE = 9;

  private FocusMarker() {}

  public static int payload(Canvas canvas) {
    return canvas.width | (canvas.height << 9);
  }
}
