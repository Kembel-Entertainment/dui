package gg.kembel.dui.core;

import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.function.*;

/** UI-only jobs. Owner-thread operations; async completions are marshalled through UiScheduler. */
public final class TaskScope implements AutoCloseable {
  private static final class Job {
    UiScheduler.Cancellation cancellation = () -> {};
  }

  private final Thread ownerThread = Thread.currentThread();

  private void ownerThread() {
    if (Thread.currentThread() != ownerThread)
      throw new IllegalStateException("UI scope operation must run on its owner thread");
  }

  private final UiScheduler scheduler;
  private final BooleanSupplier valid;
  private final Map<String, Job> jobs = new HashMap<>();
  private boolean closed;

  public TaskScope(UiScheduler scheduler) {
    this(scheduler, () -> true);
  }

  public TaskScope(UiScheduler scheduler, BooleanSupplier valid) {
    this.scheduler = Objects.requireNonNull(scheduler);
    this.valid = Objects.requireNonNull(valid);
  }

  private Job replace(String key) {
    ownerThread();
    if (closed) throw new IllegalStateException("UI task scope is closed");
    if (key == null || key.isBlank()) throw new IllegalArgumentException("UI job needs a key");
    cancel(key);
    var job = new Job();
    jobs.put(key, job);
    return job;
  }

  /** Replaces the previous job with this key. This API must not own durable settlement work. */
  public void later(String key, long ticks, Runnable task) {
    if (ticks < 0) throw new IllegalArgumentException("Delay must be nonnegative");
    Objects.requireNonNull(task);
    var job = replace(key);
    try {
      job.cancellation =
          scheduler.later(
              Math.max(1, ticks),
              () -> {
                if (take(key, job)) task.run();
              });
    } catch (RuntimeException e) {
      jobs.remove(key, job);
      throw e;
    }
  }

  /**
   * Sample UI frames with automatic final-frame delivery and owner-thread cancellation. Duration
   * zero means an explicitly unbounded loop. No durable business work belongs here.
   */
  public UiScheduler.Cancellation sample(
      String key, long duration, int interval, LongConsumer frame) {
    if (duration < 0 || interval < 1 || interval > 20)
      throw new IllegalArgumentException("Animation sampling range");
    Objects.requireNonNull(frame);
    var job = replace(key);
    class Player {
      long age;

      void tick() {
        ownerThread();
        if (closed || jobs.get(key) != job || !valid.getAsBoolean()) {
          jobs.remove(key, job);
          return;
        }
        try {
          frame.accept(age);
        } catch (RuntimeException error) {
          jobs.remove(key, job);
          throw error;
        }
        if (closed || jobs.get(key) != job) return;
        if (duration != 0 && age == duration) {
          jobs.remove(key, job);
          return;
        }
        long wait = duration == 0 ? interval : Math.min(interval, duration - age);
        age = Math.addExact(age, wait);
        job.cancellation = scheduler.later(wait, this::tick);
      }
    }
    var player = new Player();
    job.cancellation = scheduler.later(1, player::tick);
    return () -> {
      ownerThread();
      if (jobs.get(key) == job) cancel(key);
    };
  }

  /** Latest result wins. Never cancels the provider's possibly shared CompletionStage. */
  public <T> void latest(String key, CompletionStage<T> result, BiConsumer<T, Throwable> handler) {
    Objects.requireNonNull(result);
    Objects.requireNonNull(handler);
    var job = replace(key);
    result.whenComplete(
        (value, error) ->
            scheduler.execute(
                () -> {
                  if (take(key, job)) handler.accept(value, error);
                }));
  }

  private boolean take(String key, Job job) {
    ownerThread();
    if (closed || jobs.get(key) != job) return false;
    jobs.remove(key);
    return valid.getAsBoolean();
  }

  public void cancel(String key) {
    ownerThread();
    var job = jobs.remove(key);
    if (job != null) job.cancellation.cancel();
  }

  public void cancelAll() {
    ownerThread();
    var previous = List.copyOf(jobs.values());
    jobs.clear();
    previous.forEach(job -> job.cancellation.cancel());
  }

  @Override
  public void close() {
    ownerThread();
    if (closed) return;
    closed = true;
    cancelAll();
  }
}
