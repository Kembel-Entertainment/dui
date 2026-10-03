package gg.kembel.dui.core.video;

import java.util.*;

/** v1: row zero is a static header; remaining rows carry base-192 symbols, not visible map colors. */
public final class MapVideoCodec {
  public static final String CAPABILITY = "map-video-v1";
  public static final int SIZE = 128, PAYLOAD_HEIGHT = 127, RADIX = 192, VERSION = 1;
  public record Tile(int x, int y, int width, int height, byte[] colors) {
    public Tile { colors = colors.clone(); }
    @Override public byte[] colors() { return colors.clone(); }
  }
  public record Patch(int x, int y, int width, int height, byte[] colors) {
    public Patch { colors = colors.clone(); }
    @Override public byte[] colors() { return colors.clone(); }
  }
  private MapVideoCodec() {}
  public static byte symbol(int value) {
    if (value < 0 || value >= RADIX) throw new IllegalArgumentException("Symbol");
    return (byte) (value + 4);
  }
  public static int decodeSymbol(byte value) {
    int n = Byte.toUnsignedInt(value) - 4;
    if (n < 0 || n >= RADIX) throw new IllegalArgumentException("Invalid symbol");
    return n;
  }
  public static void put(byte[] colors, int at, int value, int symbols) {
    for (int i = 0; i < symbols; i++) { colors[at + i] = symbol(value % RADIX); value /= RADIX; }
    if (value != 0) throw new IllegalArgumentException("Field overflow");
  }
  public static int get(byte[] colors, int at, int symbols) {
    int value = 0;
    for (int i = symbols - 1; i >= 0; i--) value = value * RADIX + decodeSymbol(colors[at + i]);
    return value;
  }
  private static byte[] header(VideoSurfaceSpec s, int format, int x, int y, int w, int h) {
    byte[] out = new byte[SIZE * SIZE]; Arrays.fill(out, symbol(0));
    for (int i = 0; i < 4; i++) out[i] = (byte) (244 + i);
    out[4] = symbol(VERSION); out[5] = symbol(format);
    int[] values = {s.width(), s.height(), x, y, w, h,
        (int) Math.round(s.viewport().left() * 4095), (int) Math.round(s.viewport().top() * 4095),
        (int) Math.round(s.viewport().right() * 4095), (int) Math.round(s.viewport().bottom() * 4095)};
    for (int i = 0; i < values.length; i++) put(out, 6 + i * 2, values[i], 2);
    out[26] = symbol(s.integerScaling() ? 1 : 0);
    put(out, 27, s.backgroundRgb(), 4);
    return out;
  }
  public static List<Tile> encode(VideoSurfaceSpec spec, VideoFrame frame) {
    spec.validate(frame);
    int step = SIZE / spec.format().symbols;
    var tiles = new ArrayList<Tile>();
    tiles.add(new Tile(0, 0, 0, 0, header(spec, 2, 0, 0, 0, 0)));
    for (int y = 0; y < frame.height(); y += PAYLOAD_HEIGHT)
      for (int x = 0; x < frame.width(); x += step) {
        int w = Math.min(step, frame.width() - x), h = Math.min(PAYLOAD_HEIGHT, frame.height() - y);
        byte[] colors = header(spec, spec.format().ordinal(), x, y, w, h);
        for (int row = 0; row < h; row++) for (int col = 0; col < w; col++)
          put(colors, (row + 1) * SIZE + col * spec.format().symbols,
              frame.pixel(x + col, y + row), spec.format().symbols);
        tiles.add(new Tile(x, y, w, h, colors));
      }
    return List.copyOf(tiles);
  }
  /** Delta against the last frame accepted by the network writer, never a discarded frame. */
  public static Patch difference(byte[] before, byte[] after) {
    if (after.length != SIZE * SIZE || before != null && before.length != after.length)
      throw new IllegalArgumentException("Tile dimensions");
    if (before == null) return new Patch(0, 0, SIZE, SIZE, after);
    int left = SIZE, right = -1, top = SIZE, bottom = -1;
    for (int i = 0; i < after.length; i++) if (before[i] != after[i]) {
      int x = i % SIZE, y = i / SIZE;
      left = Math.min(left, x); right = Math.max(right, x);
      top = Math.min(top, y); bottom = Math.max(bottom, y);
    }
    if (right < 0) return null;
    int width = right - left + 1, height = bottom - top + 1;
    byte[] colors = new byte[width * height];
    for (int row = 0; row < height; row++)
      System.arraycopy(after, (top + row) * SIZE + left, colors, row * width, width);
    return new Patch(left, top, width, height, colors);
  }
}
