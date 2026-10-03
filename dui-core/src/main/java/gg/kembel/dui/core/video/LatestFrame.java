package gg.kembel.dui.core.video;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;

/** One-slot mailbox. Closing it makes all subsequent submissions inert. */
public final class LatestFrame implements AutoCloseable {
  private static final Object CLOSED = new Object();
  private final AtomicReference<Object> pending = new AtomicReference<>();
  private final AtomicLong dropped = new AtomicLong();
  public boolean submit(VideoFrame frame) {
    java.util.Objects.requireNonNull(frame);
    for (;;) {
      Object old = pending.get(); if (old == CLOSED) return false;
      if (pending.compareAndSet(old, frame)) { if (old != null) dropped.incrementAndGet(); return true; }
    }
  }
  public VideoFrame poll() {
    for (;;) {
      Object old = pending.get(); if (old == CLOSED || old == null) return null;
      if (pending.compareAndSet(old, null)) return (VideoFrame) old;
    }
  }
  public boolean hasFrame() { Object value = pending.get(); return value != null && value != CLOSED; }
  public void restore(VideoFrame frame) { if (!pending.compareAndSet(null, frame)) dropped.incrementAndGet(); }
  public long dropped() { return dropped.get(); }
  @Override public void close() { pending.set(CLOSED); }
}
