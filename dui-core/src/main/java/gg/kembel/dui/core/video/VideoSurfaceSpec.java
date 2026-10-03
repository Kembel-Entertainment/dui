package gg.kembel.dui.core.video;

import java.util.Objects;

/** Consumer-owned geometry and budgets; the transport's 128-pixel map size is platform-defined. */
public record VideoSurfaceSpec(int width, int height, PixelFormat format, double maximumFps,
    Viewport viewport, Budget budget, int backgroundRgb, boolean integerScaling) {
  public record Viewport(double left, double top, double right, double bottom) {
    public static final Viewport FULL = new Viewport(0, 0, 1, 1);
    public Viewport {
      if (!Double.isFinite(left + top + right + bottom) || left < 0 || top < 0
          || right > 1 || bottom > 1 || right <= left || bottom <= top)
        throw new IllegalArgumentException("Normalized viewport");
    }
  }
  public record Budget(int maximumTiles, long bytesPerSecond) {
    public Budget {
      if (maximumTiles < 1 || maximumTiles > 512 || bytesPerSecond < 16384)
        throw new IllegalArgumentException("Video budget");
    }
  }
  public VideoSurfaceSpec {
    Objects.requireNonNull(format); Objects.requireNonNull(viewport); Objects.requireNonNull(budget);
    if (width < 1 || height < 1 || width > 4096 || height > 4096
        || !Double.isFinite(maximumFps) || maximumFps < 1 || maximumFps > 240
        || backgroundRgb < 0 || backgroundRgb > 0xffffff)
      throw new IllegalArgumentException("Video specification");
    int columns = (width + 128 / format.symbols - 1) / (128 / format.symbols);
    int rows = (height + 126) / 127;
    if ((long) columns * rows + 1 > budget.maximumTiles)
      throw new IllegalArgumentException("Video exceeds tile budget");
    if ((long) (columns * rows + 1) * (128 * 128 + 32)
        > budget.bytesPerSecond)
      throw new IllegalArgumentException("Budget cannot carry one complete frame");
  }
  public void validate(VideoFrame frame) {
    if (frame.width() != width || frame.height() != height || frame.format() != format)
      throw new IllegalArgumentException("Frame does not match surface");
  }
}
