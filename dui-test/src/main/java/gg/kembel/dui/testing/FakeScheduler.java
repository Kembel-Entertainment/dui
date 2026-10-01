package gg.kembel.dui.testing;

import gg.kembel.dui.core.*;
import java.util.*;

/** Portable owner-thread fake clock. Async completion dispatch is explicit and deterministic. */
public final class FakeScheduler implements UiScheduler {
  private static final class Job {
    final long at, order;
    final Runnable run;
    boolean cancelled;

    Job(long at, long order, Runnable run) {
      this.at = at;
      this.order = order;
      this.run = run;
    }
  }

  private final PriorityQueue<Job> jobs =
      new PriorityQueue<>(
          Comparator.comparingLong((Job j) -> j.at).thenComparingLong(j -> j.order));
  private final Queue<Runnable> dispatch = new java.util.concurrent.ConcurrentLinkedQueue<>();
  private long tick, sequence;

  public long tick() {
    return tick;
  }

  public Cancellation later(long ticks, Runnable task) {
    if (ticks < 0) throw new IllegalArgumentException("Negative delay");
    Job job = new Job(Math.addExact(tick, ticks), sequence++, task);
    jobs.add(job);
    return () -> job.cancelled = true;
  }

  public void execute(Runnable task) {
    dispatch.add(task);
  }

  public void drain() {
    Runnable task;
    while ((task = dispatch.poll()) != null) task.run();
  }

  public void advance(long ticks) {
    if (ticks < 0) throw new IllegalArgumentException("Negative time");
    long end = Math.addExact(tick, ticks);
    drain();
    int budget = 10000;
    while (!jobs.isEmpty() && jobs.peek().at <= end) {
      if (--budget < 0) throw new IllegalStateException("Unbounded fake scheduler loop");
      Job job = jobs.remove();
      tick = job.at;
      if (!job.cancelled) job.run.run();
      drain();
    }
    tick = end;
    drain();
  }

  public int pending() {
    return (int) jobs.stream().filter(j -> !j.cancelled).count() + dispatch.size();
  }
}
