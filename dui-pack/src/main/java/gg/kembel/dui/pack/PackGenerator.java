package gg.kembel.dui.pack;

import com.google.gson.*;
import gg.kembel.dui.core.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Produces a deterministic pack plus its matching runtime metadata. */
public final class PackGenerator {
  private PackGenerator() {}

  public static void generate(Path clientJar, Path additionsDirectory, Path output)
      throws Exception {
    generate(clientJar, additionsDirectory, output, List.of());
  }

  public static void generate(
      Path clientJar, Path additionsDirectory, Path output, List<PackContribution> contributions)
      throws Exception {
    var additions = new TreeMap<String, byte[]>();
    if (additionsDirectory != null && Files.isDirectory(additionsDirectory))
      try (var paths = Files.walk(additionsDirectory)) {
        for (var p : paths.filter(Files::isRegularFile).sorted().toList()) {
          String name =
              additionsDirectory.relativize(p).toString().replace(File.separatorChar, '/');
          if (!name.startsWith("assets/") || name.contains(".."))
            throw new IOException("Invalid pack extension path: " + name);
          additions.put(name, Files.readAllBytes(p));
        }
      }
    var effects = PackContribution.merge(contributions, additions);
    var modules = new TreeMap<String, String>();
    var glyphImages = new ArrayList<PackContribution.Glyph>();
    var fonts = new TreeMap<String, BitmapFont>();
    var features = new TreeSet<String>();
    for (var contribution : contributions) {
      contribution
          .shaderModules()
          .forEach(
              (id, source) -> {
                if (modules.putIfAbsent(id, source) != null)
                  throw new IllegalArgumentException("Duplicate shader module " + id);
              });
      glyphImages.addAll(contribution.glyphs());
      features.addAll(contribution.features());
      for (var font : contribution.fonts())
        if (fonts.putIfAbsent(font.id(), font) != null)
          throw new IllegalArgumentException("Duplicate font: " + font.id());
    }
    var glyphBindings =
        GlyphRegistry.bind(
            glyphImages.stream().map(PackContribution.Glyph::specification).toList());

    var capabilities =
        new TreeSet<>(
            Set.of(
                "native",
                "shader-components-v1",
                "clips",
                "motion-tracks",
                "effects-32",
                gg.kembel.dui.core.PlayerModelCodec.CAPABILITY));
    capabilities.add("rgba-raster-v1");
    capabilities.add(gg.kembel.dui.core.video.MapVideoCodec.CAPABILITY);
    capabilities.addAll(features);
    var playerRenderers =
        PlayerRenderBinding.bind(
            contributions.stream().flatMap(c -> c.playerRenderers().stream()).toList());
    var shaders =
        ShaderRegistry.bind(effects.stream().map(PackContribution.Shader::specification).toList());
    Files.createDirectories(output);
    try (var assets = new VanillaAssets(clientJar)) {
      additions.put(
          "assets/dui/shaders/include/player-renderers.glsl",
          PlayerRendererPack.glsl(playerRenderers)
              .getBytes(java.nio.charset.StandardCharsets.UTF_8));
      fonts.put("dui:default", assets.bitmapFont());
      var glyphPixels = new TreeMap<String, RasterImage>();
      for (var glyph : glyphImages) {
        var image = javax.imageio.ImageIO.read(new ByteArrayInputStream(glyph.png()));
        if (image == null) throw new IOException("Invalid glyph PNG");
        glyphPixels.put(
            glyph.specification().id(),
            RasterImage.argb(
                image.getWidth(),
                image.getHeight(),
                image.getRGB(
                    0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth())));
      }
      var definitions = contributions.stream().flatMap(c -> c.worldMaps().stream()).toList();
      var worldMaps =
          WorldMapPack.prepare(assets, additions, definitions, glyphImages, glyphBindings);
      if (worldMaps.enabled())
        capabilities.add(gg.kembel.dui.core.world.WorldMapProtocol.CAPABILITY);
      byte[] pack =
          CanvasPack.build(
              assets, additions, effects, worldMaps, modules, glyphImages, glyphBindings);
      var sorted = new TreeMap<String, byte[]>();
      try (var zip = new ZipInputStream(new ByteArrayInputStream(pack))) {
        for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry())
          if (sorted.put(entry.getName(), zip.readAllBytes()) != null)
            throw new IOException("Duplicate pack entry: " + entry.getName());
      }
      var bytes = new ByteArrayOutputStream();
      try (var zip = new ZipOutputStream(bytes)) {
        for (var entry : sorted.entrySet()) {
          var e = new ZipEntry(entry.getKey());
          e.setTime(0);
          zip.putNextEntry(e);
          zip.write(entry.getValue());
          zip.closeEntry();
        }
      }
      pack = bytes.toByteArray();
      var models = new TreeSet<>(assets.itemDefinitions().keySet());
      models.add("dui:effect/panel");
      models.addAll(PlayerArmorPack.models());
      for (String name : additions.keySet())
        if (name.matches("assets/[^/]+/items/.+\\.json")) {
          var parts = name.split("/", 4);
          models.add(parts[1] + ":" + parts[3].substring(0, parts[3].length() - 5));
        }
      String sha1 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(pack));
      Files.write(output.resolve("dui.zip"), pack);
      Files.writeString(
          output.resolve("dui.json"),
          new GsonBuilder()
                  .setPrettyPrinting()
                  .create()
                  .toJson(
                      JsonParser.parseString(
                          WorldMapPack.encode(
                              metadata(
                                  capabilities,
                                  sha1,
                                  models,
                                  assets.metrics(),
                                  worldMaps,
                                  shaders,
                                  glyphBindings,
                                  fonts,
                                  glyphPixels,
                                  playerRenderers))))
              + "\n");
    }
  }

  static Map<String, Object> metadata(
      Set<String> capabilities,
      String sha1,
      Set<String> models,
      Map<String, Integer> metrics,
      WorldMapPack.Result maps,
      Map<String, ShaderBinding> shaders,
      Map<String, GlyphBinding> glyphs,
      Map<String, BitmapFont> fonts,
      Map<String, RasterImage> glyphPixels,
      Map<String, PlayerRenderBinding> playerRenderers) {
    var result = new TreeMap<String, Object>();
    result.put("protocolVersion", RendererProtocol.VERSION);
    result.put("capabilities", capabilities);
    result.put("codecHash", RendererProtocol.SCHEMA_SHA256);
    result.put("minecraftVersion", "26.2");
    result.put("libraryVersion", "0.2.0-SNAPSHOT");
    result.put("sha1", sha1);
    result.put("models", models);
    result.put("shaders", shaders);
    result.put("glyphs", glyphs);
    result.put("playerRenderers", playerRenderers);
    result.put("bitmapFonts", fonts);
    result.put("glyphPixels", glyphPixels);
    result.put("fontMetrics", metrics);
    result.put("worldMapLegendMetrics", maps.legendMetrics());
    if (maps.enabled()) {
      result.put("worldMaps", maps.maps());
    }
    return result;
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 2 || args.length > 3)
      throw new IllegalArgumentException(
          "Usage: PackGenerator <verified-client.jar> <output-directory> [own-assets-directory]");
    generate(Path.of(args[0]), args.length == 3 ? Path.of(args[2]) : null, Path.of(args[1]));
  }
}
