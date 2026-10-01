package gg.kembel.dui.pack;

import com.google.gson.*;
import gg.kembel.dui.core.PlayerModelCodec;
import java.util.*;

/** Flat vanilla texture carriers; the GPU constructs the cuboids. No player-specific assets. */
public final class PlayerArmorPack {
  private PlayerArmorPack() {}

  public static final List<String> MATERIALS =
      List.of(
          "leather", "chainmail", "copper", "iron", "gold", "diamond", "netherite", "turtle_scute");

  public static Set<String> models() {
    var out = new TreeSet<String>();
    for (var material : MATERIALS) {
      out.add(PlayerModelCodec.armorModel(material, false));
      if (!material.equals("turtle_scute")) out.add(PlayerModelCodec.armorModel(material, true));
    }
    return Set.copyOf(out);
  }

  public static JsonObject atlas() {
    var root = new JsonObject();
    var sources = new JsonArray();
    for (String material : MATERIALS)
      for (String layer : List.of("humanoid", "humanoid_leggings")) {
        if (material.equals("turtle_scute") && layer.equals("humanoid_leggings")) continue;
        for (String suffix : material.equals("leather") ? List.of("", "_overlay") : List.of("")) {
          var source = new JsonObject();
          source.addProperty("type", "minecraft:single");
          source.addProperty(
              "resource", "minecraft:entity/equipment/" + layer + "/" + material + suffix);
          sources.add(source);
        }
      }
    root.add("sources", sources);
    return root;
  }

  public static JsonObject model(String id) {
    String name = id.substring(id.lastIndexOf('/') + 1),
        layer = name.endsWith("_leggings") ? "humanoid_leggings" : "humanoid",
        material = name.replace("_leggings", "");
    var root = new JsonObject();
    root.addProperty("gui_light", "front");
    var textures = new JsonObject();
    textures.addProperty("particle", "#armor");
    textures.addProperty("armor", "minecraft:entity/equipment/" + layer + "/" + material);
    var elements = new JsonArray();
    elements.add(plane("#armor", 8, material.equals("leather") ? 0 : -1));
    if (material.equals("leather")) {
      textures.addProperty("overlay", "minecraft:entity/equipment/" + layer + "/leather_overlay");
      elements.add(plane("#overlay", 8.01, -1));
    }
    root.add("textures", textures);
    root.add("elements", elements);
    return root;
  }

  private static JsonObject plane(String texture, double z, int tint) {
    var element =
        JsonParser.parseString(
                "{\"from\":[0,0,"
                    + z
                    + "],\"to\":[16,16,"
                    + z
                    + "],\"shade\":false,\"light_emission\":15,\"faces\":{\"south\":{\"uv\":[0,0,16,16],\"texture\":\""
                    + texture
                    + "\"}}}")
            .getAsJsonObject();
    if (tint >= 0)
      element.getAsJsonObject("faces").getAsJsonObject("south").addProperty("tintindex", tint);
    return element;
  }

  public static JsonObject definition(String id) {
    var root = new JsonObject();
    var model = new JsonObject();
    model.addProperty("type", "minecraft:model");
    model.addProperty("model", id);
    if (id.contains("/leather"))
      model.add(
          "tints", JsonParser.parseString("[{\"type\":\"minecraft:dye\",\"default\":10511680}]"));
    root.add("model", model);
    return root;
  }
}
