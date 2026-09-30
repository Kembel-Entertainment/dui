package gg.kembel.dui.core;

import java.util.List;

public final class GlyphAtlas {
  public static final List<String> ICONS =
      List.of(
          "settings",
          "grid",
          "diamond",
          "star",
          "users",
          "book",
          "lock",
          "check",
          "coin",
          "leaf",
          "sun",
          "moon",
          "previous",
          "next");

  private GlyphAtlas() {}

  public static char rectangle(int bit, int height) {
    return (char) (0xEA00 + (height - 1) * 9 + bit);
  }

  public static char icon(String name) {
    int i = ICONS.indexOf(name);
    if (i < 0) throw new IllegalArgumentException("Unknown icon: " + name);
    return (char) (0xEB00 + i);
  }

  public static char icon(String name, int band) {
    return (char) (icon(name) + (band == 0 ? 0 : 0x40));
  }
}
