package gg.kembel.dui.core.video;

/** Lossless source formats. BGR555 has red in bits 0..4, green 5..9, blue 10..14. */
public enum PixelFormat {
  BGR555(2, 0x7fff), RGB888(4, 0xffffff);
  public final int symbols, maximum;
  PixelFormat(int symbols, int maximum) { this.symbols = symbols; this.maximum = maximum; }
  public int rgb(int value) {
    if (this == RGB888) return value;
    int r = value & 31, g = (value >> 5) & 31, b = (value >> 10) & 31;
    return ((r << 3 | r >> 2) << 16) | ((g << 3 | g >> 2) << 8) | (b << 3 | b >> 2);
  }
}
