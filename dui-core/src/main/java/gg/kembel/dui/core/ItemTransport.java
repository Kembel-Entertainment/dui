package gg.kembel.dui.core;

import java.util.*;

/** Versioned native-item and shared-effect transport encoding. */
public final class ItemTransport {
  public static final int DATA_INDEX = 32,
      BURST_TICKS = 96,
      NATIVE_CELLS = 47 + RendererProtocol.MOTION_CELLS;

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
        (dx + 1024L)
            | ((dy + 1024L) << RendererProtocol.BASE_DY_OFFSET)
            | ((long) size << RendererProtocol.BASE_SIZE_OFFSET)
            | (confetti ? 1L << RendererProtocol.BASE_EXPANDED_OFFSET : 0);
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
            | ((long) width << RendererProtocol.HEADER_WIDTH_OFFSET)
            | ((long) height << RendererProtocol.HEADER_HEIGHT_OFFSET)
            | ((long) x << RendererProtocol.HEADER_X_OFFSET)
            | ((long) y << RendererProtocol.HEADER_Y_OFFSET);
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
            | ((long) transition.kind().ordinal() << RendererProtocol.TRANSITION_KIND_OFFSET)
            | ((long) Math.abs(transition.distance())
                << RendererProtocol.TRANSITION_DISTANCE_OFFSET)
            | (transition.motion() && canvas.motionEnabled
                ? 1L << RendererProtocol.TRANSITION_ENABLED_OFFSET
                : 0)
            | (transition.distance() < 0 ? 1L << RendererProtocol.TRANSITION_SIGN_OFFSET : 0);
    payload.addAll(colors(parameters, 6));
    if (clip != null) {
      long clipping =
          (clip.x() - item.x() + 512L)
              | ((clip.y() - item.y() + 512L) << RendererProtocol.CLIP_Y_OFFSET)
              | ((long) clip.width() << RendererProtocol.CLIP_WIDTH_OFFSET)
              | ((long) clip.height() << RendererProtocol.CLIP_HEIGHT_OFFSET);
      payload.addAll(colors(clipping, 13));
    }
    return payload;
  }

  public static List<Integer> animationPayload(Canvas canvas) {
    return animationPayload(canvas, canvas.effects);
  }

  public static List<Integer> animationPayload(Canvas canvas, List<ShaderEffect> effects) {
    var animation = Objects.requireNonNull(canvas.animation);
    if (effects.isEmpty() || effects.size() > RendererProtocol.CARRIER_EFFECTS)
      throw new IllegalArgumentException("Invalid shader component count");
    boolean tracked = effects.stream().anyMatch(e -> canvas.effectMotions.containsKey(e.id()));
    if (tracked && effects.size() > 2)
      throw new IllegalArgumentException("Tracked effects use batches of two");
    boolean extended =
        tracked || effects.stream().anyMatch(e -> e.kind() instanceof ShaderEffect.Extension);
    long data =
        Math.max(0, animation.startedAt()) % 24000
            | ((long) canvas.width << RendererProtocol.HEADER_WIDTH_OFFSET)
            | ((long) canvas.height << RendererProtocol.HEADER_HEIGHT_OFFSET)
            | ((long) effects.size() << RendererProtocol.HEADER_X_OFFSET)
            | ((tracked ? 7L : extended ? 6L : 1L) << RendererProtocol.HEADER_KIND_OFFSET);
    var payload = new ArrayList<>(colors(data, 18));
    for (var effect : effects) {
      payload.addAll(extended ? extendedEffectPayload(effect) : effectPayload(effect));
      if (tracked) {
        var motion =
            canvas.effectMotions.getOrDefault(
                effect.id(),
                new Motion(
                    Math.max(0, animation.startedAt()),
                    1,
                    0,
                    Motion.Easing.LINEAR,
                    false,
                    0,
                    0,
                    1,
                    1,
                    0,
                    0,
                    1,
                    1,
                    .5,
                    .5));
        if (motion.enabled() && motion.startedAt() != animation.startedAt())
          throw new IllegalArgumentException("Effect motion must use canvas animation-start");
        payload.addAll((canvas.motionEnabled ? motion : motion.still()).payload());
      }
    }
    return payload;
  }

  public static List<Integer> motionPayload(Canvas canvas, Canvas.Item item, Motion motion) {
    if (!canvas.motionEnabled) motion = motion.still();
    var result =
        new ArrayList<>(
            confettiPayload(
                motion.startedAt(),
                canvas.width,
                canvas.height,
                Math.clamp(item.x(), 0, canvas.width - 1),
                Math.clamp(item.y(), 0, canvas.height - 1)));
    var clip = canvas.clips.get(item.id());
    result.set(17, clip == null ? 0x0000FF : 0xFF00FF);
    result.addAll(Collections.nCopies(6, 0));
    long clipping =
        clip == null
            ? 0
            : (clip.x() - item.x() + 512L)
                | ((clip.y() - item.y() + 512L) << RendererProtocol.CLIP_Y_OFFSET)
                | ((long) clip.width() << RendererProtocol.CLIP_WIDTH_OFFSET)
                | ((long) clip.height() << RendererProtocol.CLIP_HEIGHT_OFFSET);
    result.addAll(colors(clipping, 13));
    result.addAll(motion.payload());
    return result;
  }

  /** 69 bits per component: type, four 9-bit bounds and two 15-bit typed parameters. */
  public static List<Integer> effectPayload(ShaderEffect effect) {
    long data =
        effect.kind().code()
            | ((long) effect.x() << RendererProtocol.EFFECT_X_OFFSET)
            | ((long) effect.y() << RendererProtocol.EFFECT_Y_OFFSET)
            | ((long) effect.width() << RendererProtocol.EFFECT_WIDTH_OFFSET)
            | ((long) effect.height() << RendererProtocol.EFFECT_HEIGHT_OFFSET)
            | ((long) effect.parameter0() << RendererProtocol.EFFECT_PARAM0_OFFSET);
    var payload = new ArrayList<>(colors(data, 18));
    payload.addAll(colors(effect.parameter1(), 5));
    return payload;
  }

  public static List<Integer> extendedEffectPayload(ShaderEffect effect) {
    long data =
        effect.kind().code()
            | ((long) effect.x() << RendererProtocol.EXTENDED_X_OFFSET)
            | ((long) effect.y() << RendererProtocol.EXTENDED_Y_OFFSET)
            | ((long) effect.width() << RendererProtocol.EXTENDED_WIDTH_OFFSET)
            | ((long) effect.height() << RendererProtocol.EXTENDED_HEIGHT_OFFSET)
            | ((long) effect.parameter0() << RendererProtocol.EXTENDED_PARAM0_OFFSET);
    var result = new ArrayList<>(colors(data, 19));
    result.addAll(colors(effect.parameter1(), 5));
    return result;
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
