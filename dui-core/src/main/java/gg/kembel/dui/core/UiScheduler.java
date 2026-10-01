package gg.kembel.dui.core;

/** Platform scheduler. Both callbacks run on the UI owner thread, never inline from later(). */
public interface UiScheduler {
  @FunctionalInterface
  interface Cancellation {
    void cancel();
  }

  Cancellation later(long ticks, Runnable task);

  void execute(Runnable task);
}
