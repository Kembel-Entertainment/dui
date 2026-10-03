package gg.kembel.dui.pack;

import com.google.gson.*;
import gg.kembel.dui.core.world.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;

/** Optional map pack contribution. Application illustrations stay in the owning namespace. */
public final class WorldMapPack {
  private WorldMapPack() {}

  static final Gson JSON = new GsonBuilder().disableHtmlEscaping().create();

  public static PackContribution contribution(
      String owner, WorldMapDefinition map, Map<String, BufferedImage> images) throws IOException {
    if (!map.id().startsWith(owner + ":")) throw new IllegalArgumentException("Map owner mismatch");
    var resources = new TreeMap<String, byte[]>();
    var names = new TreeSet<String>();
    map.layers().forEach(layer -> names.add(layer.image()));
    for (String name : names) {
      var image = Objects.requireNonNull(images.get(name), "Missing image: " + name);
      if (image.getWidth() > 254
          || image.getHeight() > 254
          || image.getWidth() < 1
          || image.getHeight() < 1)
        throw new IllegalArgumentException(
            "Map images must fit the 256-pixel font atlas with a 1-pixel border; tile larger"
                + " images");
      resources.put(path(map, "images/" + name + ".png"), png(image));
    }
    return new PackContribution(owner, resources, List.of(), List.of(map));
  }

  /** Splits a divisible atlas into original-sized tile images and map-space layers. */
  public record Tiles(List<WorldMapDefinition.Layer> layers, Map<String, BufferedImage> images) {}

  public static Tiles tiles(BufferedImage image, int tileWidth, int tileHeight) {
    if (tileWidth < 1
        || tileHeight < 1
        || tileWidth > 254
        || tileHeight > 254
        || image.getWidth() % tileWidth != 0
        || image.getHeight() % tileHeight != 0)
      throw new IllegalArgumentException("Invalid map tile dimensions");
    var layers = new ArrayList<WorldMapDefinition.Layer>();
    var images = new TreeMap<String, BufferedImage>();
    for (int y = 0; y < image.getHeight(); y += tileHeight)
      for (int x = 0; x < image.getWidth(); x += tileWidth) {
        String id = "tile_" + layers.size();
        layers.add(
            new WorldMapDefinition.Layer(
                id,
                id,
                x + tileWidth / 2.0,
                y + tileHeight / 2.0,
                tileWidth,
                tileHeight,
                WorldMapDefinition.Space.MAP,
                .9,
                1,
                1,
                0));
        var copy = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_ARGB);
        copy.setRGB(
            0,
            0,
            tileWidth,
            tileHeight,
            image.getRGB(x, y, tileWidth, tileHeight, null, 0, tileWidth),
            0,
            tileWidth);
        images.put(id, copy);
      }
    return new Tiles(List.copyOf(layers), Collections.unmodifiableMap(images));
  }

  record Result(
      Map<String, WorldMapMetadata> maps,
      Map<String, Integer> legendMetrics,
      Map<String, byte[]> generated,
      String geometry) {
    boolean enabled() {
      return !maps.isEmpty();
    }
  }

  static Result prepare(
      VanillaAssets vanilla, Map<String, byte[]> resources, List<WorldMapDefinition> definitions)
      throws IOException {
    return prepare(vanilla, resources, definitions, List.of(), Map.of());
  }

  static Result prepare(
      VanillaAssets vanilla,
      Map<String, byte[]> resources,
      List<WorldMapDefinition> definitions,
      List<PackContribution.Glyph> contributedGlyphs,
      Map<String, gg.kembel.dui.core.GlyphBinding> glyphBindings)
      throws IOException {
    var maps = new TreeMap<String, WorldMapMetadata>();
    var generated = new TreeMap<String, byte[]>();
    var sorted = definitions.stream().sorted(Comparator.comparing(WorldMapDefinition::id)).toList();
    if (sorted.size() > WorldMapProtocol.MAX_MAPS)
      throw new IllegalArgumentException("Too many world maps");
    StringBuilder geometry =
        new StringBuilder("// Derived from the same definitions consumed by Paper.\n");
    geometry.append(
        "bool duiMapDefinition(int m,out vec2 size,out float pixels,out float minimum,out float"
            + " step,out vec4 opening,out int easing){\n");
    for (int i = 0; i < sorted.size(); i++) {
      var d = sorted.get(i);
      geometry.append(
          String.format(
              Locale.ROOT,
              "if(m==%d){size=vec2(%d,%d);pixels=%.8f;minimum=%.8f;step=%.8f;opening=vec4(%.8f,%.8f,%.8f,%.8f);easing=%d;return"
                  + " true;}\n",
              i,
              d.width(),
              d.height(),
              d.pixelsPerDegree(),
              d.visibleMin(),
              d.visibleStep(),
              (double) d.opening().ticks(),
              d.opening().scaleFrom(),
              d.opening().opacityFrom(),
              d.opening().opacityTo(),
              d.opening().easing().ordinal()));
      var glyphs = new TreeMap<String, WorldMapMetadata.Glyph>();
      var providers = new JsonArray();
      var advances = new JsonObject();
      for (int bit = 0; bit <= 10; bit++) {
        advances.addProperty(Character.toString(0xE800 + bit), 1 << bit);
        advances.addProperty(Character.toString(0xE900 + bit), -(1 << bit));
      }
      providers.add(JSON.toJsonTree(Map.of("type", "space", "advances", advances)));
      for (var layer : d.layers())
        if (!glyphs.containsKey(layer.image())) {
          var input = resources.get(path(d, "images/" + layer.image() + ".png"));
          if (input == null) throw new IOException("Missing map image: " + layer.image());
          var original = ImageIO.read(new ByteArrayInputStream(input));
          if (original == null || original.getWidth() > 254 || original.getHeight() > 254)
            throw new IOException("Invalid map image");
          var padded = stamp(original, i);
          int character = 0xE000 + glyphs.size();
          String texture = key(d, "stamped/" + layer.image() + ".png");
          generated.put(path(d, "stamped/" + layer.image() + ".png"), png(padded));
          providers.add(
              JSON.toJsonTree(
                  Map.of(
                      "type",
                      "bitmap",
                      "file",
                      texture,
                      "height",
                      padded.getHeight(),
                      "ascent",
                      padded.getHeight(),
                      "chars",
                      List.of(Character.toString(character)))));
          glyphs.put(layer.image(), new WorldMapMetadata.Glyph(character, padded.getWidth() + 1));
        }
      for (int index = 0; index < d.layers().size(); index++) {
        var layer = d.layers().get(index);
        var input =
            ImageIO.read(
                new ByteArrayInputStream(
                    resources.get(path(d, "images/" + layer.image() + ".png"))));
        var padded = stamp(input, i);
        int width = padded.getWidth(), height = padded.getHeight();
        for (int corner = 0; corner < 4; corner++)
          padded.setRGB(
              (corner & 1) == 0 ? 0 : width - 1,
              (corner & 2) == 0 ? 0 : height - 1,
              0xff000000
                  | (WorldMapProtocol.DYNAMIC_STAMP_COLOR << 16)
                  | (((i << 1) | (corner & 1)) << 8)
                  | (index << 1)
                  | ((corner >> 1) & 1));
        int character = 0xE000 + glyphs.size();
        String texture = key(d, "dynamic/" + layer.id() + ".png");
        generated.put(path(d, "dynamic/" + layer.id() + ".png"), png(padded));
        providers.add(
            JSON.toJsonTree(
                Map.of(
                    "type",
                    "bitmap",
                    "file",
                    texture,
                    "height",
                    height,
                    "ascent",
                    height,
                    "chars",
                    List.of(Character.toString(character)))));
        glyphs.put("@runtime/" + layer.id(), new WorldMapMetadata.Glyph(character, width + 1));
      }
      String font = d.id().split(":", 2)[0] + ":world-map/" + d.id().split(":", 2)[1] + "/map";
      generated.put(
          "assets/" + font.replace(":", "/font/") + ".json",
          encode(Map.of("providers", providers)).getBytes(StandardCharsets.UTF_8));
      var meta = new WorldMapMetadata(d, font, glyphs, d.geometryHash(), WorldMapProtocol.SHA256);
      if (maps.putIfAbsent(d.id(), meta) != null)
        throw new IllegalArgumentException("Duplicate map id: " + d.id());
    }
    geometry.append(
        "return false;}\n"
            + "bool duiMapLayer(int m,int i,out vec4 rect,out int space,out float depth,out vec3"
            + " alpha){\n");
    for (int m = 0; m < sorted.size(); m++) {
      var d = sorted.get(m);
      geometry.append("if(m==").append(m).append("){\n");
      for (int i = 0; i < d.layers().size(); i++) {
        var layer = d.layers().get(i);
        geometry.append(
            String.format(
                Locale.ROOT,
                "if(i==%d){rect=vec4(%.8f,%.8f,%.8f,%.8f);space=%d;depth=%.8f;alpha=vec3(%.8f,%.8f,%.8f);return"
                    + " true;}\n",
                i,
                layer.x(),
                layer.y(),
                layer.width(),
                layer.height(),
                layer.space().ordinal(),
                layer.depth(),
                layer.opacityMin(),
                layer.opacityMax(),
                layer.pulseRadiansPerTick()));
      }
      geometry.append("return false;}\n");
    }
    geometry.append("return false;}\n");
    Map<String, Integer> legend =
        WorldMapLegendPack.generate(vanilla, generated, contributedGlyphs, glyphBindings);
    { // Shared HUD and inert geometry also support a video-only application.
      generated.put(
          "assets/dui/shaders/include/world-map-geometry.glsl",
          geometry.toString().getBytes(StandardCharsets.UTF_8));
      for (String part : List.of("scene", "hud", "protocol"))
        try (var in =
            WorldMapPack.class.getResourceAsStream("/ui/shader/world-map-" + part + ".glsl")) {
          generated.put(
              "assets/dui/shaders/include/world-map-" + part + ".glsl",
              Objects.requireNonNull(in).readAllBytes());
        }
    }
    for (String name : generated.keySet())
      if (resources.containsKey(name))
        throw new IllegalArgumentException("Generated map asset collision: " + name);
    return new Result(Collections.unmodifiableMap(maps), legend, generated, geometry.toString());
  }

  static String encode(Object object) {
    return JSON.toJson(canonical(JSON.toJsonTree(object)));
  }

  private static JsonElement canonical(JsonElement value) {
    if (value.isJsonObject()) {
      var result = new JsonObject();
      value.getAsJsonObject().entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(e -> result.add(e.getKey(), canonical(e.getValue())));
      return result;
    }
    if (value.isJsonArray()) {
      var result = new JsonArray();
      value.getAsJsonArray().forEach(e -> result.add(canonical(e)));
      return result;
    }
    return value;
  }

  static String path(WorldMapDefinition d, String file) {
    return "assets/" + key(d, file).replace(":", "/textures/");
  }

  static String key(WorldMapDefinition d, String file) {
    return d.id().replace(":", ":world-map/") + "/" + file;
  }

  static byte[] png(BufferedImage image) throws IOException {
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(image, "png", bytes);
    return bytes.toByteArray();
  }

  static BufferedImage stamp(BufferedImage input, int map) {
    int w = input.getWidth(), h = input.getHeight();
    var padded = new BufferedImage(w + 2, h + 2, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < h + 2; y++)
      for (int x = 0; x < w + 2; x++)
        padded.setRGB(x, y, input.getRGB(Math.clamp(x - 1, 0, w - 1), Math.clamp(y - 1, 0, h - 1)));
    for (int y = 0; y < 2; y++)
      for (int x = 0; x < 2; x++)
        padded.setRGB(
            x * (w + 1),
            y * (h + 1),
            0xFF000000
                | (WorldMapProtocol.WORLD_COLOR << 16)
                | ((128 + map * 2 + x) << 8)
                | (128 + y));
    return padded;
  }
}
