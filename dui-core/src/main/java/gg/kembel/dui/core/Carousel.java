package gg.kembel.dui.core;

import java.util.*;

public final class Carousel {
  public record Slot(int index, int offset, int x) {}

  private Carousel() {}

  public static List<Slot> window(
      int count, int selected, int sideSlots, int center, int stride, int enteringDirection) {
    if (count < 1
        || selected < 0
        || selected >= count
        || sideSlots < 0
        || stride < 1
        || Math.abs(enteringDirection) > 1) throw new IllegalArgumentException("Carousel bounds");
    var result = new ArrayList<Slot>();
    for (int i = -sideSlots - (enteringDirection == 1 ? 1 : 0);
        i <= sideSlots + (enteringDirection == -1 ? 1 : 0);
        i++) result.add(new Slot(Math.floorMod(selected + i, count), i, center + i * stride));
    return List.copyOf(result);
  }
}
