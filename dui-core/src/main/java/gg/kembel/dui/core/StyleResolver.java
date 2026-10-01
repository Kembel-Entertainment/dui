package gg.kembel.dui.core;

import java.util.*;

public final class StyleResolver {
  private StyleResolver() {}

  /** Later layers win: component defaults, theme, classes, state, explicit properties. */
  @SafeVarargs
  public static Map<String, String> resolve(Map<String, String>... layers) {
    var result = new LinkedHashMap<String, String>();
    for (var layer : layers) result.putAll(layer);
    return Map.copyOf(result);
  }

  public static Map<String, String> state(Map<String, String> properties, String state) {
    var result = new HashMap<String, String>();
    String prefix = state + "-";
    properties.forEach(
        (k, v) -> {
          if (k.startsWith(prefix)) result.put(k.substring(prefix.length()), v);
        });
    return Map.copyOf(result);
  }

  public static int color(String value, ThemeTokens theme) {
    if (value.startsWith("$")) return theme.color(value.substring(1));
    if (!value.matches("#[0-9A-Fa-f]{6}"))
      throw new IllegalArgumentException("Expected $token or #RRGGBB");
    return Integer.parseInt(value.substring(1), 16);
  }
}
