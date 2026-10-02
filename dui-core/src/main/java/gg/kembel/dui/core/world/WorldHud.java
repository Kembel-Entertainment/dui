package gg.kembel.dui.core.world;

import gg.kembel.dui.core.Canvas;
import java.util.*;

/** Immutable, application-owned screen overlay. Coordinates are logical pixels, not GUI units. */
public record WorldHud(boolean coverNativeHud, int nativeHudColor, List<Surface> surfaces) {
  public static final WorldHud EMPTY = new WorldHud(false, 0, List.of());

  public enum Horizontal {
    LEFT,
    CENTER,
    RIGHT
  }

  public enum Vertical {
    TOP,
    CENTER,
    BOTTOM
  }

  public record Surface(
      Horizontal horizontal,
      Vertical vertical,
      int offsetX,
      int offsetY,
      int opacity,
      int width,
      int height,
      List<Canvas.Paint> paints,
      List<Canvas.Image> images) {
    public Surface {
      Objects.requireNonNull(horizontal);
      Objects.requireNonNull(vertical);
      paints = List.copyOf(paints);
      images = List.copyOf(images);
      for (var paint : paints) {
        if (paint.x() < 0
            || paint.y() < 0
            || paint.width() < 0
            || paint.height() < 0
            || (long) paint.x() + paint.width() > width
            || (long) paint.y() + paint.height() > height
            || (paint.color() & ~0xFFFFFF) != 0
            || paint.icon() != null && !paint.icon().matches("[a-z][a-z0-9_-]*:[a-z0-9_/-]+"))
          throw new IllegalArgumentException(
              "HUD paint is outside its surface or uses an unsupported icon");
      }
      for (var image : images) {
        Objects.requireNonNull(image.raster());
        if (image.x() < 0
            || image.y() < 0
            || image.width() < 1
            || image.height() < 1
            || (long) image.x() + image.width() > width
            || (long) image.y() + image.height() > height
            || image.pixelSize() < 1
            || image.pixelSize() > 8)
          throw new IllegalArgumentException("HUD image is outside its surface");
      }
      if (opacity < 0 || opacity > 255 || width < 120 || width > 480 || height < 9 || height > 360)
        throw new IllegalArgumentException("Invalid HUD surface dimensions/opacity");
      long left = (long) offsetX - horizontal.ordinal() * width / 2;
      long top = (long) offsetY - vertical.ordinal() * height / 2;
      if (left < -511 || left + width > 511 || top < -360 || top + height > 360)
        throw new IllegalArgumentException("HUD surface exceeds transport coordinate bounds");
    }

    public static Surface of(
        Canvas canvas, Horizontal x, Vertical y, int dx, int dy, double opacity) {
      if (!Double.isFinite(opacity) || opacity < 0 || opacity > 1)
        throw new IllegalArgumentException("HUD opacity must be 0..1");
      canvas = canvas.renderPlan();
      if (!canvas.hits.isEmpty()
          || !canvas.items.isEmpty()
          || !canvas.heads.isEmpty()
          || !canvas.playerModels.isEmpty()
          || !canvas.effects.isEmpty()
          || !canvas.primitives.isEmpty())
        throw new IllegalArgumentException(
            "HUD templates support decorative paints and images; native bodies and dialog actions"
                + " require their own backend");
      return new Surface(
          x,
          y,
          dx,
          dy,
          (int) Math.round(opacity * 255),
          canvas.width,
          canvas.height,
          canvas.paints,
          canvas.images);
    }

    public int x(int x) {
      return offsetX + x - horizontal.ordinal() * width / 2;
    }

    public int y(int y) {
      return offsetY + y - vertical.ordinal() * height / 2;
    }
  }

  public WorldHud {
    surfaces = List.copyOf(surfaces);
    if ((nativeHudColor & ~0xFFFFFF) != 0
        || surfaces.size() > 32
        || surfaces.stream().mapToInt(s -> s.paints().size()).sum() > 2048
        || surfaces.stream()
                .flatMap(s -> s.images().stream())
                .mapToLong(i -> (long) i.raster().width * i.raster().height)
                .sum()
            > 16384)
      throw new IllegalArgumentException("HUD color/surface/paint/image budget exceeded");
  }
}
