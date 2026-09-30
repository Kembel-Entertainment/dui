package gg.kembel.dui.core;

import java.util.*;

/** Versioned native-item and shared-effect transport encoding. */
public final class ItemTransport {
  public static final int DATA_INDEX = 32, BURST_TICKS = 96, NATIVE_CELLS = 47;

  private ItemTransport() {}

  /**
   * Three bits per RGB cell; threshold decoding survives vanilla lighting and enchantment glint.
   */
  public static List<Integer> payload(int dx, int dy, int size) {
    return payload(dx, dy, size, false);
  }

  public static List<Integer> payload(int dx, int dy, int size, boolean confetti) {
    if (dx < -1024 || dx > 1023 || dy < -1024 || dy > 1023 || size < 1 || size > 127)
      throw new IllegalArgumentException("Shader item offset/size out of range");
    long data =
        (dx + 1024L) | ((dy + 1024L) << 11) | ((long) size << 22) | (confetti ? 1L << 29 : 0);
    return colors(data, 10);
  }

  /** 51 bits, outside the native model crop: start tick, canvas bounds, item origin. */
  public static List<Integer> confettiPayload(long tick, int width, int height, int x, int y) {
    if (tick < 0
        || width < 1
        || width > 480
        || height < 1
        || height > 360
        || x < 0
        || x >= width
        || y < 0
        || y >= height) throw new IllegalArgumentException("Confetti bounds");
    long data =
        tick % 24000
            | ((long) width << 15)
            | ((long) height << 24)
            | ((long) x << 33)
            | ((long) y << 42);
    return colors(data, 18);
  }

  /**
   * Kind 2/3 native header, six motion cells and optional thirteen clip cells; crop is retained.
   */
  public static List<Integer> transitionPayload(
      Canvas canvas, Canvas.Item item, ItemTransition transition) {
    var payload =
        new ArrayList<>(
            confettiPayload(
                transition.startedAt(),
                canvas.width,
                canvas.height,
                Math.clamp(item.x(), 0, canvas.width - 1),
                Math.clamp(item.y(), 0, canvas.height - 1)));
    // Last header cell holds the three-bit transport kind (bits 51..53).
    var clip = canvas.clips.get(item.id());
    payload.set(17, clip == null ? 0x00FF00 : 0xFFFF00);
    long parameters =
        transition.durationTicks()
            | ((long) transition.kind().ordinal() << 7)
            | ((long) Math.abs(transition.distance()) << 9)
            | (transition.motion() ? 1L << 16 : 0)
            | (transition.distance() < 0 ? 1L << 17 : 0);
    payload.addAll(colors(parameters, 6));
    if (clip != null) {
      long clipping =
          (clip.x() - item.x() + 512L)
              | ((clip.y() - item.y() + 512L) << 10)
              | ((long) clip.width() << 20)
              | ((long) clip.height() << 29);
      payload.addAll(colors(clipping, 13));
    }
    return payload;
  }

  public static List<Integer> animationPayload(Canvas canvas) {
    var animation = Objects.requireNonNull(canvas.animation);
    if (canvas.effects.isEmpty() || canvas.effects.size() > ShaderEffect.LIMIT)
      throw new IllegalArgumentException("Invalid shader component count");
    long data =
        Math.max(0, animation.startedAt()) % 24000
            | ((long) canvas.width << 15)
            | ((long) canvas.height << 24)
            | ((long) canvas.effects.size() << 33)
            | (1L << 51);
    var payload = new ArrayList<>(colors(data, 18));
    for (var effect : canvas.effects) payload.addAll(effectPayload(effect));
    return payload;
  }

  /** 69 bits per component: type, four 9-bit bounds and two 15-bit typed parameters. */
  public static List<Integer> effectPayload(ShaderEffect effect) {
    long data =
        effect.kind().code
            | ((long) effect.x() << 3)
            | ((long) effect.y() << 12)
            | ((long) effect.width() << 21)
            | ((long) effect.height() << 30)
            | ((long) effect.parameter0() << 39);
    var payload = new ArrayList<>(colors(data, 18));
    payload.addAll(colors(effect.parameter1(), 5));
    return payload;
  }

  private static List<Integer> colors(long data, int count) {
    var colors = new ArrayList<Integer>();
    for (int i = 0; i < count; i++) {
      int bits = (int) (data >>> (3 * i)) & 7;
      colors.add(
          ((bits & 1) != 0 ? 0xFF0000 : 0)
              | ((bits & 2) != 0 ? 0x00FF00 : 0)
              | ((bits & 4) != 0 ? 0x0000FF : 0));
    }
    return colors;
  }
}
