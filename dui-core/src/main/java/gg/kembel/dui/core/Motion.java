package gg.kembel.dui.core;

import java.util.*;

/** Generic finite native-model motion. Final position is the Canvas item's destination. */
public record Motion(
    long startedAt,
    int duration,
    int delay,
    Easing easing,
    boolean enabled,
    int tx,
    int ty,
    double scaleFrom,
    double scaleTo,
    int rotateFrom,
    int rotateTo,
    double opacityFrom,
    double opacityTo,
    double pivotX,
    double pivotY) {
  public enum Easing {
    LINEAR,
    EASE_OUT,
    EASE_IN_OUT,
    BACK_OUT
  }

  public Motion {
    if (startedAt < 0
        || duration < 1
        || duration > 127
        || delay < 0
        || delay > 127
        || easing == null
        || tx < -256
        || tx > 255
        || ty < -256
        || ty > 255
        || rotateFrom < -256
        || rotateFrom > 255
        || rotateTo < -256
        || rotateTo > 255
        || !finite(scaleFrom, 0, 255. / 64)
        || !finite(scaleTo, 0, 255. / 64)
        || !finite(opacityFrom, 0, 1)
        || !finite(opacityTo, 0, 1)
        || !finite(pivotX, 0, 1)
        || !finite(pivotY, 0, 1)) throw new IllegalArgumentException("Motion transport range");
  }

  private static boolean finite(double v, double lo, double hi) {
    return Double.isFinite(v) && v >= lo && v <= hi;
  }

  public static Motion slide(long tick, int duration, int distance, boolean enabled) {
    return new Motion(
        tick, duration, 0, Easing.EASE_OUT, enabled, distance, 0, 1, 1, 0, 0, 1, 1, .5, .5);
  }

  public static final Set<String> ATTRIBUTES =
      Set.of(
          "translate-x",
          "translate-y",
          "scale-from",
          "scale-to",
          "rotate-from",
          "rotate-to",
          "opacity-from",
          "opacity-to",
          "pivot-x",
          "pivot-y",
          "easing",
          "motion-duration",
          "motion-delay",
          "motion-start");

  /** One parser shared by native models and optional procedural components. */
  public static Optional<Motion> from(Map<String, String> p, long defaultStart, boolean enabled) {
    if (ATTRIBUTES.stream().noneMatch(p::containsKey)) return Optional.empty();
    return Optional.of(
        new Motion(
            Long.parseLong(p.getOrDefault("motion-start", Long.toString(defaultStart))),
            Integer.parseInt(p.getOrDefault("motion-duration", "24")),
            Integer.parseInt(p.getOrDefault("motion-delay", "0")),
            Easing.valueOf(p.getOrDefault("easing", "ease_out").toUpperCase(Locale.ROOT)),
            enabled,
            Integer.parseInt(p.getOrDefault("translate-x", "0")),
            Integer.parseInt(p.getOrDefault("translate-y", "0")),
            Double.parseDouble(p.getOrDefault("scale-from", "1")),
            Double.parseDouble(p.getOrDefault("scale-to", "1")),
            Integer.parseInt(p.getOrDefault("rotate-from", "0")),
            Integer.parseInt(p.getOrDefault("rotate-to", "0")),
            Double.parseDouble(p.getOrDefault("opacity-from", "1")),
            Double.parseDouble(p.getOrDefault("opacity-to", "1")),
            Double.parseDouble(p.getOrDefault("pivot-x", "0.5")),
            Double.parseDouble(p.getOrDefault("pivot-y", "0.5"))));
  }

  public Motion still() {
    return new Motion(
        startedAt,
        duration,
        delay,
        easing,
        false,
        tx,
        ty,
        scaleFrom,
        scaleTo,
        rotateFrom,
        rotateTo,
        opacityFrom,
        opacityTo,
        pivotX,
        pivotY);
  }

  public List<Integer> payload() {
    int[] bits = new int[RendererProtocol.MOTION_CELLS * 3];
    set(bits, RendererProtocol.DURATION_OFFSET, RendererProtocol.DURATION_BITS, duration);
    set(bits, RendererProtocol.DELAY_OFFSET, RendererProtocol.DELAY_BITS, delay);
    set(bits, RendererProtocol.EASING_OFFSET, RendererProtocol.EASING_BITS, easing.ordinal());
    set(bits, RendererProtocol.ENABLED_OFFSET, RendererProtocol.ENABLED_BITS, enabled ? 1 : 0);
    set(bits, RendererProtocol.TX_OFFSET, RendererProtocol.TX_BITS, tx + 256);
    set(bits, RendererProtocol.TY_OFFSET, RendererProtocol.TY_BITS, ty + 256);
    set(
        bits,
        RendererProtocol.SCALEFROM_OFFSET,
        RendererProtocol.SCALEFROM_BITS,
        (int) Math.round(scaleFrom * 64));
    set(
        bits,
        RendererProtocol.SCALETO_OFFSET,
        RendererProtocol.SCALETO_BITS,
        (int) Math.round(scaleTo * 64));
    set(
        bits,
        RendererProtocol.ROTATEFROM_OFFSET,
        RendererProtocol.ROTATEFROM_BITS,
        rotateFrom + 256);
    set(bits, RendererProtocol.ROTATETO_OFFSET, RendererProtocol.ROTATETO_BITS, rotateTo + 256);
    set(
        bits,
        RendererProtocol.OPACITYFROM_OFFSET,
        RendererProtocol.OPACITYFROM_BITS,
        (int) Math.round(opacityFrom * 255));
    set(
        bits,
        RendererProtocol.OPACITYTO_OFFSET,
        RendererProtocol.OPACITYTO_BITS,
        (int) Math.round(opacityTo * 255));
    set(
        bits,
        RendererProtocol.PIVOTX_OFFSET,
        RendererProtocol.PIVOTX_BITS,
        (int) Math.round(pivotX * 255));
    set(
        bits,
        RendererProtocol.PIVOTY_OFFSET,
        RendererProtocol.PIVOTY_BITS,
        (int) Math.round(pivotY * 255));
    var result = new ArrayList<Integer>();
    for (int i = 0; i < bits.length; i += 3)
      result.add((bits[i] * 255 << 16) | (bits[i + 1] * 255 << 8) | bits[i + 2] * 255);
    return result;
  }

  private static void set(int[] out, int offset, int count, int value) {
    if (value < 0 || value >= 1 << count)
      throw new IllegalArgumentException("Motion field overflow");
    for (int i = 0; i < count; i++) out[offset + i] = value >> i & 1;
  }
}
