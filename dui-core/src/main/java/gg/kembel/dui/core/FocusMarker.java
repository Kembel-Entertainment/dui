package gg.kembel.dui.core;

/** Size-addressed technical font marker. Its text RGB remains fully caller-owned. */
public final class FocusMarker {
  public static final int BASE = 0xF0000, ROWS = 40, ADVANCE = 9;

  private FocusMarker() {}

  public static String glyph(Canvas canvas) {
    return Character.toString(BASE + (canvas.width - 120) * ROWS + canvas.height / 9 - 1);
  }
}
