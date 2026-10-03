package gg.kembel.dui.core.video;

import gg.kembel.dui.core.*;
import gg.kembel.dui.core.world.*;
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

/** A consumer template declares a dynamic video viewport and an ordinary dui HUD. */
public final class VideoSurfaceTemplate {
  public record Presentation(VideoSurfaceSpec specification, WorldHud hud) {}
  private final Map<String, String> properties;
  private final WorldHudTemplate hud;
  private final String source;
  private static final Pattern BIND = Pattern.compile("\\{\\{([a-zA-Z_][a-zA-Z_0-9.-]*)}}");
  private static final Set<String> ATTRIBUTES = Set.of("width", "height", "format", "fps",
      "left", "top", "right", "bottom", "max-tiles", "bytes-per-second", "background", "scale");
  private VideoSurfaceTemplate(Map<String, String> properties, WorldHudTemplate hud, String source) {
    this.properties = Map.copyOf(properties); this.hud = hud; this.source = source;
  }
  public static VideoSurfaceTemplate parse(String xml, RenderEnvironment environment,
      ComponentRegistry components, String source) {
    try {
      if (xml.length() > 128000) throw new IllegalArgumentException("Template too large");
      var f = DocumentBuilderFactory.newInstance();
      f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      f.setXIncludeAware(false); f.setExpandEntityReferences(false);
      var root = f.newDocumentBuilder().parse(new InputSource(new StringReader(xml))).getDocumentElement();
      if (!root.getTagName().equals("dui-video")) throw new IllegalArgumentException("Expected dui-video");
      var properties = new HashMap<String, String>();
      for (int i = 0; i < root.getAttributes().getLength(); i++) {
        var a = root.getAttributes().item(i);
        if (!ATTRIBUTES.contains(a.getNodeName())) throw new IllegalArgumentException("Unknown video attribute " + a.getNodeName());
        properties.put(a.getNodeName(), a.getNodeValue());
      }
      WorldHudTemplate hud = null;
      for (var n = root.getFirstChild(); n != null; n = n.getNextSibling()) if (n instanceof Element e) {
        if (!e.getTagName().equals("dui-hud") || hud != null) throw new IllegalArgumentException("Expected one dui-hud");
        var tf = TransformerFactory.newInstance();
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        var out = new StringWriter(); tf.newTransformer().transform(new DOMSource(e), new StreamResult(out));
        hud = WorldHudTemplate.parse(out.toString(), environment, components, source + " HUD");
      }
      return new VideoSurfaceTemplate(properties, hud, source);
    } catch (Exception e) { throw new IllegalArgumentException(source + ": " + e.getMessage(), e); }
  }
  public Presentation render(Map<String, Object> data) {
    try {
      var p = new HashMap<String, String>();
      properties.forEach((k, v) -> {
        var m = BIND.matcher(v); var out = new StringBuilder();
        while (m.find()) {
          Object value = data;
          for (String part : m.group(1).split("\\.")) {
            if (!(value instanceof Map<?, ?> map) || !map.containsKey(part)) throw new IllegalArgumentException("Missing binding " + m.group(1));
            value = map.get(part);
          }
          m.appendReplacement(out, Matcher.quoteReplacement(String.valueOf(value)));
        }
        m.appendTail(out); p.put(k, out.toString());
      });
      String scale = p.getOrDefault("scale", "fit"), background = p.getOrDefault("background", "#000000");
      if (!Set.of("fit", "integer").contains(scale) || !background.matches("#[0-9A-Fa-f]{6}")) throw new IllegalArgumentException("Invalid scale/background");
      var spec = new VideoSurfaceSpec(Integer.parseInt(p.get("width")), Integer.parseInt(p.get("height")),
          PixelFormat.valueOf(p.get("format").toUpperCase(Locale.ROOT)), Double.parseDouble(p.get("fps")),
          new VideoSurfaceSpec.Viewport(Double.parseDouble(p.getOrDefault("left", "0")), Double.parseDouble(p.getOrDefault("top", "0")),
              Double.parseDouble(p.getOrDefault("right", "1")), Double.parseDouble(p.getOrDefault("bottom", "1"))),
          new VideoSurfaceSpec.Budget(Integer.parseInt(p.get("max-tiles")), Long.parseLong(p.get("bytes-per-second"))),
          Integer.parseInt(background.substring(1), 16), scale.equals("integer"));
      return new Presentation(spec, hud == null ? WorldHud.EMPTY : hud.render(data));
    } catch (RuntimeException e) { throw new IllegalArgumentException(source + ": " + e.getMessage(), e); }
  }
}
