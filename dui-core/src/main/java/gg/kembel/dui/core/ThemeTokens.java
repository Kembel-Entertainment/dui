package gg.kembel.dui.core;

import java.util.*;

/** Immutable semantic palette. Additional token names are allowed without changing an enum. */
public record ThemeTokens(
    Map<String, Integer> colors, Map<String, Integer> spacing, Map<String, String> typography) {
  public static final ThemeTokens DARK =
      new ThemeTokens(
          Map.ofEntries(
              Map.entry("surface", 0x16171D),
              Map.entry("raised", 0x22232B),
              Map.entry("text", 0xEAEAF1),
              Map.entry("muted", 0x9697A5),
              Map.entry("accent", 0x58E6DB),
              Map.entry("border", 0x34343F),
              Map.entry("success", 0x62D394),
              Map.entry("warning", 0xF4D06B),
              Map.entry("danger", 0xEF818C),
              Map.entry("selected", 0x24504C),
              Map.entry("disabled", 0x202127)),
          Map.of("small", 3, "medium", 6, "large", 12),
          Map.of("body", "text"));

  public ThemeTokens {
    colors = Map.copyOf(colors);
    spacing = Map.copyOf(spacing);
    typography = Map.copyOf(typography);
    for (var value : colors.values())
      if (value < 0 || value > 0xffffff) throw new IllegalArgumentException("RGB token");
    for (var key :
        List.of(
            "surface",
            "raised",
            "text",
            "muted",
            "accent",
            "border",
            "success",
            "warning",
            "danger",
            "selected",
            "disabled"))
      if (!colors.containsKey(key))
        throw new IllegalArgumentException("Missing theme token: " + key);
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

  /** Typography aliases resolve to supported semantic colours; font geometry stays pack-owned. */
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

  public int semantic(int legacy) {
    return switch (legacy) {
      case 0x16171D -> color("surface");
      case 0x22232B, 0x292B35 -> color("raised");
      case 0xEAEAF1 -> color("text");
      case 0x9697A5 -> color("muted");
      case 0x58E6DB -> color("accent");
      case 0x34343F -> color("border");
      case 0x62D394 -> color("success");
      case 0xF4D06B -> color("warning");
      case 0xEF818C -> color("danger");
      case 0x24504C -> color("selected");
      case 0x202127 -> color("disabled");
      default -> legacy;
    };
  }
}
