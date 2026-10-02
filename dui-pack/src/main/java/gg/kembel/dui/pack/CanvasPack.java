package gg.kembel.dui.pack;

import com.google.gson.*;
import gg.kembel.dui.core.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;

/** A tiny reusable rectangle/icon atlas. Everything else is layout, text and server state. */
public final class CanvasPack {
  public static char rectangle(int bit, int height) {
    return (char) (0xEA00 + (height - 1) * 9 + bit);
  }

  public static byte[] build(VanillaAssets assets, Map<String, byte[]> additions)
      throws IOException {
    return build(assets, additions, List.of());
  }

  public static byte[] build(
      VanillaAssets assets, Map<String, byte[]> additions, List<PackContribution.Shader> effects)
      throws IOException {
    return build(assets, additions, effects, List.of());
  }

  public static byte[] build(
      VanillaAssets assets,
      Map<String, byte[]> additions,
      List<PackContribution.Shader> effects,
      List<gg.kembel.dui.core.world.WorldMapDefinition> maps)
      throws IOException {
    return build(
        assets,
        additions,
        effects,
        WorldMapPack.prepare(assets, additions, maps),
        Map.of(),
        List.of(),
        Map.of());
  }

  static byte[] build(
      VanillaAssets assets,
      Map<String, byte[]> additions,
      List<PackContribution.Shader> effects,
      WorldMapPack.Result worldMaps,
      Map<String, String> modules,
      List<PackContribution.Glyph> glyphs,
      Map<String, GlyphBinding> glyphBindings)
      throws IOException {
    additions = new TreeMap<>(additions);
    try (var protocol = CanvasPack.class.getResourceAsStream("/ui/shader/protocol.glsl")) {
      additions.putIfAbsent(
          "assets/dui/shaders/include/protocol.glsl",
          Objects.requireNonNull(protocol).readAllBytes());
    }
    additions.putIfAbsent(
        "assets/dui/shaders/include/player-renderers.glsl",
        PlayerRendererPack.glsl(PlayerRenderBinding.bind(List.of()))
            .getBytes(StandardCharsets.UTF_8));
    glyphs = glyphs.stream().sorted(Comparator.comparing(g -> g.specification().id())).toList();
    var bytes = new ByteArrayOutputStream();
    try (var zip = new ZipOutputStream(bytes)) {
      put(
          zip,
          "pack.mcmeta",
          "{\"pack\":{\"description\":\"dui / Vanilla UI\",\"min_format\":88,\"max_format\":88}}"
              .getBytes(StandardCharsets.UTF_8));
      put(zip, "assets/dui/font/text.json", assets.fontDefinition());
      var fontResources = new TreeMap<String, byte[]>();
      for (int h = 1; h <= 9; h++)
        for (int bit = 0; bit <= 8; bit++) {
          var image = new BufferedImage(1 << bit, Math.max(8, h), BufferedImage.TYPE_INT_ARGB);
          for (int x = 0; x < image.getWidth(); x++)
            for (int y = 0; y < h; y++) image.setRGB(x, y, 0xFFFFFFFF);
          png(fontResources, "rect_" + bit + "_" + h, image);
        }
      for (int alpha = 1; alpha < 15; alpha++)
        for (int h = 1; h <= 9; h++)
          for (int bit = 0; bit <= 8; bit++) {
            var image = new BufferedImage(1 << bit, Math.max(8, h), BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < image.getWidth(); x++)
              for (int y = 0; y < h; y++) image.setRGB(x, y, (alpha * 17 << 24) | 0xffffff);
            png(fontResources, "alpha_" + alpha + "_" + bit + "_" + h, image);
          }
      for (var glyph : glyphs) {
        var binding = GlyphRegistry.require(glyphBindings, glyph.specification().id());
        var image = ImageIO.read(new ByteArrayInputStream(glyph.png()));
        var spec = glyph.specification();
        if (image == null || image.getWidth() != spec.width() || image.getHeight() != spec.height())
          throw new IOException("Glyph PNG dimensions: " + spec.id());
        for (int offset = 0; offset < 9; offset++) {
          int rows = (spec.height() + offset + 8) / 9;
          var shifted = new BufferedImage(spec.width(), rows * 9, BufferedImage.TYPE_INT_ARGB);
          for (int y = 0; y < spec.height(); y++)
            for (int x = 0; x < spec.width(); x++)
              shifted.setRGB(x, y + offset, image.getRGB(x, y));
          for (int row = 0; row < rows; row++)
            if ((shifted.getRGB(spec.width() - 1, row * 9 + 8) >>> 24) == 0)
              shifted.setRGB(spec.width() - 1, row * 9 + 8, 0x01FFFFFF);
          png(fontResources, "glyph_" + binding.codePoint() + "_" + offset, shifted);
        }
      }
      for (int offset = 0; offset < 9; offset++) {
        JsonArray providers = new JsonArray();
        JsonObject space = new JsonObject(), advances = new JsonObject();
        space.addProperty("type", "space");
        for (int b = 0; b <= 10; b++) {
          advances.addProperty("" + (char) (0xE800 + b), 1 << b);
          advances.addProperty("" + (char) (0xE900 + b), -(1 << b));
        }
        space.add("advances", advances);
        providers.add(space);
        for (int h = 1; h <= 9; h++)
          for (int b = 0; b <= 8; b++)
            providers.add(
                bitmap("rect_" + b + "_" + h, rectangle(b, h), Math.max(8, h), 7 - offset));
        for (int alpha = 1; alpha < 15; alpha++)
          for (int h = 1; h <= 9; h++)
            for (int bit = 0; bit <= 8; bit++)
              providers.add(
                  bitmap(
                      "alpha_" + alpha + "_" + bit + "_" + h,
                      GlyphAtlas.rectangle(bit, h, alpha),
                      Math.max(8, h),
                      7 - offset));
        for (var glyph : glyphs) {
          var binding = GlyphRegistry.require(glyphBindings, glyph.specification().id());
          var provider =
              bitmap("glyph_" + binding.codePoint() + "_" + offset, binding.character(0), 9, 7);
          for (int band = 1; band < (glyph.specification().height() + offset + 8) / 9; band++)
            provider.getAsJsonArray("chars").add(Character.toString(binding.character(band)));
          providers.add(provider);
        }

        providers =
            BitmapFontAtlas.compact(providers, fontResources, "dui:gui/canvas/atlas_" + offset);
        JsonObject font = new JsonObject();
        font.add("providers", providers);
        put(
            zip,
            "assets/dui/font/canvas_" + offset + ".json",
            font.toString().getBytes(StandardCharsets.UTF_8));
      }
      for (var entry : fontResources.entrySet()) put(zip, entry.getKey(), entry.getValue());
      ShiftedText.write(zip, assets);
      ShaderItemPack.write(zip, assets, additions, effects, modules);
      FocusGuard.write(zip, worldMaps.enabled());
      for (var entry : worldMaps.generated().entrySet()) put(zip, entry.getKey(), entry.getValue());
      for (var entry : new TreeMap<>(additions).entrySet())
        if (!entry.getKey().equals("assets/minecraft/atlases/items.json"))
          put(zip, entry.getKey(), entry.getValue());
    }
    return bytes.toByteArray();
  }

  private static JsonObject bitmap(String name, char glyph, int height, int ascent) {
    JsonObject p = new JsonObject();
    p.addProperty("type", "bitmap");
    p.addProperty("file", "dui:gui/canvas/" + name + ".png");
    p.addProperty("height", height);
    p.addProperty("ascent", ascent);
    JsonArray chars = new JsonArray();
    chars.add("" + glyph);
    p.add("chars", chars);
    return p;
  }

  private static void png(Map<String, byte[]> resources, String name, BufferedImage image)
      throws IOException {
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(image, "png", bytes);
    resources.put("assets/dui/textures/gui/canvas/" + name + ".png", bytes.toByteArray());
  }

  private static void put(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
    var e = new ZipEntry(name);
    e.setTime(0);
    zip.putNextEntry(e);
    zip.write(bytes);
    zip.closeEntry();
  }
}
