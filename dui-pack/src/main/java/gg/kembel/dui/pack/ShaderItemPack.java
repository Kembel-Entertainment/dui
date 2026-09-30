package gg.kembel.dui.pack;

import com.google.gson.*;
import gg.kembel.dui.core.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;

/** 26.2 native model wrappers. The 48px frame carries binary layout data outside the 36px item. */
public final class ShaderItemPack {
  public static final int DATA_INDEX = 32;
  public static final int BURST_TICKS = 96;
  private static final Gson JSON = new Gson();

  private static byte[] shader(String ext) throws IOException {
    try (var in =
        ShaderItemPack.class.getResourceAsStream("/ui/shader/position_tex_color." + ext)) {
      String source = new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8);
      if (ext.equals("fsh"))
        try (var slots = ShaderItemPack.class.getResourceAsStream("/ui/shader/effects.glsl")) {
          source =
              source.replace(
                  "// EFFECT_FUNCTIONS",
                  new String(Objects.requireNonNull(slots).readAllBytes(), StandardCharsets.UTF_8));
        }
      return source.getBytes(StandardCharsets.UTF_8);
    }
  }

  public static void write(ZipOutputStream zip, VanillaAssets assets, Map<String, byte[]> additions)
      throws IOException {
    for (String ext : List.of("vsh", "fsh"))
      put(zip, "assets/minecraft/shaders/core/position_tex_color." + ext, shader(ext));
    var white = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    white.setRGB(0, 0, 0xFFFFFFFF);
    var clear = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    for (var entry : new TreeMap<>(Map.of("white", white, "clear", clear)).entrySet()) {
      var out = new ByteArrayOutputStream();
      ImageIO.write(entry.getValue(), "png", out);
      put(zip, "assets/dui/textures/item/" + entry.getKey() + ".png", out.toByteArray());
    }
    JsonArray elements = new JsonArray();
    // Only the corners establish bounds: a large transparent plane can occlude translucent models.
    elements.add(face(-16, -16, -15, -15, "clear", -1));
    elements.add(face(31, 31, 32, 32, "clear", -1));
    // Header, left to right at the top of the offscreen item texture: 101 / 010 / 110 / 001.
    for (int i = 0; i < 14; i++) elements.add(face(-16 + i * 3, 29, -13 + i * 3, 32, "white", i));
    // Bottom-edge cells and a spare top cell retain the 3px guard around the native crop.
    for (int i = 0; i < 16; i++)
      elements.add(face(-16 + i * 3, -16, -13 + i * 3, -13, "white", 14 + i));
    elements.add(face(26, 29, 29, 32, "white", 30));
    elements.add(face(29, 29, 32, 32, "white", 31));
    JsonObject marker =
        JsonParser.parseString(
                "{\"gui_light\":\"front\",\"textures\":{\"white\":\"dui:item/white\",\"clear\":\"dui:item/clear\",\"particle\":\"dui:item/white\"}}")
            .getAsJsonObject();
    marker.add("elements", elements);
    put(zip, "assets/dui/models/item/transport.json", JSON.toJson(marker));
    // Vanilla orders GUI draws by their original bounds, before our shader moves them.
    // A fully transparent, tall item intersects the canvas and establishes a later draw layer.
    var fence = marker.deepCopy();
    var corners = new JsonArray();
    corners.add(face(-16, -16, -15, -15, "clear", -1));
    corners.add(face(31, 31, 32, 32, "clear", -1));
    fence.add("elements", corners);
    put(zip, "assets/dui/models/item/layer_fence.json", JSON.toJson(fence));
    put(
        zip,
        "assets/dui/items/layer_fence.json",
        "{\"oversized_in_gui\":true,\"model\":{\"type\":\"minecraft:model\",\"model\":\"dui:item/layer_fence\",\"transformation\":{\"translation\":[0,-7.5,0],\"left_rotation\":[0,0,0,1],\"scale\":[1,16,1],\"right_rotation\":[0,0,0,1]}}}");
    // Procedural components use the otherwise empty 42x42 interior for parameter cells.
    // Native item wrappers keep their original crop and small transport unchanged.
    var effectMarker = marker.deepCopy();
    var effectElements = elements.deepCopy();
    for (int i = 0; i < ShaderEffect.LIMIT * ShaderEffect.CELLS; i++)
      effectElements.add(
          face(
              -13 + (i % 14) * 3,
              -13 + (i / 14) * 3,
              -10 + (i % 14) * 3,
              -10 + (i / 14) * 3,
              "white",
              32 + i));
    effectMarker.add("elements", effectElements);
    put(zip, "assets/dui/models/item/effect_transport.json", JSON.toJson(effectMarker));
    var definitions = assets.itemDefinitions();
    for (var entry : additions.entrySet())
      if (entry.getKey().matches("assets/[^/]+/items/.+\\.json")) {
        var parts = entry.getKey().split("/", 4);
        definitions.add(
            parts[1] + ":" + parts[3].substring(0, parts[3].length() - 5),
            JsonParser.parseString(new String(entry.getValue(), StandardCharsets.UTF_8)));
      }
    put(
        zip,
        "assets/dui/models/effect/panel.json",
        "{\"gui_light\":\"front\",\"textures\":{\"particle\":\"dui:item/clear\"},\"elements\":[]}");
    definitions.add(
        "dui:effect/panel",
        JsonParser.parseString(
            "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"dui:effect/panel\"}}"));
    for (var entry : definitions.entrySet()) {
      var definition = entry.getValue().getAsJsonObject().deepCopy();
      var root = new JsonObject();
      root.addProperty("type", "minecraft:composite");
      var children = new JsonArray();
      var item = new JsonObject();
      item.addProperty("type", "minecraft:composite");
      var original = new JsonArray();
      original.add(definition.get("model"));
      item.add("models", original);
      item.add(
          "transformation",
          JsonParser.parseString(
              "{\"translation\":[-0.625,-0.625,-0.625],\"left_rotation\":[0,0,0,1],\"scale\":[2.25,2.25,2.25],\"right_rotation\":[0,0,0,1]}"));
      children.add(item);
      var carrier = new JsonObject();
      carrier.addProperty("type", "minecraft:model");
      carrier.addProperty(
          "model",
          entry.getKey().equals("dui:effect/panel")
              ? "dui:item/effect_transport"
              : "dui:item/transport");
      var tints = new JsonArray();
      for (int color : new int[] {0xFF00FF, 0x00FF00, 0xFFFF00, 0x0000FF})
        tints.add(
            JsonParser.parseString("{\"type\":\"minecraft:constant\",\"value\":" + color + "}"));
      for (int i = 0;
          i
              < 28
                  + (entry.getKey().equals("dui:effect/panel")
                      ? ShaderEffect.LIMIT * ShaderEffect.CELLS
                      : 0);
          i++)
        tints.add(
            JsonParser.parseString(
                "{\"type\":\"minecraft:custom_model_data\",\"index\":"
                    + (DATA_INDEX + i)
                    + ",\"default\":0}"));
      carrier.add("tints", tints);
      children.add(carrier);
      root.add("models", children);
      definition.add("model", root);
      definition.addProperty("oversized_in_gui", true);
      put(
          zip,
          "assets/dui/items/live/" + entry.getKey().replace(':', '/') + ".json",
          JSON.toJson(definition));
    }
  }

  private static JsonObject face(int x0, int y0, int x1, int y1, String texture, int tint) {
    var element = new JsonObject();
    element.add("from", JSON.toJsonTree(new int[] {x0, y0, 8}));
    element.add("to", JSON.toJsonTree(new int[] {x1, y1, 8}));
    element.addProperty("shade", false);
    element.addProperty("light_emission", 15);
    var face = new JsonObject();
    face.addProperty("texture", "#" + texture);
    face.add("uv", JSON.toJsonTree(new int[] {0, 0, 16, 16}));
    if (tint >= 0) face.addProperty("tintindex", tint);
    var faces = new JsonObject();
    faces.add("south", face);
    element.add("faces", faces);
    return element;
  }

  private static void put(ZipOutputStream zip, String name, String text) throws IOException {
    put(zip, name, text.getBytes(StandardCharsets.UTF_8));
  }

  private static void put(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
    var e = new ZipEntry(name);
    e.setTime(0);
    zip.putNextEntry(e);
    zip.write(bytes);
    zip.closeEntry();
  }
}
