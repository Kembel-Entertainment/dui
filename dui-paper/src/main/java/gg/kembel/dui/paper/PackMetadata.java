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

  public void validate(Canvas canvas) { portable().validate(canvas); }

  public PackMetadata {
    var metadata = new gg.kembel.dui.runtime.PackMetadata(minecraftVersion, libraryVersion, sha1,
        models, fontMetrics, protocolVersion, capabilities, codecHash, worldMaps,
        worldMapLegendMetrics, shaders, glyphs, bitmapFonts, glyphPixels, playerRenderers);
    models = metadata.models(); fontMetrics = metadata.fontMetrics(); capabilities = metadata.capabilities();
    worldMaps = metadata.worldMaps(); worldMapLegendMetrics = metadata.worldMapLegendMetrics();
    shaders = metadata.shaders(); glyphs = metadata.glyphs(); bitmapFonts = metadata.bitmapFonts();
    glyphPixels = metadata.glyphPixels(); playerRenderers = metadata.playerRenderers();
  }

  public static PackMetadata read(Path path) throws IOException {
    return new Gson().fromJson(Files.readString(path), PackMetadata.class);
  }

  public GlyphFont font() {
    return new GlyphFont(fontMetrics);
  }

  public gg.kembel.dui.runtime.PackMetadata portable() {
    return new gg.kembel.dui.runtime.PackMetadata(minecraftVersion, libraryVersion, sha1, models,
        fontMetrics, protocolVersion, capabilities, codecHash, worldMaps, worldMapLegendMetrics,
        shaders, glyphs, bitmapFonts, glyphPixels, playerRenderers);
  }
}
