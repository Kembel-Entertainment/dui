package gg.kembel.dui.core;

import java.util.*;

/** Immutable ARGB source; flattening always receives an explicit RGB background. */
public final class RgbaImage {
  public enum Fit {
    COVER,
    CONTAIN
  }

  public enum Sampling {
    NEAREST,
    BILINEAR
  }

  private final int width, height;
  private final int[] pixels;

  public RgbaImage(int width, int height, int[] argb) {
    if (width < 1
        || height < 1
        || (long) width * height > 16777216
        || argb.length != (long) width * height)
      throw new IllegalArgumentException("RGBA image bounds");
    this.width = width;
    this.height = height;
    this.pixels = argb.clone();
  }

  public RasterImage flatten(int targetW, int targetH, Fit fit, Sampling sampling, int background) {
    if (targetW < 1
        || targetH < 1
        || targetW > 512
        || targetH > 512
        || (long) targetW * targetH > 16777216
        || background < 0
        || background > 0xffffff) throw new IllegalArgumentException("Image fit bounds");
    double scale =
        fit == Fit.COVER
            ? Math.max((double) targetW / width, (double) targetH / height)
            : Math.min((double) targetW / width, (double) targetH / height);
    double ox = (targetW - width * scale) / 2, oy = (targetH - height * scale) / 2;
    int[] out = new int[targetW * targetH];
    for (int y = 0; y < targetH; y++)
      for (int x = 0; x < targetW; x++) {
        double sx = (x + .5 - ox) / scale - .5, sy = (y + .5 - oy) / scale - .5;
        if (x + .5 < ox
            || y + .5 < oy
            || x + .5 >= ox + width * scale
            || y + .5 >= oy + height * scale) {
          out[y * targetW + x] = background;
          continue;
        }
        if (sampling == Sampling.NEAREST)
          out[y * targetW + x] =
              blend(pixel((int) Math.round(sx), (int) Math.round(sy)), background);
        else {
          int xx = (int) Math.floor(sx), yy = (int) Math.floor(sy);
          double fx = sx - xx, fy = sy - yy;
          int a = blend(pixel(xx, yy), background),
              b = blend(pixel(xx + 1, yy), background),
              c = blend(pixel(xx, yy + 1), background),
              d = blend(pixel(xx + 1, yy + 1), background);
          int rgb = 0;
          for (int shift : new int[] {16, 8, 0})
            rgb |=
                ((int)
                        Math.round(
                            ((a >> shift & 255) * (1 - fx) + (b >> shift & 255) * fx) * (1 - fy)
                                + ((c >> shift & 255) * (1 - fx) + (d >> shift & 255) * fx) * fy))
                    << shift;
          out[y * targetW + x] = rgb;
        }
      }
    return new RasterImage(targetW, targetH, out);
  }

  private int pixel(int x, int y) {
    return pixels[
        Math.max(0, Math.min(height - 1, y)) * width + Math.max(0, Math.min(width - 1, x))];
  }

  private static int blend(int argb, int bg) {
    int a = argb >>> 24, rgb = 0;
    for (int s : new int[] {16, 8, 0})
      rgb |= (((argb >> s & 255) * a + (bg >> s & 255) * (255 - a) + 127) / 255) << s;
    return rgb;
  }
}
