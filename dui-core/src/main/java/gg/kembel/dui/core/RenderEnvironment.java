package gg.kembel.dui.core;

import java.util.*;
import java.util.function.IntUnaryOperator;

/** Shared, immutable consumer presentation configuration for menus and world HUDs. */
public record RenderEnvironment(
    GlyphFont font,
    ThemeTokens tokens,
    WidgetSkinRegistry skins,
    Map<String, GlyphBinding> glyphs,
    java.util.function.BiFunction<Map<String, Object>, ThemeTokens, IntUnaryOperator>
        colorTransform,
    Map<String, BitmapFont> fonts,
    Map<String, RasterImage> glyphPixels,
    Map<String, PlayerRenderBinding> playerRenderers) {
  public RenderEnvironment(
      GlyphFont font,
      ThemeTokens tokens,
      WidgetSkinRegistry skins,
      Map<String, GlyphBinding> glyphs,
      java.util.function.BiFunction<Map<String, Object>, ThemeTokens, IntUnaryOperator>
          colorTransform,
      Map<String, BitmapFont> fonts,
      Map<String, RasterImage> glyphPixels) {
    this(
        font,
        tokens,
        skins,
        glyphs,
        colorTransform,
        fonts,
        glyphPixels,
        PlayerRenderBinding.bind(List.of()));
  }

  public RenderEnvironment(
      GlyphFont font,
      ThemeTokens tokens,
      WidgetSkinRegistry skins,
      Map<String, GlyphBinding> glyphs,
      java.util.function.BiFunction<Map<String, Object>, ThemeTokens, IntUnaryOperator>
          colorTransform) {
    this(font, tokens, skins, glyphs, colorTransform, Map.of(), Map.of());
  }

  public RenderEnvironment {
    Objects.requireNonNull(font);
    Objects.requireNonNull(tokens);
    Objects.requireNonNull(skins);
    glyphs = Map.copyOf(glyphs);
    Objects.requireNonNull(colorTransform);
    fonts = Map.copyOf(fonts);
    glyphPixels = Map.copyOf(glyphPixels);
    playerRenderers = Map.copyOf(playerRenderers);
    fonts.forEach(
        (key, value) -> {
          if (!key.equals(value.id())) throw new IllegalArgumentException("Font id mismatch");
        });
  }

  public RenderEnvironment(GlyphFont font, ThemeTokens tokens, WidgetSkinRegistry skins) {
    this(font, tokens, skins, Map.of(), (data, theme) -> v -> v);
  }

  public static RenderEnvironment plain(GlyphFont font) {
    return new RenderEnvironment(font, ThemeTokens.EMPTY, WidgetSkinRegistry.EMPTY);
  }

  public RenderEnvironment forData(Map<String, Object> data) {
    var transform = colorTransform.apply(Map.copyOf(data), tokens);
    return new RenderEnvironment(
        font,
        tokens,
        skins,
        glyphs,
        (ignored, theme) -> transform,
        fonts,
        glyphPixels,
        playerRenderers);
  }

  public RenderEnvironment withTokens(ThemeTokens value) {
    return new RenderEnvironment(
        font, value, skins, glyphs, colorTransform, fonts, glyphPixels, playerRenderers);
  }

  public RenderEnvironment withFont(GlyphFont value) {
    return new RenderEnvironment(
        value, tokens, skins, glyphs, colorTransform, fonts, glyphPixels, playerRenderers);
  }

  public RenderEnvironment withPlayerRenderers(Map<String, PlayerRenderBinding> value) {
    return new RenderEnvironment(
        font, tokens, skins, glyphs, colorTransform, fonts, glyphPixels, value);
  }

  public RenderEnvironment withResources(
      Map<String, BitmapFont> fonts, Map<String, RasterImage> pixels) {
    return new RenderEnvironment(
        font, tokens, skins, glyphs, colorTransform, fonts, pixels, playerRenderers);
  }
}
