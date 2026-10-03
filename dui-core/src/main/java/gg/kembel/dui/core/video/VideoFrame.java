package gg.kembel.dui.core.video;

import java.util.Objects;

/** Immutable frame. No Minecraft or emulator types occur in this API. */
public final class VideoFrame {
  private final int width, height;
  private final PixelFormat format;
  private final long sequence;
  private final int[] pixels;
  public VideoFrame(int width, int height, PixelFormat format, long sequence, int[] pixels) {
    this.format = Objects.requireNonNull(format);
    if (width < 1 || height < 1 || width > 4096 || height > 4096
        || Math.multiplyExact(width, height) != pixels.length || sequence < 0)
      throw new IllegalArgumentException("Frame dimensions/sequence");
    this.width = width; this.height = height; this.sequence = sequence;
    this.pixels = pixels.clone();
    for (int pixel : this.pixels) if (pixel < 0 || pixel > format.maximum)
      throw new IllegalArgumentException("Pixel outside " + format);
  }
  public int width() { return width; }
  public int height() { return height; }
  public PixelFormat format() { return format; }
  public long sequence() { return sequence; }
  public int pixel(int x, int y) { return pixels[y * width + x]; }
}
