package gg.kembel.dui.core;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;

/** Bounded raster composition with the same affine geometry used for native placements and hits. */
final class GroupComposer {
  private GroupComposer() {}

  static Canvas compose(String id, Canvas source, SceneGroup options) {
    var out = new Canvas(source.width, source.height, source.environment());
    var t = options.transform();
    var clip =
        options.clip() == null ? new Scene.Rect(0, 0, out.width, out.height) : options.clip();
    var buffer = new BufferedImage(out.width, out.height, BufferedImage.TYPE_INT_ARGB);
    var g = buffer.createGraphics();
    try {
      g.setClip(clip.x(), clip.y(), clip.width(), clip.height());
      g.setComposite(
          AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) options.opacity()));
      g.transform(new AffineTransform(t.a(), t.b(), t.c(), t.d(), t.tx(), t.ty()));
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      for (var primitive : source.drawOrder()) {
        if (primitive instanceof Canvas.Paint paint) {
          if (paint.text() == null && paint.icon() == null) {
            g.setColor(new Color(paint.color()));
            g.fillRect(paint.x(), paint.y(), paint.width(), paint.height());
          } else {
            RasterImage raster;
            if (paint.text() != null)
              raster =
                  RichText.require(source.environment().fonts(), "dui:default")
                      .raster(paint.text(), paint.color());
            else {
              var original = source.environment().glyphPixels().get(paint.icon());
              if (original == null)
                throw new IllegalArgumentException(
                    "Group needs registered glyph pixels: " + paint.icon());
              int[] pixels = original.pixels();
              for (int i = 0; i < pixels.length; i++) {
                int rgb = 0;
                for (int s : new int[] {16, 8, 0})
                  rgb |= ((pixels[i] >> s & 255) * (paint.color() >> s & 255) / 255) << s;
                pixels[i] = (pixels[i] & 0xff000000) | rgb;
              }
              raster = RasterImage.argb(original.width, original.height, pixels);
            }
            draw(g, raster, paint.x(), paint.y(), paint.width(), paint.height());
          }
        } else if (primitive instanceof Canvas.Image image)
          draw(g, image.raster(), image.x(), image.y(), image.width(), image.height());
      }
    } finally {
      g.dispose();
    }
    int left = out.width, top = out.height, right = 0, bottom = 0;
    for (int y = 0; y < out.height; y++)
      for (int x = 0; x < out.width; x++)
        if ((buffer.getRGB(x, y) >>> 24) != 0) {
          left = Math.min(left, x);
          top = Math.min(top, y);
          right = Math.max(right, x + 1);
          bottom = Math.max(bottom, y + 1);
        }
    if (right > left && bottom > top)
      out.image(
          id + "/raster",
          left,
          top,
          right - left,
          bottom - top,
          1,
          RasterImage.argb(
              right - left,
              bottom - top,
              buffer.getRGB(left, top, right - left, bottom - top, null, 0, right - left)));
    boolean axis = Math.abs(t.b()) < 1e-9 && Math.abs(t.c()) < 1e-9 && t.a() > 0 && t.d() > 0;
    for (var hit : source.hits) {
      if (options.opacity() == 0) continue;
      if (!axis)
        throw new IllegalArgumentException(
            "Native dialog hits require axis-aligned grid transforms");
      var rect =
          intersection(
              bounds(t, hit.x(), hit.y(), hit.width(), hit.height()), clip, out.width, out.height);
      if (rect == null) continue;
      if (rect.y() % 9 != 0 || rect.height() % 9 != 0)
        throw new IllegalArgumentException(
            "Transformed hit must align to nine-pixel rows; disable input during off-grid motion");
      out.hit(
          new Canvas.Hit(
              id + "/" + hit.id(),
              hit.action(),
              hit.value(),
              hit.tooltip(),
              rect.x(),
              rect.y(),
              rect.width(),
              rect.height()));
    }
    for (var item : source.items) {
      if (source.motions.containsKey(item.id()))
        throw new IllegalArgumentException(
            "Group native motion must be sampled before composition");
      double scale = Math.hypot(t.a(), t.b());
      if (t.a() * t.d() - t.b() * t.c() < 0
          || Math.abs(scale - Math.hypot(t.c(), t.d())) > 1e-6
          || Math.abs(t.a() * t.c() + t.b() * t.d()) > 1e-6)
        throw new IllegalArgumentException("Native items require uniform group scale");
      int size = (int) Math.round(item.size() * scale);
      var center = t.apply(item.x() + item.size() / 2., item.y() + item.size() / 2.);
      var itemClip = source.clips.get(item.id());
      if (itemClip != null && !axis)
        throw new IllegalArgumentException("Rotated local item clipping needs a consumer backend");
      var viewport =
          itemClip == null
              ? clip
              : intersection(
                  bounds(t, itemClip.x(), itemClip.y(), itemClip.width(), itemClip.height()),
                  clip,
                  out.width,
                  out.height);
      if (viewport == null || options.opacity() == 0) continue;
      String key = id + "/" + item.id();
      out.item(
          key,
          (int) Math.round(center.x() - size / 2.),
          (int) Math.round(center.y() - size / 2.),
          size,
          new ItemClip(viewport.x(), viewport.y(), viewport.width(), viewport.height()));
      int rotation = (int) Math.round(Math.toDegrees(Math.atan2(t.b(), t.a())));
      out.motion(
          key,
          new Motion(
              0,
              1,
              0,
              Motion.Easing.LINEAR,
              false,
              0,
              0,
              1,
              1,
              rotation,
              rotation,
              options.opacity(),
              options.opacity(),
              .5,
              .5));
    }
    for (var effect : source.effects) {
      if (!axis || source.effectMotions.containsKey(effect.id()))
        throw new IllegalArgumentException(
            "Compose procedural motion inside the contributed shader or sample it first");
      var rect = bounds(t, effect.x(), effect.y(), effect.width(), effect.height());
      var cropped = intersection(rect, clip, out.width, out.height);
      if (cropped == null || options.opacity() == 0) continue;
      if (!rect.equals(cropped))
        throw new IllegalArgumentException("Procedural backend lacks generic partial clipping");
      String key = id + "/" + effect.id();
      out.effect(
          new ShaderInvocation(
              key,
              effect.shader(),
              rect.x(),
              rect.y(),
              rect.width(),
              rect.height(),
              effect.parameters(),
              effect.lifetimeTicks()));
      if (options.opacity() != 1)
        out.effectMotion(
            key,
            new Motion(
                0,
                1,
                0,
                Motion.Easing.LINEAR,
                false,
                0,
                0,
                1,
                1,
                0,
                0,
                options.opacity(),
                options.opacity(),
                .5,
                .5));
    }
    for (var head : source.heads) {
      if (!translation(t) || options.opacity() != 1)
        throw new IllegalArgumentException("Native heads support group translation only");
      var p = t.apply(head.x(), head.y());
      var rect = new Scene.Rect((int) Math.round(p.x()), (int) Math.round(p.y()), 8, 8);
      var visible = intersection(rect, clip, out.width, out.height);
      if (visible == null) continue;
      if (!rect.equals(visible))
        throw new IllegalArgumentException("Native heads lack partial group clipping");
      out.head(rect.x(), rect.y(), head.source(), head.hat());
    }
    for (var model : source.playerModels) {
      if (!translation(t) || options.opacity() != 1)
        throw new IllegalArgumentException("Player models support group translation only");
      var p = t.apply(model.x(), model.y());
      var rect =
          new Scene.Rect(
              (int) Math.round(p.x()), (int) Math.round(p.y()), model.width(), model.height());
      var visible = intersection(rect, clip, out.width, out.height);
      if (visible == null) continue;
      if (!rect.equals(visible))
        throw new IllegalArgumentException("Player models lack partial group clipping");
      out.playerModel(
          id + "/" + model.id(),
          model.source(),
          rect.x(),
          rect.y(),
          rect.width(),
          rect.height(),
          model.renderer(),
          model.facing(),
          model.outerLayer(),
          model.idle());
    }
    if (!source.coverage.isEmpty())
      throw new IllegalArgumentException("Place popup coverage outside transformed groups");
    for (var primitive : source.primitives) {
      if (primitive.clip() != null && !axis)
        throw new IllegalArgumentException(
            "Nested rectangular backend clipping requires axis-aligned transforms");
      var viewport =
          primitive.clip() == null
              ? clip
              : intersection(
                  bounds(
                      t,
                      primitive.clip().x(),
                      primitive.clip().y(),
                      primitive.clip().width(),
                      primitive.clip().height()),
                  clip,
                  out.width,
                  out.height);
      if (viewport != null && options.opacity() > 0)
        out.primitive(
            new RenderPrimitive(
                id + "/" + primitive.id(),
                primitive.type(),
                primitive.bounds(),
                primitive.data(),
                t.multiply(primitive.transform()),
                viewport,
                options.opacity() * primitive.opacity()));
    }
    return out;
  }

  private static boolean translation(Transform2D t) {
    return t.a() == 1 && t.b() == 0 && t.c() == 0 && t.d() == 1;
  }

  private static void draw(Graphics2D g, RasterImage raster, int x, int y, int w, int h) {
    var image = new BufferedImage(raster.width, raster.height, BufferedImage.TYPE_INT_ARGB);
    image.setRGB(0, 0, raster.width, raster.height, raster.pixels(), 0, raster.width);
    g.drawImage(image, x, y, w, h, null);
  }

  static Scene.Rect bounds(Transform2D t, double x, double y, double w, double h) {
    var points =
        java.util.List.of(
            t.apply(x, y), t.apply(x + w, y), t.apply(x, y + h), t.apply(x + w, y + h));
    int
        left =
            (int)
                Math.floor(
                    points.stream().mapToDouble(Transform2D.Point::x).min().orElseThrow() + 1e-9),
        top =
            (int)
                Math.floor(
                    points.stream().mapToDouble(Transform2D.Point::y).min().orElseThrow() + 1e-9),
        right =
            (int)
                Math.ceil(
                    points.stream().mapToDouble(Transform2D.Point::x).max().orElseThrow() - 1e-9),
        bottom =
            (int)
                Math.ceil(
                    points.stream().mapToDouble(Transform2D.Point::y).max().orElseThrow() - 1e-9);
    return new Scene.Rect(left, top, right - left, bottom - top);
  }

  private static Scene.Rect intersection(Scene.Rect a, Scene.Rect b, int width, int height) {
    int l = Math.max(0, Math.max(a.x(), b.x())),
        t = Math.max(0, Math.max(a.y(), b.y())),
        r = Math.min(width, Math.min(a.x() + a.width(), b.x() + b.width())),
        d = Math.min(height, Math.min(a.y() + a.height(), b.y() + b.height()));
    return r > l && d > t ? new Scene.Rect(l, t, r - l, d - t) : null;
  }
}
