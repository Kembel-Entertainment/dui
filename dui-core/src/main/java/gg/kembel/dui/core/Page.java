package gg.kembel.dui.core;

import java.util.*;

/** Immutable, zero-indexed page. Empty collections still have page zero of one. */
public record Page<T>(List<T> items, int index, int pages, int total, int size) {
  public Page {
    items = List.copyOf(items);
    if (size < 1
        || total < 0
        || pages != count(total, size)
        || index < 0
        || index >= pages
        || items.size() != Math.min(size, total - (long) index * size))
      throw new IllegalArgumentException("Invalid page");
  }

  public static int count(int total, int size) {
    if (size < 1 || total < 0) throw new IllegalArgumentException("Page size must be positive");
    return Math.max(1, (int) (((long) total + size - 1) / size));
  }

  public static <T> Page<T> of(List<T> source, int requested, int size) {
    int pages = count(source.size(), size), index = Math.clamp(requested, 0, pages - 1);
    int from = (int) Math.min((long) index * size, source.size());
    return new Page<>(
        source.subList(from, (int) Math.min((long) from + size, source.size())),
        index,
        pages,
        source.size(),
        size);
  }

  public boolean hasPrevious() {
    return index > 0;
  }

  public boolean hasNext() {
    return index + 1 < pages;
  }
}
