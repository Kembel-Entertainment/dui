package gg.kembel.dui.paper;

import gg.kembel.dui.core.*;
import java.util.*;
import java.util.function.Function;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.*;

/** One plain_message, one continuous canvas. Hit geometry is emitted before decorative glyphs. */
public final class CanvasRenderer {
  private static Component run(String text, String font, int color) {
    return Component.text(text)
        .font(Key.key("dui", font))
        .color(TextColor.color(color))
        .shadowColor(ShadowColor.shadowColor(0));
  }

  private static Component shift(int n) {
    return run(GlyphFont.shift(n), "canvas_0", 0xFFFFFF);
  }

  public Component render(Canvas canvas, Function<Canvas.Hit, Key> callback) {
    return render(canvas, callback, head -> NativeHeads.render(head, null));
  }

  public Component render(
      Canvas canvas,
      Function<Canvas.Hit, Key> callback,
      Function<Canvas.Head, Component> headRenderer) {
    return render(
        canvas,
        callback,
        headRenderer,
        hit ->
            HoverEvent.showText(
                Component.text(
                    hit.tooltip().isBlank() ? hit.id() : hit.tooltip(), NamedTextColor.AQUA)));
  }

  public Component render(
      Canvas canvas,
      Function<Canvas.Hit, Key> callback,
      Function<Canvas.Head, Component> headRenderer,
      Function<Canvas.Hit, HoverEvent<?>> tooltip) {
    return renderActions(
        canvas, hit -> ClickEvent.custom(callback.apply(hit), null), headRenderer, tooltip);
  }

  /** Supports native URL confirmation alongside server-side custom click callbacks. */
  public Component renderActions(
      Canvas canvas,
      Function<Canvas.Hit, ClickEvent> callback,
      Function<Canvas.Head, Component> headRenderer,
      Function<Canvas.Hit, HoverEvent<?>> tooltip) {
    canvas = canvas.renderPlan();
    Map<String, ClickEvent> actions = new HashMap<>();
    for (var hit : canvas.hits)
      if (!hit.action().isBlank()) actions.put(hit.id(), callback.apply(hit));
    var result = Component.text();
    for (int y = 0; y < canvas.height; y += 9) {
      var line = Component.text();
      // Vanilla resolves the first positive advance at the pointer. Invisible spans provide exact
      // hit boxes.
      int start = 0;
      while (start < canvas.width + 2) {
        Canvas.Hit hit = canvas.at(start, y + 4);
        int end = start + 1;
        while (end < canvas.width + 2 && Objects.equals(hit, canvas.at(end, y + 4))) end++;
        Component span = shift(end - start);
        if (hit != null) {
          span = span.hoverEvent(tooltip.apply(hit));
          if (actions.containsKey(hit.id())) span = span.clickEvent(actions.get(hit.id()));
        }
        line.append(span);
        start = end;
      }
      line.append(shift(-canvas.width - 2));
      if (y == 0 && canvas.hideFocusOutline)
        line.append(run("" + FocusMarker.GLYPH, "focus_guard", FocusMarker.payload(canvas)))
            .append(shift(-FocusMarker.ADVANCE));
      images(line, canvas, y, true);
      for (var p : canvas.paints) {
        if (p.y() + p.height() <= y || p.y() >= y + 9) continue;
        if (p.text() != null || p.icon() != null) {
          if (p.icon() != null && p.icon().startsWith("item/")) {
            int half = (y - p.y()) / 9;
            line.append(shift(p.x()))
                .append(run("" + ItemGlyphs.glyph(p.icon(), half), "items", p.color()))
                .append(shift(-p.x() - ItemGlyphs.advance(p.icon(), half)));
            continue;
          }
          int offset = p.y() % 9, band = (y - p.y() / 9 * 9) / 9;
          if (offset == 0 && band != 0 || band > 1) continue;
          String font =
              p.text() != null
                  ? (offset == 0 ? "text" : "text_" + offset + "_" + band)
                  : "canvas_" + offset;
          Component glyph =
              p.text() != null
                  ? run(p.text(), font, p.color())
                  : run("" + GlyphAtlas.icon(p.icon(), band), font, p.color());
          int advance = p.text() != null ? p.width() : 10;
          line.append(shift(p.x())).append(glyph).append(shift(-p.x() - advance));
        } else {
          int top = Math.max(p.y(), y), h = Math.min(p.y() + p.height(), y + 9) - top;
          int x = p.x(), remaining = p.width();
          while (remaining > 0) {
            int bit = 31 - Integer.numberOfLeadingZeros(Math.min(remaining, 256));
            int width = 1 << bit;
            line.append(shift(x))
                .append(run("" + GlyphAtlas.rectangle(bit, h), "canvas_" + (top - y), p.color()))
                .append(shift(-x - width - 1));
            x += width;
            remaining -= width;
          }
        }
      }
      images(line, canvas, y, false);
      for (var head : canvas.heads)
        if (head.y() == y)
          line.append(shift(head.x()))
              .append(headRenderer.apply(head))
              .append(shift(-head.x() - 8));
      // Intrinsic width must cover every intermediate pen position, including bitmap's +1 advance.
      line.append(shift(canvas.width + 2));
      result.append(line.build());
      if (y + 9 < canvas.height) result.append(Component.newline());
    }
    return result.build();
  }

  private static void images(
      net.kyori.adventure.text.TextComponent.Builder line,
      Canvas canvas,
      int y,
      boolean background) {
    for (var image : canvas.images) {
      if (image.background() != background) continue;
      int cell = image.pixelSize();
      for (int row = Math.max(0, (y - image.y()) / cell); row < image.raster().height; row++) {
        int py = image.y() + row * cell;
        if (py >= y + 9 || py >= image.y() + image.height()) break;
        int top = Math.max(py, y),
            bottom = Math.min(Math.min(py + cell, y + 9), image.y() + image.height());
        if (bottom <= top) continue;
        line.append(shift(image.x()));
        for (int col = 0; col < image.raster().width; ) {
          int columnStart = col, color = image.raster().rgb(col++, row);
          while (col < image.raster().width && image.raster().rgb(col, row) == color) col++;
          int pixels = Math.min(col * cell, image.width()) - columnStart * cell;
          var glyphs = new StringBuilder();
          while (pixels > 0) {
            int bit = 31 - Integer.numberOfLeadingZeros(Math.min(pixels, 256));
            glyphs.append(GlyphAtlas.rectangle(bit, bottom - top)).append(GlyphFont.shift(-1));
            pixels -= 1 << bit;
          }
          line.append(run(glyphs.toString(), "canvas_" + (top - y), color));
        }
        line.append(shift(-image.x() - image.width()));
      }
    }
  }
}
