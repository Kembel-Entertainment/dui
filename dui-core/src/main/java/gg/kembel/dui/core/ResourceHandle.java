package gg.kembel.dui.core;

import java.util.*;
import java.util.concurrent.*;

/**
 * Shared provider state; controllers subscribe with TaskScope.latest, never perform I/O in render.
 */
public final class ResourceHandle<T> {
  public enum Status {
    LOADING,
    READY,
    ERROR
  }

  public record Snapshot<T>(Status status, T value, T fallback, String error) {
    public T available() {
      return value != null ? value : fallback;
    }
  }

  private volatile Snapshot<T> snapshot;
  private final CompletableFuture<T> completion;

  public ResourceHandle(CompletableFuture<T> completion, T fallback) {
    this.completion = Objects.requireNonNull(completion);
    snapshot = new Snapshot<>(Status.LOADING, null, fallback, "");
    completion.whenComplete(
        (v, e) ->
            snapshot =
                e == null && v != null
                    ? new Snapshot<>(Status.READY, v, fallback, "")
                    : new Snapshot<>(
                        Status.ERROR,
                        null,
                        fallback,
                        e == null
                            ? "Empty resource"
                            : Objects.toString(e.getMessage(), "Resource failed")));
  }

  public static <T> ResourceHandle<T> ready(T value) {
    return new ResourceHandle<>(CompletableFuture.completedFuture(value), null);
  }

  public Snapshot<T> snapshot() {
    return snapshot;
  }

  public CompletableFuture<T> completion() {
    return completion;
  }
}
