package gg.kembel.dui.core;

import java.util.*;

/** Immutable semantic palette. Additional token names are allowed without changing an enum. */
public record ThemeTokens(
    Map<String, Integer> colors, Map<String, Integer> spacing, Map<String, String> typography) {
  public static final ThemeTokens EMPTY = new ThemeTokens(Map.of(), Map.of(), Map.of());

  public ThemeTokens {
    colors = Map.copyOf(colors);
    spacing = Map.copyOf(spacing);
    typography = Map.copyOf(typography);
    for (var value : colors.values())
      if (value < 0 || value > 0xffffff) throw new IllegalArgumentException("RGB token");
    if (spacing.values().stream().anyMatch(v -> v < 0))
      throw new IllegalArgumentException("Negative spacing");
  }

  public int color(String name) {
    Integer value = colors.get(name);
    if (value == null) throw new IllegalArgumentException("Unknown theme token: " + name);
    return value;
  }

  public int space(String name) {
    Integer value = spacing.get(name);
    if (value == null) throw new IllegalArgumentException("Unknown spacing token: " + name);
    return value;
  }

  public String type(String name) {
    String value = typography.get(name);
    if (value == null) throw new IllegalArgumentException("Unknown typography token: " + name);
    color(value);
    return "$" + value;
  }

  public ThemeTokens with(Map<String, Integer> overrides) {
    var c = new HashMap<>(colors);
    c.putAll(overrides);
    return new ThemeTokens(c, spacing, typography);
  }
}
