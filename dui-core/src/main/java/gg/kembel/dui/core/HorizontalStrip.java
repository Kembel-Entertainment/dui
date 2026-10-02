package gg.kembel.dui.core;

import java.util.*;

/** Centered cells with explicit capacity and spacing. */
public final class HorizontalStrip {
  public record Slot(int index, int x, int y, int width, int height) {}

  private HorizontalStrip() {}

  public static List<Slot> layout(
      int count, int x, int y, int width, int cellWidth, int height, int minimumGap) {
    if (count < 0 || width < 1 || cellWidth < 1 || height < 1 || minimumGap < 0)
      throw new IllegalArgumentException("Horizontal strip geometry");
    if (count == 0) return List.of();
    int capacity = (width + minimumGap) / (cellWidth + minimumGap);
    if (count > capacity)
      throw new IllegalArgumentException(
          "Horizontal strip overflow: page " + count + " cells into capacity " + capacity);
    int stride = cellWidth + minimumGap, start = x + (width - (count * stride - minimumGap)) / 2;
    var result = new ArrayList<Slot>();
    for (int i = 0; i < count; i++)
      result.add(new Slot(i, start + i * stride, y, cellWidth, height));
    return List.copyOf(result);
  }
}
