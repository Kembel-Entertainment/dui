package gg.kembel.dui.core.world;

/** Runtime map-pixel geometry. Integer quantization is shared with the vanilla shader transport. */
public record WorldLayerState(int x, int y, int width, int height, double opacity) {
  public WorldLayerState {
    if (x < -2048
        || x > 2047
        || y < -2048
        || y > 2047
        || width < 1
        || width > 512
        || height < 1
        || height > 512
        || !Double.isFinite(opacity)
        || opacity < 0
        || opacity > 1) throw new IllegalArgumentException("Runtime map geometry range");
  }

  public boolean contains(WorldMapGeometry.Point p) {
    return Math.abs(p.x() - x) <= width / 2. && Math.abs(p.y() - y) <= height / 2.;
  }

  public int color() {
    return ((x + 2048) << 12) | (y + 2048);
  }

  /**
   * A float exactly represents every unsigned 24-bit integer. Dedicated entity Y carries this word.
   */
  public int geometry(int zoom, boolean reducedMotion) {
    if (zoom < 0 || zoom > 31) throw new IllegalArgumentException("Zoom range");
    return (width - 1) | ((height - 1) << 9) | (zoom << 18) | (reducedMotion ? 1 << 23 : 0);
  }

  public static WorldLayerState decode(int color, int geometry, int opacity) {
    return new WorldLayerState(
        ((color >> 12) & 4095) - 2048,
        (color & 4095) - 2048,
        (geometry & 511) + 1,
        ((geometry >> 9) & 511) + 1,
        opacity / 255.);
  }
}
