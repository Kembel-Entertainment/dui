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
  public record Node(
      String type, Map<String, String> props, List<Node> children, Map<String, Object> values) {
    public Node(String type, Map<String, String> props, List<Node> children) {
      this(type, props, children, Map.of());
    }

    public Node {
      props = Map.copyOf(props);
      children = List.copyOf(children);
      values = Map.copyOf(values);
    }

    public <T> T value(String key, Class<T> type) {
      if (!values.containsKey(key))
        throw new IllegalArgumentException("Missing typed property: " + key);
      return type.cast(values.get(key));
    }

    public String s(String key, String fallback) {
      return props.getOrDefault(key, fallback);
    }

    public int n(String key, int fallback) {
      return Integer.parseInt(s(key, "" + fallback));
    }

    public boolean b(String key) {
      return s(key, "false").equals("true");
    }
  }

  private static final Set<String> TAGS = ComponentSchemas.all().keySet();
  private static final Pattern BIND = Pattern.compile("\\{\\{([a-zA-Z_][a-zA-Z_0-9.-]*)}}");
  private final Element root;
  private final Map<String, Map<String, String>> styles = new HashMap<>();
  private static final Set<String> STYLE_PROPS =
      Set.of(
          "fill",
          "border",
          "color",
          "font",
          "selected-fill",
          "selected-border",
          "selected-color",
          "active-fill",
          "active-border",
          "active-color",
          "disabled-fill",
          "disabled-border",
          "disabled-color",
          "highlight",
          "bevel",
          "padding",
          "padding-x",
          "padding-y");
  private final GlyphFont metrics;
  private final RenderEnvironment environment;
  private final Map<String, ComponentRegistry.Definition> definitions;
  private final Map<String, Element> fragments = new HashMap<>();
  private final String sourceName;
  private static final Set<String> PLACEMENT =
      Set.of("id", "x", "y", "width", "height", "anchor-x", "anchor-y", "class");

  private MenuTemplate(
      Element root, RenderEnvironment environment, ComponentRegistry registry, String sourceName)
      throws Exception {
    this.environment = environment;
    this.metrics = environment.font();
    this.root = root;
    this.sourceName = sourceName;
    this.definitions = new HashMap<>(registry.definitions());
    for (var child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (!(child instanceof Element e) || !e.getTagName().equals("dui-component")) continue;
      String name = e.getAttribute("name");
      Set<String> properties = new HashSet<>();
      if (!e.getAttribute("props").isBlank())
        for (String prop : e.getAttribute("props").split(",", -1))
          if (!properties.add(prop.strip()))
            throw new IllegalArgumentException("Duplicate property: " + prop);
      var definition =
          ComponentRegistry.builder()
              .template(name, properties, "<dui-fragment/>")
              .build()
              .definitions()
              .get(name);
      if (TAGS.contains(name) || definitions.putIfAbsent(name, definition) != null)
        throw new IllegalArgumentException("Duplicate component: " + name);
      fragments.put(name, e);
    }
    for (var entry : registry.definitions().entrySet()) {
      if (TAGS.contains(entry.getKey()))
        throw new IllegalArgumentException("Reserved component: " + entry.getKey());
      if (entry.getValue().template() != null) {
        var fragment = document(entry.getValue().template());
        if (!fragment.getTagName().equals("dui-fragment"))
          throw new IllegalArgumentException(
              "Component source needs a dui-fragment root: " + entry.getKey());
        fragments.put(entry.getKey(), fragment);
      }
    }
    collectStyles(root, "");
    for (var entry : fragments.entrySet()) {
      collectStyles(entry.getValue(), "__" + entry.getKey() + "_");
      long roots =
          elements(entry.getValue()).stream()
              .filter(e -> !e.getTagName().equals("dui-style"))
              .count();
      if (roots != 1)
        throw new IllegalArgumentException("Component needs one visual root: " + entry.getKey());
      for (var child : elements(entry.getValue())) validate(child, 1);
    }
    validate(root, 0);
  }

  private Set<String> styleProperties() {
    var result = new HashSet<>(STYLE_PROPS);
    definitions.values().forEach(d -> result.addAll(d.attributes()));
    environment
        .skins()
        .all()
        .values()
        .forEach(s -> result.addAll(s.properties().properties().keySet()));
    return result;
  }

  private static List<Element> elements(Element parent) {
    var result = new ArrayList<Element>();
    for (var child = parent.getFirstChild(); child != null; child = child.getNextSibling())
      if (child instanceof Element element) result.add(element);
    return result;
  }

  private void collectStyles(Element parent, String prefix) {
    var renames = new HashMap<String, String>();
    for (var e : elements(parent)) {
      if (!e.getTagName().equals("dui-style")) continue;
      String id = e.getAttribute("id"), name = prefix + id;
      if (id.isBlank() || styles.containsKey(name) || renames.putIfAbsent(id, name) != null)
        throw new IllegalArgumentException("Duplicate / empty style: " + id);
      Map<String, String> props = new HashMap<>();
      for (int i = 0; i < e.getAttributes().getLength(); i++) {
        var a = e.getAttributes().item(i);
        if (!a.getNodeName().equals("id")) {
          if (!styleProperties().contains(a.getNodeName()))
            throw new IllegalArgumentException("Invalid style property: " + a.getNodeName());
          props.put(a.getNodeName(), a.getNodeValue());
        }
      }
      styles.put(name, Map.copyOf(props));
    }
    if (!prefix.isEmpty()) renameClasses(parent, renames);
  }

  private static void renameClasses(Element parent, Map<String, String> renames) {
    if (parent.hasAttribute("class"))
      parent.setAttribute(
          "class",
          Arrays.stream(parent.getAttribute("class").strip().split("\\s+"))
              .map(name -> renames.getOrDefault(name, name))
              .collect(java.util.stream.Collectors.joining(" ")));
    elements(parent).forEach(e -> renameClasses(e, renames));
  }

  private static Element document(String xml) throws Exception {
    if (xml.length() > 128_000) throw new IllegalArgumentException("Template too large");
    var factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    return factory
        .newDocumentBuilder()
        .parse(new InputSource(new StringReader(xml)))
        .getDocumentElement();
  }

  public static MenuTemplate parse(
      String xml, RenderEnvironment environment, ComponentRegistry registry) throws Exception {
    return parse(xml, environment, registry, "<template>");
  }

  public static MenuTemplate parse(
      String xml, RenderEnvironment environment, ComponentRegistry registry, String sourceName)
      throws Exception {
    try {
      var root = document(xml);
      if (!root.getTagName().equals("dui-menu"))
        throw new IllegalArgumentException("Expected dui-menu");
      return new MenuTemplate(
          root,
          Objects.requireNonNull(environment),
          Objects.requireNonNull(registry),
          Objects.requireNonNull(sourceName));
    } catch (Exception e) {
      throw new IllegalArgumentException(sourceName + ": " + e.getMessage(), e);
    }
  }

  private void validate(Element e, int depth) {
    String tag = e.getTagName(), type = tag.startsWith("dui-") ? tag.substring(4) : "";
    var definition = definitions.get(type);
    if (depth > 20 || !tag.startsWith("dui-") || !TAGS.contains(type) && definition == null)
      throw new IllegalArgumentException("Unknown / nested component: " + tag);
    if (type.equals("component")) {
      if (depth != 1)
        throw new IllegalArgumentException("Component definitions must be direct menu children");
      for (int i = 0; i < e.getAttributes().getLength(); i++)
        if (!Set.of("name", "props").contains(e.getAttributes().item(i).getNodeName()))
          throw new IllegalArgumentException("Invalid component definition attribute");
      return;
    }
    if (type.equals("style") && (depth != 1 || e.hasChildNodes()))
      throw new IllegalArgumentException(
          "Styles must be empty direct children of menu or component");
    for (int i = 0; i < e.getAttributes().getLength(); i++) {
      String key = e.getAttributes().item(i).getNodeName();
      if (definition == null
          ? !ComponentSchemas.supports(tag.substring(4), key)
              && !environment
                  .skins()
                  .find(type)
                  .map(s -> s.properties().properties().containsKey(key))
                  .orElse(false)
          : !PLACEMENT.contains(key) && !definition.attributes().contains(key))
        throw new IllegalArgumentException("Unknown attribute " + key + " on " + tag);
    }
    for (var child : elements(e)) validate(child, depth + 1);
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

  private List<Node> expand(
      Element e,
      Map<String, Object> data,
      int[] budget,
      Map<String, List<Node>> outlets,
      int depth) {
    if (depth > 32) throw new IllegalArgumentException("Component expansion depth exceeded");
    if (--budget[0] < 0) throw new IllegalArgumentException("Expanded node limit");
    String type = e.getTagName().substring(4);
    if (type.equals("style") || type.equals("component")) return List.of();
    if (type.equals("outlet")) {
      String name = e.getAttribute("name");
      if (!outlets.containsKey(name))
        throw new IllegalArgumentException("Missing content outlet: " + name);
      var projected = outlets.get(name);
      budget[0] -= projected.stream().mapToInt(MenuTemplate::nodeCount).sum();
      if (budget[0] < 0)
        throw new IllegalArgumentException("Expanded node limit in outlet: " + name);
      return projected;
    }
    if (type.equals("content"))
      throw new IllegalArgumentException("Content must belong to a template component");
    if (fragments.containsKey(type)) {
      var properties = new HashMap<String, Object>();
      for (String key : definitions.get(type).attributes()) {
        if (!e.hasAttribute(key)) continue;
        String raw = e.getAttribute(key);
        var match = BIND.matcher(raw);
        properties.put(key, match.matches() ? lookup(data, match.group(1)) : bind(raw, data));
      }
      var strings = new HashMap<String, String>();
      properties.forEach((key, value) -> strings.put(key, String.valueOf(value)));
      definitions.get(type).schema().resolve(strings).forEach(properties::putIfAbsent);
      // ID is available for explicitly namespaced internal controls, without changing their
      // payloads.
      if (e.hasAttribute("id")) properties.put("id", bind(e.getAttribute("id"), data));
      var content = new HashMap<String, List<Node>>();
      var defaultContent = new ArrayList<Node>();
      for (var child : elements(e)) {
        if (child.getTagName().equals("dui-content")) {
          String name = child.getAttribute("name");
          if (name.isBlank()
              || content.putIfAbsent(name, children(child, data, budget, outlets, depth + 1))
                  != null)
            throw new IllegalArgumentException("Content needs a unique nonempty name");
        } else defaultContent.addAll(expand(child, data, budget, outlets, depth + 1));
      }
      if (content.containsKey("default") && !defaultContent.isEmpty())
        throw new IllegalArgumentException("Default content must be supplied once");
      content.putIfAbsent("default", List.copyOf(defaultContent));
      var scope = new HashMap<>(data);
      scope.put("props", Map.copyOf(properties));
      var expanded = children(fragments.get(type), scope, budget, content, depth + 1);
      if (expanded.size() != 1)
        throw new IllegalArgumentException("Component must expand to one root: " + type);
      var visual = expanded.getFirst();
      var props = new HashMap<>(visual.props());
      String callerClasses = bind(e.getAttribute("class"), data).strip();
      if (!callerClasses.isEmpty())
        for (String name : callerClasses.split("\\s+")) {
          var style = styles.get(name);
          if (style == null) throw new IllegalArgumentException("Unknown style: " + name);
          style.forEach((key, value) -> props.put(key, bind(value, data)));
        }
      for (String key : PLACEMENT)
        if (e.hasAttribute(key)) props.put(key, bind(e.getAttribute(key), data));
      return List.of(new Node(visual.type(), props, visual.children(), visual.values()));
    }
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
        nodes.addAll(children(e, scope, budget, outlets, depth + 1));
      }
      return nodes;
    }
    if (type.equals("if")) return children(e, data, budget, outlets, depth + 1);
    Map<String, String> props = new HashMap<>();
    String classes = bind(e.getAttribute("class"), data).strip();
    if (!classes.isEmpty())
      for (String name : classes.split("\\s+")) {
        var style = styles.get(name);
        if (style == null) throw new IllegalArgumentException("Unknown style: " + name);
        style.forEach((key, value) -> props.put(key, bind(value, data)));
      }
    var explicit = new HashMap<String, String>();
    var structured = new HashMap<String, Object>();
    var ownDefinition = definitions.get(type);
    for (int i = 0; i < e.getAttributes().getLength(); i++) {
      var a = e.getAttributes().item(i);
      if (ownDefinition != null && ownDefinition.contract().values().containsKey(a.getNodeName())) {
        var match = BIND.matcher(a.getNodeValue());
        structured.put(
            a.getNodeName(), match.matches() ? lookup(data, match.group(1)) : a.getNodeValue());
      } else explicit.put(a.getNodeName(), bind(a.getNodeValue(), data));
    }
    String state =
        "true".equals(explicit.get("locked"))
            ? "disabled"
            : "true".equals(explicit.get("active"))
                ? "active"
                : "true".equals(explicit.get("checked")) ? "selected" : "normal";
    var resolved = StyleResolver.resolve(props, StyleResolver.state(props, state), explicit);
    props.clear();
    props.putAll(resolved);
    var definition = definitions.get(type);
    var acceptedProps =
        definition == null
            ? environment
                .skins()
                .find(type)
                .map(s -> s.properties().resolve(props))
                .orElse(Map.copyOf(props))
            : definition.schema().resolve(props);
    return List.of(
        new Node(
            type,
            acceptedProps,
            children(e, data, budget, outlets, depth + 1),
            definition == null ? Map.of() : definition.contract().resolve(structured)));
  }

  private static int nodeCount(Node node) {
    return 1 + node.children().stream().mapToInt(MenuTemplate::nodeCount).sum();
  }

  private List<Node> children(
      Element e,
      Map<String, Object> data,
      int[] budget,
      Map<String, List<Node>> outlets,
      int depth) {
    List<Node> nodes = new ArrayList<>();
    for (var child = e.getFirstChild(); child != null; child = child.getNextSibling())
      if (child instanceof Element el) nodes.addAll(expand(el, data, budget, outlets, depth));
    return nodes;
  }

  public Canvas render(Map<String, Object> data) {
    return render(data, Map.of());
  }

  public Canvas render(Map<String, Object> data, Map<String, RasterImage> images) {
    return render(data, images, null);
  }

  public Canvas render(
      Map<String, Object> data, Map<String, RasterImage> images, ThemeTokens tokens) {
    Node node;
    try {
      node = expand(root, data, new int[] {512}, Map.of(), 0).getFirst();
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(sourceName + ": " + e.getMessage(), e);
    }
    node = resolveTokens(node, tokens == null ? environment.tokens() : tokens);
    Canvas canvas =
        new Canvas(
            node.b("compact") ? node.n("compact-width", node.n("width", -1)) : node.n("width", -1),
            node.b("compact")
                ? node.n("compact-height", node.n("height", -1))
                : node.n("height", -1),
            (tokens == null ? environment : environment.withTokens(tokens)).forData(data));
    canvas.effectLimit = node.n("effect-budget", ShaderInvocation.LIMIT);
    if (canvas.effectLimit < 1 || canvas.effectLimit > RendererProtocol.MAX_EFFECTS)
      throw new IllegalArgumentException("Effect budget 1.." + RendererProtocol.MAX_EFFECTS);
    if (!Set.of("hidden", "native").contains(node.s("focus-outline", "native")))
      throw new IllegalArgumentException("focus-outline must be hidden or native");
    canvas.animationStart = Long.parseLong(node.s("animation-start", "0"));
    canvas.motionEnabled = !node.s("motion", "true").equals("false");
    canvas.hideFocusOutline = !node.s("focus-outline", "native").equals("native");
    String background = node.s("background", "none");
    if (!background.equals("none"))
      canvas.rect(
          0, 0, canvas.width, canvas.height, StyleResolver.color(background, canvas.tokens()));
    if (canvas.hideFocusOutline) {
      String mask = node.s("focus-outline-color", background);
      if (mask.equals("none"))
        throw new IllegalArgumentException(
            "Hidden focus outline requires focus-outline-color or an explicit background");
      canvas.focusOutlineColor = StyleResolver.color(mask, canvas.tokens());
    }
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
              canvas.animationStart,
              !node.s("motion", "true").equals("false"),
              canvas.effects.stream().mapToInt(ShaderInvocation::lifetimeTicks).max().orElse(0));
    }
    return canvas;
  }

  private Node resolveTokens(Node n, ThemeTokens tokens) {
    var p = new HashMap<>(n.props());
    var definition = definitions.get(n.type());
    var schema =
        definition != null
            ? definition.schema()
            : environment
                .skins()
                .find(n.type())
                .map(WidgetSkinRegistry.Skin::properties)
                .orElse(new PropertySchema(Map.of()));
    for (var entry : schema.properties().entrySet())
      if (entry.getValue().type() == PropertySchema.Type.COLOR) {
        var value = p.get(entry.getKey());
        if (value != null && value.startsWith("$"))
          p.put(entry.getKey(), "#%06X".formatted(tokens.color(value.substring(1))));
      }
    p.replaceAll(
        (k, v) ->
            Set.of("padding", "padding-x", "padding-y", "gap").contains(k) && v.startsWith("$")
                ? Integer.toString(tokens.space(v.substring(1)))
                : (Set.of(
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
                                "highlight")
                            .contains(k)
                        && v.startsWith("$"))
                    ? "#%06X".formatted(tokens.color(v.substring(1)))
                    : v);
    return new Node(
        n.type(),
        p,
        n.children().stream().map(ch -> resolveTokens(ch, tokens)).toList(),
        n.values());
  }

  private int natural(Node n) {
    return natural(n, 480);
  }

  public Measure.Size measure(Node node, Measure.Constraints constraints) {
    var custom = definitions.get(node.type());
    if (custom != null && custom.measurer() != null && !node.props().containsKey("height")) {
      return constraints.constrain(
          custom
              .measurer()
              .measure(new Measure.Context(node, constraints, environment, this::measure)));
    }
    int w =
        node.props().containsKey("width") && !node.s("width", "").equals("fill")
            ? LayoutLength.resolve(node.s("width", "fill"), constraints.maxWidth())
            : constraints.maxWidth();
    return constraints.constrain(new Measure.Size(w, natural(node, w)));
  }

  private int natural(Node n, int availableWidth) {
    var custom = definitions.get(n.type());
    if (custom != null && custom.renderer() != null && !n.props().containsKey("height")) {
      if (custom.measurer() != null)
        return measure(n, Measure.Constraints.available(availableWidth, 360)).height();
      return custom.height();
    }
    if (n.props.containsKey("height") && n.s("height", "").matches("-?[0-9]+"))
      return n.n("height", 18);
    if ((n.type().equals("text") || n.type().equals("heading")) && n.props().containsKey("font")) {
      var face = RichText.require(environment.fonts(), n.s("font", ""));
      int lines =
          n.b("wrap")
              ? Math.max(
                  1,
                  GlyphFont.from(face)
                      .wrap(n.s("label", ""), Math.max(1, availableWidth), n.n("max-lines", 40))
                      .size())
              : 1;
      return (lines * face.lineHeight() + 8) / 9 * 9;
    }
    if (n.type().equals("column") || n.type().equals("panel") || n.type().equals("group"))
      return n.children.stream()
              .mapToInt(
                  ch ->
                      natural(
                          ch,
                          Math.max(0, availableWidth - n.n("padding-x", n.n("padding", 0)) * 2)))
              .sum()
          + Math.max(0, n.children.size() - 1) * n.n("gap", 0)
          + environment.skins().find(n.type()).map(sk -> sk.paddingY() * 2).orElse(0);
    return environment
        .skins()
        .find(n.type())
        .map(WidgetSkinRegistry.Skin::height)
        .orElseGet(
            () ->
                switch (n.type()) {
                  case "player-model" -> 216;
                  case "spacer" -> 9;
                  default -> 18;
                });
  }

  private static int color(Node n, String key, int fallback) {
    String value = n.s(key, "");
    if (value.isEmpty()) {
      if (fallback < 0) throw new IllegalArgumentException("Missing explicit " + key);
      return fallback;
    }
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

  private void draw(
      Canvas c,
      Node n,
      int x,
      int y,
      int w,
      int h,
      List<Runnable> overlays,
      boolean positioned,
      Map<String, RasterImage> images) {
    try {
      if (w < n.n("min-width", 0)
          || w > n.n("max-width", 480)
          || h < n.n("min-height", 0)
          || h > n.n("max-height", 360))
        throw new IllegalArgumentException("Layout constraints exceeded");
      var previous = c.style(n.props());
      var placement = c.enter(n.s("id", ""), x, y);
      try {
        drawNode(c, n, x, y, w, h, overlays, positioned, images);
      } finally {
        c.style(previous);
        c.leave(placement);
      }
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          sourceName
              + " <dui-"
              + n.type()
              + "> id='"
              + n.s("id", "")
              + "' at "
              + x
              + ","
              + y
              + " "
              + w
              + "x"
              + h
              + ": "
              + e.getMessage(),
          e);
    }
  }

  private static int anchor(String value, int available, int size, int inset, boolean vertical) {
    if (!(vertical ? Set.of("top", "center", "bottom") : Set.of("left", "center", "right"))
        .contains(value))
      throw new IllegalArgumentException(
          "Invalid " + (vertical ? "vertical" : "horizontal") + " anchor: " + value);
    return switch (value) {
      case "left", "top" -> inset;
      case "center" -> (vertical ? (available - size) / 18 * 9 : (available - size) / 2) + inset;
      case "right", "bottom" -> available - size - inset;
      default -> throw new IllegalArgumentException("Invalid anchor: " + value);
    };
  }

  private boolean isClipped(Node node) {
    var definition = definitions.get(node.type());
    return (node.type().equals("item")
            || definition != null
                && definition
                    .attributes()
                    .containsAll(Set.of("clip-x", "clip-y", "clip-width", "clip-height")))
        && Set.of("clip-x", "clip-y", "clip-width", "clip-height").stream()
            .allMatch(k -> !node.s(k, "").isBlank());
  }

  private void drawNode(
      Canvas c,
      Node n,
      int x,
      int y,
      int w,
      int h,
      List<Runnable> overlays,
      boolean positioned,
      Map<String, RasterImage> images) {
    boolean clippedItem = isClipped(n);
    if (w < 1 || h < 0 || !clippedItem && (x < 0 || y < 0 || x + w > c.width || y + h > c.height))
      throw new IllegalArgumentException("Component outside canvas: " + n.type);
    if (h == 0) return;
    if (n.type().equals("group")) {
      var transform =
          Transform2D.translation(
                  Double.parseDouble(n.s("translate-x", "0")),
                  Double.parseDouble(n.s("translate-y", "0")))
              .multiply(Transform2D.translation(x, y))
              .multiply(Transform2D.rotation(Double.parseDouble(n.s("rotate", "0"))))
              .multiply(
                  Transform2D.scale(
                      Double.parseDouble(n.s("scale-x", "1")),
                      Double.parseDouble(n.s("scale-y", "1"))))
              .multiply(Transform2D.translation(-x, -y));
      Scene.Rect clip =
          n.props().containsKey("clip-x")
              ? new Scene.Rect(
                  n.n("clip-x", 0), n.n("clip-y", 0), n.n("clip-width", w), n.n("clip-height", h))
              : null;
      var options = new SceneGroup(transform, clip, Double.parseDouble(n.s("opacity", "1")));
      final int gx = x, gy = y, gw = w, gh = h;
      c.group(
          n.s("id", "group"),
          options,
          childCanvas -> {
            var childOverlays = new ArrayList<Runnable>();
            draw(
                childCanvas,
                new Node("column", n.props(), n.children(), n.values()),
                gx,
                gy,
                gw,
                gh,
                childOverlays,
                positioned,
                images);
            childOverlays.forEach(Runnable::run);
          });
      return;
    }
    var custom = definitions.get(n.type());
    if (custom != null && custom.renderer() != null) {
      custom
          .renderer()
          .draw(
              new ComponentContext(
                  c,
                  n,
                  x,
                  y,
                  w,
                  h,
                  Map.copyOf(images),
                  new ComponentContext.ChildRenderer() {
                    public void draw(Node child, int cx, int cy, int cw, int ch) {
                      MenuTemplate.this.draw(c, child, cx, cy, cw, ch, overlays, true, images);
                    }

                    public Measure.Size measure(Node child, Measure.Constraints constraints) {
                      return MenuTemplate.this.measure(child, constraints);
                    }
                  },
                  overlays::add));
      return;
    }
    if (n.type.equals("layer")) {
      if (n.b("cover")) c.cover(n.s("id", "popup"), x, y, w, h);
      if (!n.s("dismiss", "").isBlank())
        c.hit(
            new Canvas.Hit(
                n.s("id", "popup") + "_dismiss",
                n.s("dismiss", ""),
                "",
                "Close options",
                0,
                0,
                c.width,
                c.height));
      int left = 0, top = 0, right = w, bottom = h;
      for (Node ch : n.children) {
        String dock = ch.s("dock", "");
        if (!dock.isBlank()) {
          int dw = right - left, dh = bottom - top, dx = left, dy = top;
          switch (dock) {
            case "top" -> {
              dh = natural(ch);
              top += dh;
            }
            case "bottom" -> {
              dh = natural(ch);
              dy = bottom - dh;
              bottom -= dh;
            }
            case "left" -> {
              dw = LayoutLength.resolve(ch.s("width", "fill"), right - left);
              left += dw;
            }
            case "right" -> {
              dw = LayoutLength.resolve(ch.s("width", "fill"), right - left);
              dx = right - dw;
              right -= dw;
            }
            case "fill" -> {}
            default -> throw new IllegalArgumentException("Unknown dock: " + dock);
          }
          if (left > right || top > bottom)
            throw new IllegalArgumentException("Dock overflow: " + ch.type());
          draw(c, ch, x + dx, y + dy, dw, dh, overlays, true, images);
          continue;
        }
        int ox = ch.n("x", 0), oy = ch.n("y", 0);
        int cw = LayoutLength.resolve(ch.s("width", "fill"), w - ox),
            chh = LayoutLength.resolve(ch.s("height", "" + natural(ch)), h - oy);
        int cx = anchor(ch.s("anchor-x", "left"), w, cw, ox, false),
            cy = anchor(ch.s("anchor-y", "top"), h, chh, oy, true);
        boolean childClip = isClipped(ch);
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
      c.rect(x, y, w, h, color(n, "fill", -1));
      return;
    }
    if (n.type.equals("surface")) {
      int bevel = n.n("bevel", 0);
      surface(c, x, y, w, h, color(n, "fill", -1), color(n, "border", bevel == 0 ? 0 : -1), bevel);
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
      var generic = Motion.from(n.props(), c.animationStart, c.motionEnabled);
      if (generic.isPresent()) c.motion(id, generic.orElseThrow());
      return;
    }
    if (w < 9 || h < 0 || !positioned && (y % 9 != 0 || h % 9 != 0))
      throw new IllegalArgumentException(
          "Invalid layout for " + n.type + ": " + x + "," + y + " " + w + "x" + h);
    if (h == 0) return;
    if (n.type.equals("dropdown")) {
      DropdownComponent.draw(c, n, x, y, w, h, overlays, environment.skins().require("dropdown"));
      return;
    }
    boolean container = Set.of("menu", "row", "column", "grid", "panel").contains(n.type);
    if (container) {
      var skin = environment.skins().find(n.type());
      if (skin.isPresent())
        skin.orElseThrow()
            .painter()
            .draw(
                new ComponentContext(
                    c,
                    n,
                    x,
                    y,
                    w,
                    h,
                    images,
                    (child, cx, cy, cw, ch) ->
                        draw(c, child, cx, cy, cw, ch, overlays, positioned, images),
                    overlays::add),
                "field");
      int
          pad =
              n.n(
                  "padding-x",
                  n.n("padding", skin.map(WidgetSkinRegistry.Skin::paddingX).orElse(0))),
          py = n.n("padding-y", skin.map(WidgetSkinRegistry.Skin::paddingY).orElse(0));
      x += pad;
      y += py;
      w -= pad * 2;
      h -= py * 2;
      int gap = n.n("gap", 0);
      final int contentWidth = w;
      if (n.type.equals("row") || n.type.equals("menu")) {
        final int rowWidth = w;
        int fixed =
            n.children.stream()
                .filter(ch -> ch.props.containsKey("width") && !ch.s("width", "").equals("fill"))
                .mapToInt(ch -> LayoutLength.resolve(ch.s("width", "0"), rowWidth))
                .sum();
        long flexible =
            n.children.stream()
                .filter(ch -> !ch.props.containsKey("width") || ch.s("width", "").equals("fill"))
                .count();
        int available = w - fixed - gap * Math.max(0, n.children.size() - 1);
        int left = (int) flexible;
        for (Node ch : n.children) {
          boolean auto = !ch.props.containsKey("width") || ch.s("width", "").equals("fill");
          int cw = auto ? available / left : LayoutLength.resolve(ch.s("width", "fill"), w);
          if (auto) {
            available -= cw;
            left--;
          }
          int chh = Math.min(h, natural(ch, cw)), cy = y;
          String alignment = n.s("cross-align", "stretch");
          if (alignment.equals("stretch")) chh = h;
          else if (alignment.equals("center")) cy = y + (h - chh) / 18 * 9;
          else if (alignment.equals("end")) cy = y + h - chh;
          else if (!alignment.equals("start"))
            throw new IllegalArgumentException("cross-align start/center/end/stretch");
          draw(c, ch, x, cy, cw, chh, overlays, positioned, images);
          x += cw + gap;
        }
      } else if (n.type.equals("grid")) {
        int columns = n.n("columns", 2);
        if (columns < 1 || columns > 16) throw new IllegalArgumentException("grid columns 1..16");
        if (n.children.stream()
            .anyMatch(
                ch ->
                    ch.props().containsKey("column-span") || ch.props().containsKey("row-span"))) {
          int rowHeight =
              n.n(
                  "cell-height",
                  n.children.stream()
                      .mapToInt(ch -> natural(ch, contentWidth / columns) / ch.n("row-span", 1))
                      .max()
                      .orElse(18));
          int strideY = rowHeight + gap, rows = (h + gap) / strideY, strideX = (w + gap) / columns;
          var used = new HashSet<Integer>();
          var cells = new ArrayList<GridLayout.Cell>();
          for (int i = 0; i < n.children.size(); i++) {
            var ch = n.children.get(i);
            int cs = ch.n("column-span", 1), rs = ch.n("row-span", 1);
            boolean placed = false;
            search:
            for (int rr = 0; rr < rows; rr++)
              for (int cc = 0; cc < columns; cc++) {
                if (cs < 1 || rs < 1 || cc + cs > columns || rr + rs > rows) continue;
                boolean free = true;
                for (int yy = rr; yy < rr + rs; yy++)
                  for (int xx = cc; xx < cc + cs; xx++)
                    if (used.contains(yy * columns + xx)) free = false;
                if (!free) continue;
                for (int yy = rr; yy < rr + rs; yy++)
                  for (int xx = cc; xx < cc + cs; xx++) used.add(yy * columns + xx);
                cells.add(new GridLayout.Cell("" + i, cc, rr, cs, rs));
                placed = true;
                break search;
              }
            if (!placed)
              throw new IllegalArgumentException(
                  "Spanning grid overflow: " + ch.s("id", ch.type()));
          }
          var boxes = GridLayout.place(cells, columns, rows, x, y, strideX, strideY, gap, gap);
          for (int i = 0; i < boxes.size(); i++) {
            var b = boxes.get(i);
            draw(
                c,
                n.children.get(i),
                b.x(),
                b.y(),
                b.width(),
                b.height(),
                overlays,
                positioned,
                images);
          }
          return;
        }
        int cw = (w - gap * (columns - 1)) / columns, cy = y, index = 0, rh = 0;
        for (Node ch : n.children) {
          int chh = natural(ch, cw);
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
                .mapToInt(ch -> natural(ch, contentWidth))
                .sum();
        long fill = n.children.stream().filter(ch -> ch.s("height", "").equals("fill")).count();
        int available = h - fixed - gap * Math.max(0, n.children.size() - 1), left = (int) fill;
        if (available < 0)
          throw new IllegalArgumentException(
              "Column overflow: "
                  + n.type
                  + " needs "
                  + (h - available)
                  + ", has "
                  + h
                  + "; children="
                  + n.children.stream().map(ch -> ch.type() + ":" + natural(ch)).toList());
        for (Node ch : n.children) {
          boolean auto = ch.s("height", "").equals("fill");
          int chh = auto ? (available / left / 9) * 9 : natural(ch, contentWidth);
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
    if (n.type.equals("spacer")) return;
    if (n.type.equals("player-model")) {
      c.playerModel(
          n.s("id", "player"),
          n.s("source", "viewer"),
          x,
          y,
          w,
          h,
          n.s("renderer", "dui:player"),
          n.n("facing", 0),
          !n.s("outer-layer", "true").equals("false"),
          !n.s("idle", "true").equals("false"));
      return;
    }
    if (environment.skins().find(n.type).isEmpty()) {
      switch (n.type) {
        case "text", "heading" ->
            TextLayout.draw(
                c,
                n.s("label", ""),
                x,
                y,
                w,
                h,
                n.s("align", "left"),
                n.b("wrap"),
                n.n("max-lines", Math.max(1, h / 9)),
                color(n, "color", -1));
        case "icon" -> c.icon(x, y, n.s("name", ""), color(n, "color", -1));
        case "head" -> {
          c.head(x, y, n.s("player", "self"), !n.s("hat", "true").equals("false"));
          ControlBehavior.basicHits(c, n, x, y, w, h);
        }
        case "divider" -> c.rect(x, y, w, h, color(n, "fill", -1));
        default -> throw new IllegalArgumentException("No consumer skin registered for " + n.type);
      }
      return;
    }
    var skin = environment.skins().require(n.type);
    var context =
        new ComponentContext(
            c,
            n,
            x,
            y,
            w,
            h,
            images,
            (child, cx, cy, cw, ch) -> draw(c, child, cx, cy, cw, ch, overlays, positioned, images),
            overlays::add);
    skin.painter().draw(context, "field");
    ControlBehavior.hits(context, skin);
  }
}
