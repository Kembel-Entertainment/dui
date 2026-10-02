package gg.kembel.dui.pack;

import java.util.*;

/** Trusted build-time package contributions. Runtime templates cannot inject shader code. */
public record PackContribution(
    String owner,
    Map<String, byte[]> resources,
    List<Shader> shaders,
    List<gg.kembel.dui.core.world.WorldMapDefinition> worldMaps,
    Map<String, String> shaderModules,
    List<Glyph> glyphs,
    List<gg.kembel.dui.core.BitmapFont> fonts,
    Set<String> features,
    List<gg.kembel.dui.core.PlayerRenderSpec> playerRenderers) {
  public PackContribution(
      String owner,
      Map<String, byte[]> resources,
      List<Shader> shaders,
      List<gg.kembel.dui.core.world.WorldMapDefinition> maps,
      Map<String, String> modules,
      List<Glyph> glyphs,
      List<gg.kembel.dui.core.BitmapFont> fonts,
      Set<String> features) {
    this(owner, resources, shaders, maps, modules, glyphs, fonts, features, List.of());
  }

  public PackContribution(
      String owner,
      Map<String, byte[]> resources,
      List<Shader> shaders,
      List<gg.kembel.dui.core.world.WorldMapDefinition> maps,
      Map<String, String> modules,
      List<Glyph> glyphs) {
    this(owner, resources, shaders, maps, modules, glyphs, List.of(), Set.of());
  }

  public PackContribution(String owner, Map<String, byte[]> resources, List<Shader> shaders) {
    this(owner, resources, shaders, List.of(), Map.of(), List.of());
  }

  public PackContribution(
      String owner,
      Map<String, byte[]> resources,
      List<Shader> shaders,
      List<gg.kembel.dui.core.world.WorldMapDefinition> maps) {
    this(owner, resources, shaders, maps, Map.of(), List.of());
  }

  public record Glyph(gg.kembel.dui.core.GlyphSpec specification, byte[] png) {
    public Glyph {
      Objects.requireNonNull(specification);
      png = png.clone();
    }

    public byte[] png() {
      return png.clone();
    }
  }

  public record Shader(gg.kembel.dui.core.ShaderSpec specification, String function, String glsl) {
    public Shader {
      if (specification == null
          || function == null
          || !function.matches("[a-zA-Z_][a-zA-Z0-9_]*")
          || glsl == null
          || glsl.length() > 32768)
        throw new IllegalArgumentException("Invalid effect contribution");
    }
  }

  public PackContribution {
    if (owner == null || !owner.matches("[a-z][a-z0-9_-]*"))
      throw new IllegalArgumentException("Contribution owner");
    var copy = new TreeMap<String, byte[]>();
    resources.forEach(
        (k, v) -> {
          if (!k.startsWith("assets/" + owner + "/") || k.contains(".."))
            throw new IllegalArgumentException("Resources must belong to " + owner);
          copy.put(k, v.clone());
        });
    resources = Collections.unmodifiableMap(copy);
    shaders = List.copyOf(shaders);
    worldMaps = List.copyOf(worldMaps);
    shaderModules = Map.copyOf(shaderModules);
    glyphs = List.copyOf(glyphs);
    fonts = List.copyOf(fonts);
    features = Set.copyOf(features);
    playerRenderers = List.copyOf(playerRenderers);
    for (var renderer : playerRenderers)
      if (!renderer.id().startsWith(owner + ":"))
        throw new IllegalArgumentException("Player renderer owner");
    for (var font : fonts)
      if (!font.id().startsWith(owner + ":")) throw new IllegalArgumentException("Font owner");
    for (String feature : features)
      if (!feature.matches(owner + ":[a-z][a-z0-9_/-]*"))
        throw new IllegalArgumentException("Feature owner");
    for (var id : shaderModules.keySet())
      if (!id.startsWith(owner + ":")) throw new IllegalArgumentException("Shader module owner");
    for (var glyph : glyphs)
      if (!glyph.specification().id().startsWith(owner + ":"))
        throw new IllegalArgumentException("Glyph owner");
    for (var map : worldMaps)
      if (!map.id().startsWith(owner + ":"))
        throw new IllegalArgumentException("World-map owner mismatch");
    for (var e : shaders)
      if (!e.specification().id().startsWith(owner + ":"))
        throw new IllegalArgumentException("Effect owner mismatch");
  }

  @Override
  public Map<String, byte[]> resources() {
    var result = new TreeMap<String, byte[]>();
    resources.forEach((k, v) -> result.put(k, v.clone()));
    return Collections.unmodifiableMap(result);
  }

  public static List<Shader> merge(
      List<PackContribution> contributions, Map<String, byte[]> assets) {
    var ids = new HashSet<String>();
    var functions = new HashSet<String>();
    var result = new ArrayList<Shader>();
    for (var c : contributions) {
      c.resources()
          .forEach(
              (k, v) -> {
                if (assets.putIfAbsent(k, v.clone()) != null)
                  throw new IllegalArgumentException("Pack resource collision: " + k);
              });
      for (var effect : c.shaders()) {
        if (!ids.add(effect.specification().id()) || !functions.add(effect.function()))
          throw new IllegalArgumentException(
              "Effect contribution collision: " + effect.specification().id());
        result.add(effect);
      }
    }
    return List.copyOf(result);
  }
}
