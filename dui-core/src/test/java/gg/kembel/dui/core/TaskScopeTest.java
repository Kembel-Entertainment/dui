package gg.kembel.dui.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;

class TaskScopeTest {
  static final class Scheduler implements UiScheduler {
    final List<Runnable> queue = new ArrayList<>();
    final List<Long> delays = new ArrayList<>();
    int cancellations;

    public Cancellation later(long ticks, Runnable task) {
      queue.add(task);
      delays.add(ticks);
      // Deliberately leave cancelled callbacks queued: real dispatch may already have begun.
      return () -> cancellations++;
    }

    public void execute(Runnable task) {
      queue.add(task);
    }

    void drain() {
      var copy = List.copyOf(queue);
      queue.clear();
      copy.forEach(Runnable::run);
    }
  }

  @Test
  void replacedCancelledAndClosedJobsNeverInvokeHandlers() {
    var scheduler = new Scheduler();
    var scope = new TaskScope(scheduler);
    var seen = new ArrayList<String>();
    scope.later("animation", 0, () -> seen.add("old"));
    scope.later("animation", 24, () -> seen.add("new"));
    scheduler.drain();
    assertEquals(List.of("new"), seen);
    assertEquals(List.of(1L, 24L), scheduler.delays);
    scope.later("cancelled", 1, () -> fail("cancelled callback"));
    scope.cancel("cancelled");
    scheduler.drain();
    scope.later("closed", 1, () -> fail("closed callback"));
    scope.close();
    scope.close();
    scheduler.drain();
    assertEquals(3, scheduler.cancellations);
    assertThrows(IllegalStateException.class, () -> scope.later("later", 1, () -> {}));
  }

  @Test
  void asyncResultsAreOwnerThreadDispatchedLatestWinsAndProvidersAreNotCancelled() {
    var scheduler = new Scheduler();
    var scope = new TaskScope(scheduler);
    var first = new CompletableFuture<String>();
    var second = new CompletableFuture<String>();
    var seen = new ArrayList<String>();
    scope.latest("images", first, (value, error) -> seen.add(value));
    scope.latest("images", second, (value, error) -> seen.add(value));
    first.complete("stale");
    second.complete("fresh");
    assertTrue(seen.isEmpty());
    scheduler.drain();
    assertEquals(List.of("fresh"), seen);
    var shared = new CompletableFuture<String>();
    scope.latest("shared", shared, (value, error) -> fail("closed async callback"));
    scope.close();
    assertFalse(shared.isCancelled());
    shared.complete("late");
    scheduler.drain();
  }

  @Test
  void validityErrorAndReentrantSchedulingAreHandled() {
    var scheduler = new Scheduler();
    var valid = new AtomicBoolean(false);
    var scope = new TaskScope(scheduler, valid::get);
    scope.later("hidden", 1, () -> fail("invalid callback"));
    scheduler.drain();
    valid.set(true);
    var error = new IllegalStateException("provider failed");
    var seen = new ArrayList<Throwable>();
    scope.latest(
        "error", CompletableFuture.failedFuture(error), (value, failure) -> seen.add(failure));
    scheduler.drain();
    assertEquals(List.of(error), seen);
    scope.later("again", 1, () -> scope.later("again", 1, () -> seen.add(null)));
    scheduler.drain();
    scheduler.drain();
    assertEquals(2, seen.size());
    assertThrows(IllegalArgumentException.class, () -> scope.later("bad", -1, () -> {}));
  }

  @Test
  void backgroundCompletionOnlyRunsTheHandlerWhenTheOwnerDrainsTheQueue() throws Exception {
    var scheduler = new Scheduler();
    var scope = new TaskScope(scheduler);
    var pending = new CompletableFuture<String>();
    var handlerThread = new AtomicReference<Thread>();
    scope.latest("provider", pending, (value, error) -> handlerThread.set(Thread.currentThread()));
    var worker = new Thread(() -> pending.complete("ready"));
    worker.start();
    worker.join();
    assertNull(handlerThread.get());
    scheduler.drain();
    assertSame(Thread.currentThread(), handlerThread.get());
  }
}
