package gg.kembel.dui.core;

import java.util.*;

/**
 * Built-in property support and cost categories; extension packages declare their own attributes.
 */
public final class ComponentSchemas {
  public record Schema(
      String name,
      Set<String> attributes,
      Map<String, String> defaults,
      String constraints,
      String cost,
      String backend) {
    public Schema {
      attributes = Set.copyOf(attributes);
      defaults = Map.copyOf(defaults);
    }
  }

  private static final Set<String> PLACEMENT =
      Set.of(
          "id",
          "class",
          "x",
          "y",
          "width",
          "height",
          "anchor-x",
          "anchor-y",
          "min-width",
          "max-width",
          "min-height",
          "max-height",
          "dock",
          "column-span",
          "row-span");
  private static final Set<String> STYLE =
      Set.of(
          "fill",
          "border",
          "color",
          "disabled-fill",
          "disabled-border",
          "disabled-color",
          "selected-fill",
          "selected-border",
          "selected-color",
          "active-fill",
          "active-border",
          "active-color",
          "highlight",
          "bevel",
          "padding",
          "padding-x",
          "padding-y");
  private static final Map<String, Schema> ALL = build();

  private ComponentSchemas() {}

  public static Map<String, Schema> all() {
    return ALL;
  }

  public static Schema get(String name) {
    Schema s = ALL.get(name);
    if (s == null) throw new IllegalArgumentException("Unknown component schema: " + name);
    return s;
  }

  public static boolean supports(String tag, String attr) {
    return get(tag).attributes().contains(attr);
  }

  private static Map<String, Schema> build() {
    var map = new TreeMap<String, Schema>();
    put(
        map,
        "menu",
        "gap padding background focus-outline-color compact compact-width compact-height"
            + " focus-outline motion animation-start effect-budget",
        "Canvas 120..480 by 9..360; height multiple of nine",
        "canvas",
        "font/native/effects");
    for (String tag : List.of("row", "column", "panel", "layer"))
      put(
          map,
          tag,
          "gap padding columns cross-align title label detail icon tone locked active action"
              + " payload tooltip"
              + (tag.equals("layer") ? " cover dismiss" : ""),
          "Allocated rectangles must fit; hit rows use multiples of nine",
          "paints/hits",
          "font");
    put(
        map,
        "grid",
        "columns gap padding cell-height cross-align",
        "1..16 columns; spanning cells cannot overlap",
        "paints/hits",
        "font");
    put(
        map,
        "icon",
        "name tone",
        "Contributed glyph dimensions 1..32 by 1..18; caller tint policy",
        "paints",
        "font");
    for (String tag : List.of("heading", "text"))
      put(
          map,
          tag,
          "label align wrap max-lines tone font",
          "Injected font metrics; bounded wrapping and ellipsis",
          "paints",
          "font");
    for (String tag : List.of("toggle", "checkbox", "button", "choice", "tab", "badge", "progress"))
      put(
          map,
          tag,
          "label detail value max checked active locked icon action payload tooltip"
              + " previous-tooltip next-tooltip tone player hat",
          "Nine-pixel hit rows; application authorizes actions",
          "paints/hits",
          "font");
    for (String tag : List.of("rect", "surface", "divider", "spacer"))
      put(map, tag, "tone", "Positive bounded geometry", "paints", "font");
    put(
        map,
        "group",
        "gap padding translate-x translate-y scale-x scale-y rotate opacity clip-x clip-y"
            + " clip-width clip-height",
        "Bounded affine raster composition; hit/native capabilities checked",
        "raster/native/hits",
        "group");
    put(
        map,
        "dropdown",
        "label value open locked action select dismiss tooltip dismiss-tooltip",
        "1..8 distinct options; popup must fit above or below",
        "paints/hits/coverage",
        "font/occlusion");
    put(
        map,
        "option",
        "label value locked tooltip",
        "Distinct nonempty value",
        "none",
        "structural");
    put(
        map,
        "player-model",
        "source facing outer-layer idle renderer",
        "Full-body GPU skin and vanilla armor; source is an appearance key; registered renderer"
            + " selects up to four viewports/eight poses",
        "player model / nine-pixel skin bands / up to four armor carriers",
        "player-model-v2");
    put(
        map,
        "head",
        "player hat label action tooltip payload locked",
        "Native 8x8 profile; nine-pixel rows",
        "one portrait",
        "head");
    put(
        map,
        "hitbox",
        "action payload tooltip locked",
        "Unique id and action; full nine-pixel rows",
        "one hit",
        "font");
    put(
        map,
        "image",
        "source pixel-size image-layer action payload tooltip locked",
        "Source snapshot required; pixel-size 1..8; total sample budget 16384",
        "sampled pixels",
        "runtime image");
    put(
        map,
        "item",
        "size clip-x clip-y" + " clip-width clip-height " + String.join(" ", Motion.ATTRIBUTES),
        "Size 1..127; clip offsets -512..511; motion bounded by protocol",
        "one native body (11 GUI units)",
        "native/model/clip/motion");
    put(
        map,
        "repeat",
        "items as",
        "Bound list <=200; stable ids supplied by consumer",
        "expanded nodes",
        "structural");
    put(map, "if", "test", "Boolean binding", "expanded nodes", "structural");
    put(map, "style", "", "Local style properties", "none", "structural");
    put(
        map,
        "component",
        "name props",
        "One visual root; scoped properties and outlets",
        "expanded nodes",
        "structural");
    put(map, "outlet", "name", "Declared named content", "expanded nodes", "structural");
    put(map, "content", "name", "Caller-scoped content", "expanded nodes", "structural");
    return Map.copyOf(map);
  }

  private static void put(
      Map<String, Schema> map,
      String name,
      String attrs,
      String constraints,
      String cost,
      String backend) {
    var set = new HashSet<>(PLACEMENT);
    set.addAll(STYLE);
    if (!attrs.isBlank()) set.addAll(List.of(attrs.split(" ")));
    var defaults = new HashMap<String, String>();
    if (set.contains("gap")) defaults.put("gap", "0");
    if (set.contains("locked")) defaults.put("locked", "false");
    if (name.equals("player-model"))
      defaults.putAll(
          Map.of("source", "viewer", "facing", "0", "outer-layer", "true", "idle", "true"));
    if (name.equals("layer")) defaults.put("cover", "false");
    if (name.equals("menu")) {
      defaults.put("background", "none");
      defaults.put("effect-budget", "8");
      defaults.put("focus-outline", "native");
    }
    map.put(name, new Schema(name, set, defaults, constraints, cost, backend));
  }
}
