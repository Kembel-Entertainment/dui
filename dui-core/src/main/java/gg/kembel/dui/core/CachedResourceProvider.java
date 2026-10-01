package gg.kembel.dui.core;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Shared loading snapshots with bounded key/cost retention. Resolve from controllers, not render.
 */
public final class CachedResourceProvider<K, T> implements ResourceProvider<K, T> {
  private record Entry<T>(ResourceHandle<T> handle, long cost) {}

  private final long maxWeight;
  private final BoundedCache<K, Entry<T>> cache;
  private final Function<K, CompletableFuture<T>> load;
  private final Function<K, T> fallback;
  private final ToLongFunction<K> cost;

  public CachedResourceProvider(
      int entries,
      long weight,
      ToLongFunction<K> cost,
      Function<K, CompletableFuture<T>> load,
      Function<K, T> fallback) {
    this.maxWeight = weight;
    this.cache = new BoundedCache<>(entries, weight, Entry::cost);
    this.cost = Objects.requireNonNull(cost);
    this.load = Objects.requireNonNull(load);
    this.fallback = Objects.requireNonNull(fallback);
  }

  public ResourceHandle<T> resolve(K key) {
    Objects.requireNonNull(key);
    return cache
        .computeIfAbsent(
            key,
            k -> {
              long weight = cost.applyAsLong(k);
              if (weight < 0 || weight > maxWeight)
                throw new IllegalArgumentException("Resource cost");
              var future = Objects.requireNonNull(load.apply(k));
              return new Entry<>(new ResourceHandle<>(future, fallback.apply(k)), weight);
            })
        .handle();
  }

  public int retained() {
    return cache.size();
  }

  public long retainedWeight() {
    return cache.weight();
  }
}
