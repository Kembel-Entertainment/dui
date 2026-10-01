package gg.kembel.dui.core;

import java.util.*;

/**
 * Visual layout only; card values, hidden information and game eligibility remain
 * application-owned.
 */
public final class CardStrip {
  public record Slot(int index, int x, int y, int width, int height) {}

  private CardStrip() {}

  public static List<Slot> layout(
      int count, int x, int y, int width, int cardWidth, int height, int minimumGap) {
    if (count < 0 || width < 1 || cardWidth < 1 || height < 1 || minimumGap < 0)
      throw new IllegalArgumentException("Card strip geometry");
    if (count == 0) return List.of();
    int capacity = (width + minimumGap) / (cardWidth + minimumGap);
    if (count > capacity)
      throw new IllegalArgumentException(
          "Card strip overflow: page " + count + " cards into capacity " + capacity);
    int stride = cardWidth + minimumGap, start = x + (width - (count * stride - minimumGap)) / 2;
    var result = new ArrayList<Slot>();
    for (int i = 0; i < count; i++)
      result.add(new Slot(i, start + i * stride, y, cardWidth, height));
    return List.copyOf(result);
  }
}
