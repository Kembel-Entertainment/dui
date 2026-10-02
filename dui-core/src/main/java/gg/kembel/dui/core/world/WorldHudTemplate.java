package gg.kembel.dui.core.world;

import gg.kembel.dui.core.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

/**
 * Screen anchors around ordinary dui-menu templates; bindings, styles, fragments and flow layout
 * are shared.
 */
public final class WorldHudTemplate {
  private record Surface(Map<String, String> props, MenuTemplate template) {}

  private static final Pattern BIND = Pattern.compile("\\{\\{([a-zA-Z_][a-zA-Z_0-9.-]*)}}");
  private static final Set<String> PROPS =
      Set.of("anchor-x", "anchor-y", "offset-x", "offset-y", "opacity", "visible");
  private final String source;
  private final String nativeHud, nativeColor;
  private final List<Surface> surfaces;

  private WorldHudTemplate(String source, String nativeHud, String color, List<Surface> surfaces) {
    this.source = source;
    this.nativeHud = nativeHud;
    nativeColor = color;
    this.surfaces = List.copyOf(surfaces);
  }

  public static WorldHudTemplate parse(String xml, RenderEnvironment environment) {
    return parse(xml, environment, ComponentRegistry.EMPTY, "<world-hud>");
  }

  public static WorldHudTemplate parse(
      String xml, RenderEnvironment environment, ComponentRegistry registry, String source) {
    try {
      if (xml.length() > 128000) throw new IllegalArgumentException("Template too large");
      var factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      factory.setXIncludeAware(false);
      factory.setExpandEntityReferences(false);
      var root =
          factory
              .newDocumentBuilder()
              .parse(new InputSource(new StringReader(xml)))
              .getDocumentElement();
      if (!root.getTagName().equals("dui-hud"))
        throw new IllegalArgumentException("Expected dui-hud");
      var props = attributes(root, Set.of("native-hud", "native-hud-color"));
      var transformerFactory = TransformerFactory.newInstance();
      transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
      var transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      var surfaces = new ArrayList<Surface>();
      for (var node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
        if (!(node instanceof Element e)) continue;
        if (!e.getTagName().equals("dui-surface"))
          throw new IllegalArgumentException("HUD children must be dui-surface");
        var placement = attributes(e, PROPS);
        Element menu = null;
        for (var child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
          if (!(child instanceof Element element)) continue;
          if (menu != null || !element.getTagName().equals("dui-menu"))
            throw new IllegalArgumentException("Surface needs exactly one dui-menu");
          menu = element;
        }
        if (menu == null) throw new IllegalArgumentException("Surface needs a dui-menu");
        var writer = new StringWriter();
        transformer.transform(new DOMSource(menu), new StreamResult(writer));
        surfaces.add(
            new Surface(
                placement,
                MenuTemplate.parse(
                    writer.toString(),
                    environment,
                    registry,
                    source + " surface " + surfaces.size())));
      }
      if (surfaces.size() > 32) throw new IllegalArgumentException("HUD has more than 32 surfaces");
      return new WorldHudTemplate(
          source,
          props.getOrDefault("native-hud", "keep"),
          props.getOrDefault("native-hud-color", ""),
          surfaces);
    } catch (Exception e) {
      throw new IllegalArgumentException(source + ": " + e.getMessage(), e);
    }
  }

  private static Map<String, String> attributes(Element element, Set<String> allowed) {
    var result = new HashMap<String, String>();
    for (int i = 0; i < element.getAttributes().getLength(); i++) {
      var a = element.getAttributes().item(i);
      if (!allowed.contains(a.getNodeName()))
        throw new IllegalArgumentException("Unknown HUD attribute: " + a.getNodeName());
      result.put(a.getNodeName(), a.getNodeValue());
    }
    return Map.copyOf(result);
  }

  private static String bind(String input, Map<String, Object> data) {
    var matcher = BIND.matcher(input);
    var result = new StringBuilder();
    while (matcher.find()) {
      Object value = data;
      for (String part : matcher.group(1).split("\\.")) {
        if (!(value instanceof Map<?, ?> map) || !map.containsKey(part))
          throw new IllegalArgumentException("Missing binding: " + matcher.group(1));
        value = map.get(part);
      }
      matcher.appendReplacement(result, Matcher.quoteReplacement(String.valueOf(value)));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  public WorldHud render(Map<String, Object> data) {
    return render(data, Map.of(), null);
  }

  public WorldHud render(
      Map<String, Object> data, Map<String, RasterImage> images, ThemeTokens theme) {
    try {
      String nativeMode = bind(nativeHud, data), color = bind(nativeColor, data);
      if (nativeMode.equals("keep") && color.isBlank()) color = "#000000";
      if (!Set.of("cover", "keep").contains(nativeMode) || !color.matches("#[0-9A-Fa-f]{6}"))
        throw new IllegalArgumentException(
            "native-hud must be cover/keep; native-hud-color must be #RRGGBB");
      var result = new ArrayList<WorldHud.Surface>();
      for (var surface : surfaces) {
        var p = new HashMap<String, String>();
        surface.props().forEach((key, value) -> p.put(key, bind(value, data)));
        String visible = p.getOrDefault("visible", "true");
        if (!Set.of("true", "false").contains(visible))
          throw new IllegalArgumentException("visible must be true/false");
        if (visible.equals("false")) continue;
        result.add(
            WorldHud.Surface.of(
                surface.template().render(data, images, theme),
                WorldHud.Horizontal.valueOf(
                    p.getOrDefault("anchor-x", "center").toUpperCase(Locale.ROOT)),
                WorldHud.Vertical.valueOf(
                    p.getOrDefault("anchor-y", "top").toUpperCase(Locale.ROOT)),
                Integer.parseInt(p.getOrDefault("offset-x", "0")),
                Integer.parseInt(p.getOrDefault("offset-y", "0")),
                Double.parseDouble(p.getOrDefault("opacity", "1"))));
      }
      return new WorldHud(
          nativeMode.equals("cover"), Integer.parseInt(color.substring(1), 16), result);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(source + ": " + e.getMessage(), e);
    }
  }
}
