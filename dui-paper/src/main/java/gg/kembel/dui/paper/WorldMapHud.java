package gg.kembel.dui.paper;

import gg.kembel.dui.core.*;
import gg.kembel.dui.core.world.*;
import java.util.*;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.*;

/** Generic paint transport. Template/application code owns every visible HUD layout and color. */
final class WorldMapHud {
  private final Map<String, Integer> metrics;
  private final Map<String, GlyphBinding> glyphs;

  WorldMapHud(Map<String, Integer> metrics, Map<String, GlyphBinding> glyphs) {
    this.metrics = metrics;
    this.glyphs = glyphs;
  }

  Component render(WorldHud presentation) {
    var result = Component.text();
    if (presentation.coverNativeHud())
      result.append(
          run(
              Character.toString(0xE085)
                  + WorldHudCodec.shift(-4)
                  + Character.toString(0xE086)
                  + WorldHudCodec.shift(-4),
              WorldHudCodec.font(0),
              presentation.nativeHudColor()));
    for (var surface : presentation.surfaces()) {
      if (surface.opacity() == 0) continue;
      for (var image : surface.images()) if (image.background()) image(result, surface, image);
      for (var paint : surface.paints()) {
        if (paint.text() != null) {
          var text = new StringBuilder();
          for (int cp : paint.text().codePoints().toArray()) {
            String ch = Character.toString(cp);
            if (!metrics.containsKey(ch)) ch = "?";
            text.append(ch);
            if (!ch.equals(" ")) text.append(WorldHudCodec.shift(-2));
          }
          primitive(
              result, surface, paint.x(), paint.y(), text.toString(), paint.width(), paint.color());
        } else if (paint.icon() != null) {
          primitive(
              result,
              surface,
              paint.x(),
              paint.y(),
              Character.toString(GlyphRegistry.require(glyphs, paint.icon()).character(0)),
              GlyphRegistry.require(glyphs, paint.icon()).advance() + 2,
              paint.color());
        } else
          rectangle(
              result, surface, paint.x(), paint.y(), paint.width(), paint.height(), paint.color());
      }
      for (var image : surface.images()) if (!image.background()) image(result, surface, image);
    }
    return result.build();
  }

  private static Component run(String text, String font, int color) {
    return Component.text(text)
        .font(Key.key(font))
        .color(TextColor.color(color))
        .shadowColor(ShadowColor.none());
  }

  private void primitive(
      net.kyori.adventure.text.TextComponent.Builder out,
      WorldHud.Surface s,
      int x,
      int y,
      String glyph,
      int advance,
      int color) {
    int dx = s.x(x), dy = s.y(y), pen = WorldHudCodec.pen(s, dx, dy);
    out.append(
        run(
            WorldHudCodec.shift(pen) + glyph + WorldHudCodec.shift(-pen - advance),
            WorldHudCodec.font(dy),
            color));
  }

  private void rectangle(
      net.kyori.adventure.text.TextComponent.Builder out,
      WorldHud.Surface s,
      int x,
      int y,
      int width,
      int height,
      int color) {
    for (int row = 0; row < height; row += 9) {
      int h = Math.min(9, height - row), left = 0;
      while (left < width) {
        int bit = 31 - Integer.numberOfLeadingZeros(Math.min(128, width - left));
        int w = 1 << bit;
        primitive(
            out,
            s,
            x + left,
            y + row,
            Character.toString(GlyphAtlas.rectangle(bit, h)),
            w + 3,
            color);
        left += w;
      }
    }
  }

  private void image(
      net.kyori.adventure.text.TextComponent.Builder out, WorldHud.Surface s, Canvas.Image image) {
    int cell = image.pixelSize();
    for (int row = 0; row < image.raster().height; row++) {
      int h = Math.min(cell, image.height() - row * cell);
      if (h <= 0) break;
      for (int col = 0; col < image.raster().width; ) {
        int start = col, argb = image.raster().argb(col++, row), color = argb & 0xffffff;
        while (col < image.raster().width && image.raster().argb(col, row) == argb) col++;
        int w = Math.min(col * cell, image.width()) - start * cell;
        int opacity = (s.opacity() * (argb >>> 24) + 127) / 255;
        if (w > 0 && opacity > 0)
          rectangle(
              out,
              new WorldHud.Surface(
                  s.horizontal(),
                  s.vertical(),
                  s.offsetX(),
                  s.offsetY(),
                  opacity,
                  s.width(),
                  s.height(),
                  List.of(),
                  List.of()),
              image.x() + start * cell,
              image.y() + row * cell,
              w,
              h,
              color);
      }
    }
  }
}
