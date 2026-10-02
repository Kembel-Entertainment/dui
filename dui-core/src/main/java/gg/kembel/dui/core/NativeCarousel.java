package gg.kembel.dui.core;

import java.util.*;

/**
 * A resource-driven carousel using genuine item models. Caller supplies stacks for returned carrier
 * ids.
 */
public final class NativeCarousel {
  public record Symbol(String carrierId, String resourceKey) {}

  private NativeCarousel() {}

  public static List<Symbol> draw(
      Canvas canvas,
      String id,
      List<String> resources,
      int selected,
      int x,
      int y,
      int width,
      int height,
      int itemSize,
      long tick,
      int duration,
      int travel,
      boolean motion) {
    if (resources.size() < 2
        || resources.size() > 64
        || new HashSet<>(resources).size() != resources.size()
        || selected < 0
        || selected >= resources.size()
        || itemSize < 1
        || itemSize > 127
        || height < itemSize
        || width < itemSize)
      throw new IllegalArgumentException("Native carousel geometry/symbol resources");
    var clip = new ItemClip(x, y, width, height);
    int slots = (height + itemSize - 1) / itemSize + 2;
    var result = new ArrayList<Symbol>();
    for (int offset = -slots / 2; offset <= slots / 2; offset++) {
      int index = Math.floorMod(selected + offset, resources.size());
      String carrier = id + "_slot_" + (offset + slots / 2);
      canvas.item(
          carrier,
          x + (width - itemSize) / 2,
          y + (height - itemSize) / 2 + offset * itemSize,
          itemSize,
          clip);
      canvas.motion(
          carrier,
          new Motion(
              tick,
              duration,
              0,
              Motion.Easing.EASE_OUT,
              motion,
              0,
              travel,
              1,
              1,
              0,
              0,
              1,
              1,
              .5,
              .5));
      result.add(new Symbol(carrier, resources.get(index)));
    }
    return List.copyOf(result);
  }
}
