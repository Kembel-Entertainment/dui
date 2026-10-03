package gg.kembel.dui.paper;

import com.google.gson.*;
import gg.kembel.dui.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public record PackMetadata(
    String minecraftVersion,
    String libraryVersion,
    String sha1,
    Set<String> models,
    Map<String, Integer> fontMetrics,
    int protocolVersion,
    Set<String> capabilities,
    String codecHash,
    Map<String, gg.kembel.dui.core.world.WorldMapMetadata> worldMaps,
    Map<String, Integer> worldMapLegendMetrics,
    Map<String, ShaderBinding> shaders,
    Map<String, GlyphBinding> glyphs,
    Map<String, BitmapFont> bitmapFonts,
    Map<String, RasterImage> glyphPixels,
    Map<String, PlayerRenderBinding> playerRenderers) {
  public PackMetadata(
      String mc,
      String lib,
      String sha,
      Set<String> models,
      Map<String, Integer> metrics,
      int version,
      Set<String> capabilities,
      String hash,
      Map<String, gg.kembel.dui.core.world.WorldMapMetadata> maps,
      Map<String, Integer> legend,
      Map<String, ShaderBinding> shaders,
      Map<String, GlyphBinding> glyphs,
      Map<String, BitmapFont> fonts,
      Map<String, RasterImage> pixels) {
    this(
        mc,
        lib,
        sha,
        models,
        metrics,
        version,
        capabilities,
        hash,
        maps,
        legend,
        shaders,
        glyphs,
        fonts,
        pixels,
        PlayerRenderBinding.bind(List.of()));
  }

  public PackMetadata(
      String mc,
      String lib,
      String sha,
      Set<String> models,
      Map<String, Integer> metrics,
      int version,
      Set<String> capabilities,
      String hash,
      Map<String, gg.kembel.dui.core.world.WorldMapMetadata> maps,
      Map<String, Integer> legend,
      Map<String, ShaderBinding> shaders,
      Map<String, GlyphBinding> glyphs) {
    this(
        mc,
        lib,
        sha,
        models,
        metrics,
        version,
        capabilities,
        hash,
        maps,
        legend,
        shaders,
        glyphs,
        Map.of(),
        Map.of());
  }

  public PackMetadata(
      String mc,
      String lib,
      String sha,
      Set<String> models,
      Map<String, Integer> metrics,
      int version,
      Set<String> capabilities,
      String hash) {
    this(
        mc,
        lib,
        sha,
        models,
        metrics,
        version,
        capabilities,
        hash,
        Map.of(),
        Map.of(),
        Map.of(),
        Map.of());
  }

  public boolean supports(String capability) {
    return capabilities.contains(capability);
  }

  public void validate(Canvas canvas) {
    if (canvas.images.stream().anyMatch(i -> i.raster().hasAlpha()) && !supports("rgba-raster-v1"))
      throw new IllegalArgumentException("Pack lacks rgba-raster-v1 capability");
    if (!canvas.playerModels.isEmpty() && !supports(PlayerModelCodec.CAPABILITY))
      throw new IllegalArgumentException(
          "Pack lacks " + PlayerModelCodec.CAPABILITY + " capability");
    for (var model : canvas.playerModels) {
      var binding = playerRenderers.get(model.renderer());
      if (binding == null
          || binding.code() != model.rendererCode()
          || model.viewportIndex() >= binding.specification().viewports().size()
          || model.facing() >= binding.specification().poses().size())
        throw new IllegalArgumentException("Model renderer metadata mismatch");
      var viewport = binding.specification().viewports().get(model.viewportIndex());
      if (viewport.width() != model.width() || viewport.height() != model.height())
        throw new IllegalArgumentException("Model viewport mismatch");
    }
    for (var paint : canvas.paints)
      if (paint.icon() != null) GlyphRegistry.require(glyphs, paint.icon());
    for (var effect : canvas.effects) ShaderRegistry.require(shaders, effect.shader());
    if (canvas.effects.size() > 8 && !supports("effects-32"))
      throw new IllegalArgumentException("Pack lacks effects-32 capability");
    if ((!canvas.motions.isEmpty() || !canvas.effectMotions.isEmpty())
        && !supports("motion-tracks"))
      throw new IllegalArgumentException("Pack lacks motion-tracks capability");
  }

  public PackMetadata {
    if (!"26.2".equals(minecraftVersion)
        || !"0.2.0-SNAPSHOT".equals(libraryVersion)
        || sha1 == null
        || !sha1.matches("[a-f0-9]{40}"))
      throw new IllegalArgumentException("Unsupported or invalid dui pack metadata");
    if (protocolVersion != RendererProtocol.VERSION
        || !RendererProtocol.SCHEMA_SHA256.equals(codecHash))
      throw new IllegalArgumentException(
          "Pack protocol/hash mismatch; rebuild the pack for dui 0.2");
    capabilities = Set.copyOf(capabilities);
    if (!capabilities.containsAll(
        Set.of("native", "shader-components-v1", "clips", "motion-tracks", "effects-32")))
      throw new IllegalArgumentException("Missing renderer capabilities");
    glyphs = glyphs == null ? Map.of() : Map.copyOf(glyphs);
    playerRenderers =
        playerRenderers == null ? PlayerRenderBinding.bind(List.of()) : Map.copyOf(playerRenderers);
    var addresses = new HashSet<Integer>();
    for (var entry : playerRenderers.entrySet())
      if (!entry.getKey().equals(entry.getValue().specification().id())
          || !addresses.add(entry.getValue().code()))
        throw new IllegalArgumentException("Model renderer binding collision");
    bitmapFonts = bitmapFonts == null ? Map.of() : Map.copyOf(bitmapFonts);
    glyphPixels = glyphPixels == null ? Map.of() : Map.copyOf(glyphPixels);
    for (var entry : bitmapFonts.entrySet())
      if (!entry.getKey().equals(entry.getValue().id()))
        throw new IllegalArgumentException("Font metadata mismatch");
    GlyphRegistry.validate(glyphs);
    shaders = shaders == null ? Map.of() : Map.copyOf(shaders);
    ShaderRegistry.validate(shaders);
    worldMaps = worldMaps == null ? Map.of() : Map.copyOf(worldMaps);
    worldMapLegendMetrics =
        worldMapLegendMetrics == null ? Map.of() : Map.copyOf(worldMapLegendMetrics);
    if (supportsWorldMaps(capabilities) != !worldMaps.isEmpty())
      throw new IllegalArgumentException("Map capability/metadata mismatch");
    for (var entry : worldMaps.entrySet())
      if (!entry.getKey().equals(entry.getValue().definition().id()))
        throw new IllegalArgumentException("Map id mismatch");
    if ((!worldMaps.isEmpty() || capabilities.contains(gg.kembel.dui.core.video.MapVideoCodec.CAPABILITY))
        && !worldMapLegendMetrics.containsKey("?"))
      throw new IllegalArgumentException("Missing world/video HUD font metrics");
    models = Set.copyOf(models);
    fontMetrics = Map.copyOf(fontMetrics);
    new GlyphFont(fontMetrics);
  }

  private static boolean supportsWorldMaps(Set<String> capabilities) {
    return capabilities.contains(gg.kembel.dui.core.world.WorldMapProtocol.CAPABILITY);
  }

  public static PackMetadata read(Path path) throws IOException {
    return new Gson().fromJson(Files.readString(path), PackMetadata.class);
  }

  public GlyphFont font() {
    return new GlyphFont(fontMetrics);
  }
}
