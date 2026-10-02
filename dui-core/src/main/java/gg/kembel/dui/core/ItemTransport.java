package gg.kembel.dui.core;

import java.util.*;

/** Versioned native-item and shared-effect transport encoding. */
public final class ItemTransport {
  public static final int DATA_INDEX = 32, NATIVE_CELLS = 47 + RendererProtocol.MOTION_CELLS;

  private ItemTransport() {}

  /**
   * Three bits per RGB cell; threshold decoding survives vanilla lighting and enchantment glint.
   */
  public static List<Integer> payload(int dx, int dy, int size) {
    return payload(dx, dy, size, false);
  }

  public static List<Integer> payload(int dx, int dy, int size, boolean expanded) {
    if (dx < -1024 || dx > 1023 || dy < -1024 || dy > 1023 || size < 1 || size > 127)
      throw new IllegalArgumentException("Shader item offset/size out of range");
    long data =
        (dx + 1024L)
            | ((dy + 1024L) << RendererProtocol.BASE_DY_OFFSET)
            | ((long) size << RendererProtocol.BASE_SIZE_OFFSET)
            | (expanded ? 1L << RendererProtocol.BASE_EXPANDED_OFFSET : 0);
    return colors(data, 10);
  }

  /** 51 bits, outside the native model crop: start tick, canvas bounds, item origin. */
  public static List<Integer> headerPayload(long tick, int width, int height, int x, int y) {
    if (tick < 0
        || width < 1
        || width > 480
        || height < 1
        || height > 360
        || x < 0
        || x >= width
        || y < 0
        || y >= height) throw new IllegalArgumentException("Render header bounds");
    long data =
        tick % 24000
            | ((long) width << RendererProtocol.HEADER_WIDTH_OFFSET)
            | ((long) height << RendererProtocol.HEADER_HEIGHT_OFFSET)
            | ((long) x << RendererProtocol.HEADER_X_OFFSET)
            | ((long) y << RendererProtocol.HEADER_Y_OFFSET);
    return colors(data, 18);
  }

  public static List<Integer> animationPayload(
      Canvas canvas, List<ShaderInvocation> effects, Map<String, ShaderBinding> bindings) {
    var animation = Objects.requireNonNull(canvas.animation);
    if (effects.isEmpty() || effects.size() > RendererProtocol.CARRIER_EFFECTS)
      throw new IllegalArgumentException("Shader batch count");
    boolean tracked = canvas.effectMotions.containsKey(effects.getFirst().id());
    long header =
        Math.max(0, animation.startedAt()) % 24000
            | ((long) canvas.width << RendererProtocol.HEADER_WIDTH_OFFSET)
            | ((long) canvas.height << RendererProtocol.HEADER_HEIGHT_OFFSET)
            | ((long) effects.size() << RendererProtocol.HEADER_X_OFFSET)
            | ((tracked ? 6L : 1L) << RendererProtocol.HEADER_KIND_OFFSET);
    var payload = new ArrayList<>(colors(header, 18));
    var bits = new ShaderParameters.Bits();
    for (var effect : effects) {
      if (canvas.effectMotions.containsKey(effect.id()) != tracked)
        throw new IllegalArgumentException("Mixed shader motion batch");
      bits.put(ShaderRegistry.require(bindings, effect.shader()).code(), 6);
      bits.put(effect.x(), 9);
      bits.put(effect.y(), 9);
      bits.put(effect.width(), 9);
      bits.put(effect.height(), 9);
      for (var p : effect.shader().parameters())
        bits.put(p.encode(effect.parameters().get(p.name())), p.bits());
      if (tracked) {
        var motion = canvas.effectMotions.get(effect.id());
        if (motion.enabled() && motion.startedAt() != animation.startedAt())
          throw new IllegalArgumentException("Shader motion must share animation-start");
        bits.cells((canvas.motionEnabled ? motion : motion.still()).payload(), 99);
      }
    }
    if (bits.size() > 576) throw new IllegalArgumentException("Shader batch transport overflow");
    payload.addAll(bits.colors());
    return List.copyOf(payload);
  }

  public static List<Integer> motionPayload(Canvas canvas, Canvas.Item item, Motion motion) {
    if (!canvas.motionEnabled) motion = motion.still();
    var result =
        new ArrayList<>(
            headerPayload(
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
