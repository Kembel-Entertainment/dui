package gg.kembel.dui.core;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Runtime RGB pixels. Images never become resource-pack assets. */
public final class RasterImage {
  public final int width, height;
  private final int[] rgb;
  private final boolean alpha;

  public RasterImage(int width, int height, int[] rgb) {
    if (width < 1 || height < 1 || width > 512 || height > 512 || rgb.length != width * height)
      throw new IllegalArgumentException("Invalid raster dimensions");
    this.width = width;
    this.height = height;
    this.rgb = rgb.clone();
    this.alpha = false;
    for (int i = 0; i < this.rgb.length; i++) this.rgb[i] = 0xff000000 | (this.rgb[i] & 0xFFFFFF);
  }

  private RasterImage(int width, int height, int[] argb, boolean alpha) {
    if (width < 1
        || height < 1
        || width > 512
        || height > 512
        || argb.length != (long) width * height)
      throw new IllegalArgumentException("Invalid raster dimensions");
    this.width = width;
    this.height = height;
    this.rgb = argb.clone();
    this.alpha = alpha;
  }

  public static RasterImage argb(int width, int height, int[] pixels) {
    return new RasterImage(width, height, pixels, true);
  }

  public boolean hasAlpha() {
    return alpha;
  }

  public int argb(int x, int y) {
    return rgb[y * width + x];
  }

  public int[] pixels() {
    return rgb.clone();
  }

  public int rgb(int x, int y) {
    return rgb[y * width + x] & 0xffffff;
  }

  public static RasterImage decode(byte[] bytes, int background) throws IOException {
    try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) throw new IOException("Unsupported thumbnail image");
      var reader = readers.next();
      try {
        reader.setInput(input);
        int w = reader.getWidth(0), h = reader.getHeight(0);
        if (w < 1 || h < 1 || w > 4096 || h > 4096 || (long) w * h > 4_000_000)
          throw new IOException("Thumbnail dimensions too large");
        var image = reader.read(0);
        double scale = Math.min(1, 256.0 / Math.max(w, h));
        return new RgbaImage(w, h, image.getRGB(0, 0, w, h, null, 0, w))
            .flatten(
                Math.max(1, (int) (w * scale)),
                Math.max(1, (int) (h * scale)),
                RgbaImage.Fit.CONTAIN,
                RgbaImage.Sampling.BILINEAR,
                background);
      } finally {
        reader.dispose();
      }
    }
  }

  public RasterImage cover(int w, int h) {
    if (w < 1 || h < 1 || w > 512 || h > 512)
      throw new IllegalArgumentException("Invalid image target");
    var source = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    source.setRGB(0, 0, width, height, rgb, 0, width);
    var target = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    var g = target.createGraphics();
    try {
      double scale = Math.max((double) w / width, (double) h / height);
      int dw = (int) Math.ceil(width * scale), dh = (int) Math.ceil(height * scale);
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g.drawImage(source, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
    } finally {
      g.dispose();
    }
    return alpha
        ? RasterImage.argb(w, h, target.getRGB(0, 0, w, h, null, 0, w))
        : new RasterImage(w, h, target.getRGB(0, 0, w, h, null, 0, w));
  }
}
