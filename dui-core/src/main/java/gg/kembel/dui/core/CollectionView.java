package gg.kembel.dui.core;

import java.util.*;
import java.util.function.Function;

/** Stable identities survive page changes. No index-derived callback keys. */
public record CollectionView<T>(Page<T> page, List<Entry<T>> entries) {
  public record Entry<T>(String key, T value) {}

  public CollectionView {
    entries = List.copyOf(entries);
  }

  public static <T> CollectionView<T> of(
      List<T> values, int page, int capacity, Function<T, String> key) {
    var keys = new HashSet<String>();
    for (T value : values) {
      String id = key.apply(value);
      if (id == null || id.isBlank() || !keys.add(id))
        throw new IllegalArgumentException("Collection keys must be unique");
    }
    var p = Page.of(values, page, capacity);
    return new CollectionView<>(
        p, p.items().stream().map(v -> new Entry<>(key.apply(v), v)).toList());
  }
}
