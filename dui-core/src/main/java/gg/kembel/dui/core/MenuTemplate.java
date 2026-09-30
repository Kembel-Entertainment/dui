package gg.kembel.dui.core;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

/**
 * Deliberately small HTML-like component language: data, flow layout, components; no browser or
 * scripts.
 */
public final class MenuTemplate {
  public record Node(String type, Map<String, String> props, List<Node> children) {
    String s(String key, String fallback) {
      return props.getOrDefault(key, fallback);
    }

    int n(String key, int fallback) {
      return Integer.parseInt(s(key, "" + fallback));
    }

    boolean b(String key) {
      return s(key, "false").equals("true");
    }
  }

  private static final Set<String> TAGS =
      Set.of(
          "menu",
          "row",
          "column",
          "grid",
          "panel",
          "repeat",
          "if",
          "heading",
          "text",
          "divider",
          "spacer",
          "nav",
          "toggle",
          "checkbox",
          "dropdown",
          "option",
          "button",
          "choice",
          "tab",
          "badge",
          "progress",
          "stat",
          "card",
          "empty",
          "entry",
          "tree",
          "node",
          "head",
          "slot",
          "style",
          "layer",
          "surface",
          "hitbox",
          "wheel",
          "rect",
          "playing-card",
          "chip-stack",
          "reel",
          "lever",
          "particles",
          "lights",
          "image",
          "item");
  private static final Set<String> ATTRS =
      Set.of(
          "width",
          "height",
          "gap",
          "padding",
          "columns",
          "title",
          "label",
          "detail",
          "value",
          "max",
          "checked",
          "active",
          "locked",
          "icon",
          "id",
          "action",
          "tooltip",
          "tone",
          "items",
          "as",
          "test",
          "align",
          "x",
          "y",
          "parent",
          "rank",
          "limit",
          "shape",
          "status",
          "player",
          "hat",
          "count",
          "durability",
          "enchanted",
          "theme",
          "payload",
          "open",
          "select",
          "dismiss",
          "class",
          "fill",
          "border",
          "color",
          "disabled-fill",
          "disabled-border",
          "disabled-color",
          "highlight",
          "bevel",
          "animation-start",
          "motion",
          "previous",
          "duration",
          "turns",
          "symbol-size",
          "symbols",
          "effect",
          "origin-x",
          "origin-y",
          "delay",
          "radius",
          "sequence",
          "compact",
          "compact-width",
          "compact-height",
          "focus-outline",
          "source",
          "pixel-size",
          "size",
          "burst-start",
          "transition",
          "transition-start",
          "transition-duration",
          "transition-distance",
          "clip-x",
          "clip-y",
          "clip-width",
          "clip-height",
          "image-layer",
          "face-down",
          "animation",
          "lift",
          "card-height",
          "palette",
          "from",
          "to",
          "variant");
  private static final Pattern BIND = Pattern.compile("\\{\\{([a-zA-Z_][a-zA-Z_0-9.]*)}}");
  private final Element root;
  private final Map<String, Map<String, String>> styles = new HashMap<>();
  private static final Set<String> STYLE_PROPS =
      Set.of(
          "fill",
          "border",
          "color",
          "disabled-fill",
          "disabled-border",
          "disabled-color",
          "highlight",
          "bevel",
          "padding");
  private final GlyphFont metrics;

  private MenuTemplate(Element root, GlyphFont metrics) {
    this.metrics = metrics;
    this.root = root;
    for (var child = root.getFirstChild(); child != null; child = child.getNextSibling())
      if (child instanceof Element e && e.getTagName().equals("dui-style")) {
        String id = e.getAttribute("id");
        if (id.isBlank() || styles.containsKey(id))
          throw new IllegalArgumentException("Duplicate / empty style: " + id);
        Map<String, String> props = new HashMap<>();
        for (int i = 0; i < e.getAttributes().getLength(); i++) {
          var a = e.getAttributes().item(i);
          if (!a.getNodeName().equals("id")) {
            if (!STYLE_PROPS.contains(a.getNodeName()))
              throw new IllegalArgumentException("Invalid style property: " + a.getNodeName());
            props.put(a.getNodeName(), a.getNodeValue());
          }
        }
        styles.put(id, Map.copyOf(props));
      }
  }

  public static MenuTemplate parse(String xml) throws Exception {
    return parse(xml, new GlyphFont());
  }

  public static MenuTemplate parse(String xml, GlyphFont metrics) throws Exception {
    if (xml.length() > 128_000) throw new IllegalArgumentException("Template too large");
    var factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    Element root =
        factory
            .newDocumentBuilder()
            .parse(new InputSource(new StringReader(xml)))
            .getDocumentElement();
    if (!root.getTagName().equals("dui-menu"))
      throw new IllegalArgumentException("Expected dui-menu");
    validate(root, 0);
    return new MenuTemplate(root, Objects.requireNonNull(metrics));
  }

  private static void validate(Element e, int depth) {
    if (depth > 20
        || !e.getTagName().startsWith("dui-")
        || !TAGS.contains(e.getTagName().substring(4)))
      throw new IllegalArgumentException("Unknown / nested component: " + e.getTagName());
    if (e.getTagName().equals("dui-style") && (depth != 1 || e.hasChildNodes()))
      throw new IllegalArgumentException("Styles must be empty direct children of dui-menu");
    for (int i = 0; i < e.getAttributes().getLength(); i++)
      if (!ATTRS.contains(e.getAttributes().item(i).getNodeName()))
        throw new IllegalArgumentException(
            "Unknown attribute: " + e.getAttributes().item(i).getNodeName());
    for (var child = e.getFirstChild(); child != null; child = child.getNextSibling())
      if (child instanceof Element el) validate(el, depth + 1);
  }

  private static Object lookup(Map<String, Object> data, String path) {
    Object value = data;
    for (String part : path.split("\\.")) {
      if (!(value instanceof Map<?, ?> m) || !m.containsKey(part))
        throw new IllegalArgumentException("Missing binding: " + path);
      value = m.get(part);
    }
    return value;
  }

  private static String bind(String value, Map<String, Object> data) {
    var m = BIND.matcher(value);
    StringBuilder out = new StringBuilder();
    while (m.find())
      m.appendReplacement(out, Matcher.quoteReplacement(String.valueOf(lookup(data, m.group(1)))));
    m.appendTail(out);
    return out.toString();
  }

  private List<Node> expand(Element e, Map<String, Object> data, int[] budget) {
    if (--budget[0] < 0) throw new IllegalArgumentException("Expanded node limit");
    String type = e.getTagName().substring(4);
    if (type.equals("style")) return List.of();
    if (type.equals("if") && !bind(e.getAttribute("test"), data).equals("true")) return List.of();
    if (type.equals("repeat")) {
      String path = e.getAttribute("items").replace("{{", "").replace("}}", "");
      Object value = lookup(data, path);
      if (!(value instanceof List<?> list) || list.size() > 200)
        throw new IllegalArgumentException("repeat needs list <= 200");
      List<Node> nodes = new ArrayList<>();
      for (Object item : list) {
        var scope = new HashMap<>(data);
        scope.put(e.getAttribute("as"), item);
        nodes.addAll(children(e, scope, budget));
      }
      return nodes;
    }
    if (type.equals("if")) return children(e, data, budget);
    Map<String, String> props = new HashMap<>();
    String classes = bind(e.getAttribute("class"), data).strip();
    if (!classes.isEmpty())
      for (String name : classes.split("\\s+")) {
        var style = styles.get(name);
        if (style == null) throw new IllegalArgumentException("Unknown style: " + name);
        style.forEach((key, value) -> props.put(key, bind(value, data)));
      }
    for (int i = 0; i < e.getAttributes().getLength(); i++) {
      var a = e.getAttributes().item(i);
      props.put(a.getNodeName(), bind(a.getNodeValue(), data));
    }
    return List.of(new Node(type, props, children(e, data, budget)));
  }

  private List<Node> children(Element e, Map<String, Object> data, int[] budget) {
    List<Node> nodes = new ArrayList<>();
    for (var child = e.getFirstChild(); child != null; child = child.getNextSibling())
      if (child instanceof Element el) nodes.addAll(expand(el, data, budget));
    return nodes;
  }

  public Canvas render(Map<String, Object> data) {
    return render(data, Map.of());
  }

  public Canvas render(Map<String, Object> data, Map<String, RasterImage> images) {
    Node node = expand(root, data, new int[] {512}).getFirst();
    UiTheme theme = UiTheme.named(node.s("theme", "default"));
    Canvas canvas =
        new Canvas(
            node.b("compact")
                ? node.n("compact-width", node.n("width", 440))
                : node.n("width", 440),
            node.b("compact")
                ? node.n("compact-height", node.n("height", 306))
                : node.n("height", 306),
            theme,
            metrics);
    if (!Set.of("hidden", "native").contains(node.s("focus-outline", "hidden")))
      throw new IllegalArgumentException("focus-outline must be hidden or native");
    canvas.motionEnabled = !node.s("motion", "true").equals("false");
    canvas.hideFocusOutline = !node.s("focus-outline", "hidden").equals("native");
    if (theme != UiTheme.DEFAULT) canvas.rect(0, 0, canvas.width, canvas.height, BG);
    var overlays = new ArrayList<Runnable>();
    draw(canvas, node, 0, 0, canvas.width, canvas.height, overlays, false, images);
    overlays.forEach(Runnable::run);
    if (!canvas.effects.isEmpty()) {
      String carrier = "__effects";
      if (canvas.items.stream().anyMatch(i -> i.id().equals(carrier)))
        throw new IllegalArgumentException("Reserved item id: " + carrier);
      canvas.item(carrier, 0, 0, 1);
      canvas.animation =
          new Canvas.Animation(
              carrier,
              Long.parseLong(node.s("animation-start", "-1")),
              !node.s("motion", "true").equals("false"),
              canvas.effects.stream().mapToInt(ShaderEffect::lifetimeTicks).max().orElse(0));
    }
    return canvas;
  }

  private static final int BG = 0x16171D,
      EDGE = 0x34343F,
      MUTED = 0x9697A5,
      WHITE = 0xEAEAF1,
      CYAN = 0x58E6DB,
      GOLD = 0xF4D06B,
      GREEN = 0x62D394;

  private static int tone(Node n) {
    return switch (n.s("tone", "")) {
      case "gold" -> GOLD;
      case "green" -> GREEN;
      case "danger" -> 0xEF818C;
      case "muted" -> MUTED;
      default -> CYAN;
    };
  }

  private static int natural(Node n) {
    if (n.props.containsKey("height") && !n.s("height", "").equals("fill"))
      return n.n("height", 18);
    return switch (n.type) {
      case "divider", "spacer", "badge", "progress" -> 9;
      case "card", "empty" -> 72;
      case "slot" -> 63;
      case "stat" -> 45;
      case "nav", "entry", "dropdown" -> 27;
      case "column", "panel" ->
          n.children.stream().mapToInt(MenuTemplate::natural).sum()
              + Math.max(0, n.children.size() - 1) * n.n("gap", 0)
              + (n.type.equals("panel") ? 18 : 0);
      default -> 18;
    };
  }

  private static int color(Node n, String key, int fallback) {
    String value = n.s(key, "");
    if (value.isEmpty()) return fallback;
    if (!value.matches("#[0-9a-fA-F]{6}"))
      throw new IllegalArgumentException("Expected #RRGGBB for " + key);
    return Integer.parseInt(value.substring(1), 16);
  }

  private static void surface(
      Canvas c, int x, int y, int w, int h, int fill, int border, int bevel) {
    if (bevel == 0) {
      c.rect(x, y, w, h, fill);
      return;
    }
    if (bevel < 0 || bevel > 4 || w < bevel * 2 + 1 || h < bevel * 2 + 2)
      throw new IllegalArgumentException("Invalid surface bevel");
    c.rect(x + bevel, y, w - bevel * 2, h, border);
    c.rect(x, y + bevel, w, h - bevel * 2, border);
    c.rect(x + bevel, y + bevel, w - bevel * 2, h - bevel * 2 - 1, fill);
  }

  private static void draw(
      Canvas c,
      Node n,
      int x,
      int y,
      int w,
      int h,
      List<Runnable> overlays,
      boolean positioned,
      Map<String, RasterImage> images) {
    boolean clippedItem = n.type.equals("item") && !n.s("clip-width", "").isBlank();
    if (w < 1 || h < 0 || !clippedItem && (x < 0 || y < 0 || x + w > c.width || y + h > c.height))
      throw new IllegalArgumentException("Component outside canvas: " + n.type);
    if (h == 0) return;
    if (n.type.equals("layer")) {
      for (Node ch : n.children) {
        int cx = ch.n("x", 0),
            cy = ch.n("y", 0),
            cw = ch.s("width", "fill").equals("fill") ? w - cx : ch.n("width", w),
            chh = ch.s("height", "fill").equals("fill") ? h - cy : ch.n("height", natural(ch));
        boolean childClip = ch.type.equals("item") && !ch.s("clip-width", "").isBlank();
        if (cw < 1 || chh < 1 || !childClip && (cx < 0 || cy < 0 || cx + cw > w || cy + chh > h))
          throw new IllegalArgumentException("Layer overflow: " + ch.type);
        draw(c, ch, x + cx, y + cy, cw, chh, overlays, true, images);
      }
      return;
    }
    if (n.type.equals("hitbox")) {
      String id = n.s("id", ""), action = n.s("action", "");
      if (id.isBlank() || action.isBlank())
        throw new IllegalArgumentException("Hitbox needs id and action");
      c.hit(
          new Canvas.Hit(
              id, n.b("locked") ? "" : action, n.s("payload", ""), n.s("tooltip", ""), x, y, w, h));
      return;
    }
    if (n.type.equals("rect")) {
      c.rect(x, y, w, h, color(n, "fill", BG));
      return;
    }
    if (n.type.equals("surface")) {
      surface(c, x, y, w, h, color(n, "fill", BG), color(n, "border", EDGE), n.n("bevel", 1));
      return;
    }
    if (n.type.equals("image")) {
      String source = n.s("source", "");
      var raster = images.get(source);
      if (raster == null) throw new IllegalArgumentException("Missing image source: " + source);
      String imageLayer = n.s("image-layer", "foreground");
      if (!java.util.Set.of("background", "foreground").contains(imageLayer))
        throw new IllegalArgumentException("Image layer must be background or foreground");
      c.image(
          n.s("id", source),
          x,
          y,
          w,
          h,
          n.n("pixel-size", 3),
          raster,
          imageLayer.equals("background"));
      if (!n.s("action", "").isBlank())
        c.hit(
            new Canvas.Hit(
                n.s("id", source),
                n.b("locked") ? "" : n.s("action", ""),
                n.s("payload", ""),
                n.s("tooltip", ""),
                x,
                y,
                w,
                h));
      return;
    }
    if (n.type.equals("item")) {
      String id = n.s("id", "");
      int size = n.n("size", Math.min(w, h));
      ItemClip clip = null;
      boolean clipped =
          !n.s("clip-x", "").isBlank()
              || !n.s("clip-y", "").isBlank()
              || !n.s("clip-width", "").isBlank()
              || !n.s("clip-height", "").isBlank();
      if (clipped) {
        for (String attr : java.util.List.of("clip-x", "clip-y", "clip-width", "clip-height"))
          if (n.s(attr, "").isBlank())
            throw new IllegalArgumentException(
                "Item clip requires all four clip attributes: " + id);
        clip =
            new ItemClip(
                n.n("clip-x", 0), n.n("clip-y", 0), n.n("clip-width", 0), n.n("clip-height", 0));
      }
      c.item(id, x, y, size, clip);
      String transition = n.s("transition", "");
      if (!transition.isBlank()) {
        try {
          c.transition(
              id,
              new ItemTransition(
                  ItemTransition.Kind.valueOf(transition.toUpperCase(java.util.Locale.ROOT)),
                  Long.parseLong(n.s("transition-start", "-1")),
                  n.n("transition-duration", 24),
                  n.n("transition-distance", 36),
                  c.motionEnabled));
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException(
              "Invalid item transition on " + id + ": " + e.getMessage(), e);
        }
      }
      long burst = Long.parseLong(n.s("burst-start", "-1"));
      if (burst >= 0) {
        if (!transition.isBlank() || clipped)
          throw new IllegalArgumentException(
              "Use a separate particles component with item transitions or clips");
        if (c.confetti != null)
          throw new IllegalArgumentException("Only one confetti carrier is supported");
        c.confetti = new Canvas.Confetti(id, burst);
      }
      return;
    }
    if (EffectComponent.supports(n.type)) {
      EffectComponent.draw(c, n, x, y, w, h);
      return;
    }
    if (w < 9 || h < 0 || !positioned && (y % 9 != 0 || h % 9 != 0))
      throw new IllegalArgumentException(
          "Invalid layout for " + n.type + ": " + x + "," + y + " " + w + "x" + h);
    if (h == 0) return;
    if (n.type.equals("tree")) {
      TreeComponent.draw(c, n, x, y, w, h);
      return;
    }
    if (n.type.equals("slot")) {
      SlotComponent.draw(c, n, x, y, w, h);
      return;
    }
    if (n.type.equals("dropdown")) {
      DropdownComponent.draw(c, n, x, y, w, h, overlays);
      return;
    }
    boolean container = Set.of("menu", "row", "column", "grid", "panel", "card").contains(n.type);
    if (container) {
      if (n.type.equals("panel") || n.type.equals("card"))
        c.panel(
            x,
            y,
            w,
            h,
            n.type.equals("card") ? 0x22232B : BG,
            n.type.equals("panel") ? CYAN : EDGE);
      int pad = n.n("padding", n.type.equals("panel") || n.type.equals("card") ? 6 : 0),
          py = pad == 0 ? 0 : 9;
      x += pad;
      y += py;
      w -= pad * 2;
      h -= py * 2;
      int gap = n.n("gap", 0);
      if (n.type.equals("row") || n.type.equals("menu")) {
        int fixed =
            n.children.stream()
                .filter(ch -> ch.props.containsKey("width") && !ch.s("width", "").equals("fill"))
                .mapToInt(ch -> ch.n("width", 0))
                .sum();
        long flexible =
            n.children.stream()
                .filter(ch -> !ch.props.containsKey("width") || ch.s("width", "").equals("fill"))
                .count();
        int available = w - fixed - gap * Math.max(0, n.children.size() - 1);
        int left = (int) flexible;
        for (Node ch : n.children) {
          boolean auto = !ch.props.containsKey("width") || ch.s("width", "").equals("fill");
          int cw = auto ? available / left : ch.n("width", w);
          if (auto) {
            available -= cw;
            left--;
          }
          draw(c, ch, x, y, cw, h, overlays, positioned, images);
          x += cw + gap;
        }
      } else if (n.type.equals("grid")) {
        int columns = n.n("columns", 2);
        if (columns < 1 || columns > 16) throw new IllegalArgumentException("grid columns 1..16");
        int cw = (w - gap * (columns - 1)) / columns, cy = y, index = 0, rh = 0;
        for (Node ch : n.children) {
          int chh = natural(ch);
          draw(
              c, ch, x + (index % columns) * (cw + gap), cy, cw, chh, overlays, positioned, images);
          rh = Math.max(rh, chh);
          if (++index % columns == 0) {
            cy += rh + gap;
            rh = 0;
          }
        }
        if (cy + (index % columns == 0 ? 0 : rh) > y + h + gap)
          throw new IllegalArgumentException("Grid overflow");
      } else {
        int fixed =
            n.children.stream()
                .filter(ch -> !ch.s("height", "").equals("fill"))
                .mapToInt(MenuTemplate::natural)
                .sum();
        long fill = n.children.stream().filter(ch -> ch.s("height", "").equals("fill")).count();
        int available = h - fixed - gap * Math.max(0, n.children.size() - 1), left = (int) fill;
        if (available < 0)
          throw new IllegalArgumentException(
              "Column overflow: " + n.type + " needs " + (h - available) + ", has " + h);
        for (Node ch : n.children) {
          boolean auto = ch.s("height", "").equals("fill");
          int chh = auto ? (available / left / 9) * 9 : natural(ch);
          if (auto) {
            available -= chh;
            left--;
          }
          draw(c, ch, x, y, w, chh, overlays, positioned, images);
          y += chh + gap;
        }
      }
      return;
    }
    String label = n.s("label", ""),
        detail = n.s("detail", ""),
        value = n.s("value", ""),
        icon = n.s("icon", "");
    int accent = tone(n);
    int ty = y + Math.max(0, (h - 9) / 2);
    boolean locked = n.b("locked");
    switch (n.type) {
      case "head" -> {
        int hy = y + (h >= 27 ? 9 : 0);
        c.head(x, hy, n.s("player", "self"), !n.s("hat", "true").equals("false"));
        if (!label.isBlank()) c.text(x + 12, hy, w - 12, label, WHITE);
      }
      case "heading" -> {
        c.text(x, ty, w, label, color(n, "color", WHITE));
      }
      case "text" -> {
        int tx =
            n.s("align", "").equals("center")
                ? x + (w - c.metrics().width(c.metrics().fit(label, w))) / 2
                : x;
        c.text(
            tx,
            ty,
            w - (tx - x),
            label,
            color(n, "color", n.s("tone", "").equals("") ? MUTED : accent));
      }
      case "divider" -> c.rect(x, y + 3, w, 1, EDGE);
      case "spacer" -> {}
      case "nav" -> {
        if (n.b("active")) {
          c.panel(x, y, w, h - 1, 0x273436, 0x376564);
          c.rect(x, y + 3, 2, h - 7, CYAN);
        }
        if (!icon.isEmpty())
          c.icon(
              x + 6,
              icon.startsWith("item/") ? y + (h >= 27 ? 9 : 0) : ty,
              icon,
              n.b("active") ? CYAN : MUTED);
        int labelOffset = icon.startsWith("item/") ? 29 : 21;
        c.text(x + labelOffset, ty, w - labelOffset - 4, label, n.b("active") ? CYAN : MUTED);
      }
      case "toggle" -> {
        c.rect(x, y + h - 2, w, 1, 0x282932);
        c.text(x + 3, ty, w - 36, label, locked ? MUTED : WHITE);
        if (locked) c.icon(x + w - 18, ty, "lock", GOLD);
        else {
          int sx = x + w - 25, sy = y + (h - 10) / 2;
          c.panel(
              sx,
              sy,
              23,
              10,
              n.b("checked") ? 0x259F86 : 0x484955,
              n.b("checked") ? 0x42D8B8 : 0x626372);
          c.rect(sx + (n.b("checked") ? 14 : 2), sy + 1, 7, 8, WHITE);
        }
      }
      case "checkbox" -> {
        if (h < 18 || w < 30)
          throw new IllegalArgumentException("Checkbox needs at least 30x18 pixels");
        int sy = y + (h - 12) / 2;
        c.panel(x, sy, 12, 12, n.b("checked") ? 0x259F86 : 0x22232B, locked ? 0x626372 : 0x58E6DB);
        if (n.b("checked")) c.icon(x + 1, sy + 1, "check", locked ? 0x9697A5 : 0xF9FAFB);
        c.text(x + 18, ty, w - 21, label, locked ? MUTED : WHITE);
      }
      case "button", "tab", "choice" -> {
        if (n.type.equals("button") && n.props.containsKey("fill")) {
          surface(
              c,
              x,
              y,
              w,
              h,
              color(n, locked ? "disabled-fill" : "fill", locked ? 0x403041 : BG),
              color(n, locked ? "disabled-border" : "border", EDGE),
              n.n("bevel", 1));
          if (n.props.containsKey("highlight"))
            c.rect(x + 3, y + 3, w - 6, 1, color(n, "highlight", WHITE));
          String fit = c.metrics().fit(label, w - 6);
          int tx =
              n.s("align", "center").equals("left") ? x + 3 : x + (w - c.metrics().width(fit)) / 2;
          c.text(
              tx,
              ty,
              w - (tx - x),
              fit,
              color(n, locked ? "disabled-color" : "color", locked ? MUTED : WHITE));
          break;
        }
        boolean active = n.b("active");
        c.panel(
            x, y, w, h - 1, locked ? 0x202127 : active ? 0x24504C : 0x292B35, active ? CYAN : EDGE);
        if (n.type.equals("choice")) {
          c.text(x + 6, ty, w / 2 - 9, label, MUTED);
          c.text(x + w / 2, ty, 9, "<", CYAN);
          c.text(x + w / 2 + 18, ty, w / 2 - 42, value, CYAN);
          c.text(x + w - 15, ty, 9, ">", CYAN);
          if (!locked && !n.s("action", "").isBlank()) {
            String id = n.s("id", n.s("action", ""));
            c.hit(
                new Canvas.Hit(
                    id + "_previous",
                    n.s("action", ""),
                    "-1",
                    "Previous option",
                    x + w / 2 - 3,
                    y,
                    21,
                    h));
            c.hit(
                new Canvas.Hit(
                    id + "_next", n.s("action", ""), "1", "Next option", x + w - 24, y, 24, h));
          }
        } else if (label.isBlank() && !icon.isBlank()) {
          int size = icon.startsWith("item/") ? 18 : 9;
          c.icon(
              x + (w - size) / 2,
              icon.startsWith("item/") ? y + (h >= 27 ? 9 : 0) : ty,
              icon,
              locked ? MUTED : accent);
        } else {
          String fitted = c.metrics().fit(label, w - 10);
          int tx =
              n.s("align", "center").equals("left")
                  ? x + 6
                  : x + (w - c.metrics().width(fitted)) / 2;
          c.text(tx, ty, w - (tx - x) - 3, fitted, locked ? MUTED : active ? CYAN : accent);
        }
      }
      case "badge" -> {
        c.rect(x, y, w, 9, 0x33313B);
        c.text(x + 3, y, w - 6, label, accent);
      }
      case "progress" -> {
        double progress =
            Math.max(
                0,
                Math.min(
                    1,
                    Double.parseDouble(value)
                        / Math.max(1, Double.parseDouble(n.s("max", "100")))));
        c.panel(x, y + 1, w, 6, 0x30313D, EDGE);
        c.rect(x + 1, y + 2, (int) ((w - 2) * progress), 4, accent);
      }
      case "stat" -> {
        c.panel(x, y, w, h - 1, 0x22232B, EDGE);
        if (!icon.isEmpty()) c.icon(x + 6, y + 9, icon, accent);
        c.text(x + (icon.isEmpty() ? 6 : 21), y + 9, w - 27, label, MUTED);
        c.text(x + 6, y + 27, w - 12, value, accent);
      }
      case "empty" -> {
        c.panel(x, y, w, h - 1, 0x202127, EDGE);
        c.icon(x + w / 2 - 4, y + 9, icon.isEmpty() ? "book" : icon, MUTED);
        String text = c.metrics().fit(label, w - 18);
        c.text(x + (w - c.metrics().width(text)) / 2, y + 27, w - 12, text, WHITE);
        c.text(x + 9, y + 45, w - 18, detail, MUTED);
      }
      case "entry" -> {
        boolean head = !n.s("player", "").isBlank();
        int ey = head ? y + (h >= 27 ? 9 : 0) : ty;
        c.rect(x, y, w, h - 1, 0x22232B);
        if (head) c.head(x + 6, ey, n.s("player", ""), !n.s("hat", "true").equals("false"));
        else if (!icon.isEmpty())
          c.icon(x + 6, icon.startsWith("item/") ? y + (h >= 27 ? 9 : 0) : ty, icon, accent);
        c.text(x + 21, ey, w - 100, label, WHITE);
        c.text(x + w - 76, ey, 70, value, accent);
      }
      default -> throw new IllegalArgumentException("Unsupported component " + n.type);
    }
    String action = n.s("action", "");
    if (locked)
      c.hit(
          new Canvas.Hit(
              n.s("id", "locked:" + label),
              "",
              value,
              n.s("tooltip", "Locked / " + label),
              x,
              y,
              w,
              h));
    else if (!action.isBlank() && !n.type.equals("choice"))
      c.hit(
          new Canvas.Hit(
              n.s("id", action + ":" + value),
              action,
              n.s("payload", value),
              n.s("tooltip", label),
              x,
              y,
              w,
              h));
  }
}
