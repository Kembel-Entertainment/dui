package gg.kembel.dui.core;

import java.util.*;
import java.util.function.*;

/**
 * Thread-safe weighted LRU. In-flight work can be cached, but rendering must only read snapshots.
 */
public final class BoundedCache<K, V> {
  private final int maxEntries;
  private final long maxWeight;
  private final ToLongFunction<V> weigh;
  private long weight;

  private record Entry<V>(V value, long cost) {}

  private final LinkedHashMap<K, Entry<V>> entries = new LinkedHashMap<>(16, .75f, true);

  public BoundedCache(int maxEntries, long maxWeight, ToLongFunction<V> weigh) {
    if (maxEntries < 1 || maxWeight < 1) throw new IllegalArgumentException("Cache bounds");
    this.maxEntries = maxEntries;
    this.maxWeight = maxWeight;
    this.weigh = Objects.requireNonNull(weigh);
  }

  public synchronized V get(K key) {
    var entry = entries.get(key);
    return entry == null ? null : entry.value();
  }

  public synchronized void put(K key, V value) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(value);
    long cost = weigh.applyAsLong(value);
    if (cost < 0 || cost > maxWeight)
      throw new IllegalArgumentException("Resource exceeds cache budget");
    var old = entries.remove(key);
    if (old != null) weight -= old.cost();
    while (entries.size() >= maxEntries || weight > maxWeight - cost) {
      var first = entries.entrySet().iterator();
      var entry = first.next();
      weight -= entry.getValue().cost();
      first.remove();
    }
    entries.put(key, new Entry<>(value, cost));
    weight += cost;
  }

  public synchronized V computeIfAbsent(K key, Function<K, V> loader) {
    V value = get(key);
    if (value == null) {
      value = Objects.requireNonNull(loader.apply(key));
      put(key, value);
    }
    return value;
  }

  public synchronized V compute(K key, BiFunction<K, V, V> update) {
    V value = Objects.requireNonNull(update.apply(key, get(key)));
    put(key, value);
    return value;
  }

  public synchronized int size() {
    return entries.size();
  }

  public synchronized long weight() {
    return weight;
  }

  public synchronized void retain(Set<K> keys) {
    var iterator = entries.entrySet().iterator();
    while (iterator.hasNext()) {
      var entry = iterator.next();
      if (!keys.contains(entry.getKey())) {
        weight -= entry.getValue().cost();
        iterator.remove();
      }
    }
  }
}
