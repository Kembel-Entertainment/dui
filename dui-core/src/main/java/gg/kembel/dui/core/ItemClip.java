package gg.kembel.dui.core;

/** A fixed canvas-space viewport for a native item, including its animated pixels. */
public record ItemClip(int x, int y, int width, int height) {
  public ItemClip {
    if (x < 0 || y < 0 || width < 1 || height < 1 || x + width > 480 || y + height > 360)
      throw new IllegalArgumentException("Item clip must fit the maximum canvas bounds");
  }
}
