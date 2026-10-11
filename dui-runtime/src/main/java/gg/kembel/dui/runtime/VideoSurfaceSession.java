package gg.kembel.dui.runtime;

import gg.kembel.dui.core.video.*;
import gg.kembel.dui.core.world.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;


/** One viewer's bounded streaming surface. Only submit()/statistics() may run outside the viewer context. */
public final class VideoSurfaceSession implements AutoCloseable {
  public record Statistics(long submitted, long sent, long dropped, long bytes, long encodeNanos, long sequence) {}
  final SpatialRuntime runtime;
  final UiViewer viewer;
  private final SpatialView view;
  final VideoSurfaceOptions options;
  private final VideoSurfaceSpec spec;
  private final Consumer<SurfaceInput> input;
  private final LatestFrame mailbox = new LatestFrame();
  private final AtomicBoolean scheduled = new AtomicBoolean();
  private final AtomicLong submitted = new AtomicLong(), sent = new AtomicLong(), bytes = new AtomicLong(), encodeNanos = new AtomicLong();
  private final AtomicLong sequence = new AtomicLong(-1);
  private volatile boolean active = true, started;
  private volatile MapVideoConnection bridge;
  private volatile byte[][] lastSent;
  private final VideoPacer pacer;
  private UUID pointerTarget;
  private Consumer<SurfacePointerInput> pointer;
  private boolean pointerWheel;
  private float lastYaw, lastPitch;
  private long lastPrimaryTick = -1, lastSecondaryTick = -1;
  private Runnable closed = () -> {};
  private final WorldMapHud hudRenderer;
  private WorldHud presentation;
  private gg.kembel.dui.core.UiScheduler.Cancellation ticker = () -> {};
  VideoSurfaceSession(SpatialRuntime runtime, UiViewer viewer, SpatialView view, VideoSurfaceSpec spec,
      VideoSurfaceOptions options, Consumer<SurfaceInput> input) {
    this.runtime = runtime; this.viewer = viewer; this.view = view; this.spec = spec; this.options = options; this.input = input;
    pacer = new VideoPacer(spec.maximumFps(), spec.budget().bytesPerSecond());
    hudRenderer = new WorldMapHud(runtime.owner.metadata().worldMapLegendMetrics(), runtime.owner.metadata().glyphs());
  }
  public boolean isActive() { return active; }
  public boolean isStarted() { return started; }
  /** Opt into Vanilla look/click/scroll capture; applications own all cursor coordinates and bindings. */
  public VideoSurfaceSession pointerInput(Consumer<SurfacePointerInput> handler) {
    return pointerInput(handler, true);
  }
  /** When captureWheel is true, slot pulses become SCROLL instead of SurfaceInput.SLOT. */
  public VideoSurfaceSession pointerInput(Consumer<SurfacePointerInput> handler, boolean captureWheel) {
    runtime.owner.check(viewer); pointer = Objects.requireNonNull(handler); pointerWheel = captureWheel;
    lastYaw = view.yaw(); lastPitch = view.pitch();
    if (active && started && pointerTarget == null)
      try { createPointerTarget(); } catch (RuntimeException error) { runtime.failure(viewer, error, () -> finish(true)); }
    return this;
  }
  boolean capturesWheel() { return pointer != null && pointerWheel; }
  public VideoSurfaceSpec specification() { return spec; }
  public VideoSurfaceSession onClose(Runnable callback) { runtime.owner.check(viewer); closed = Objects.requireNonNull(callback); if (!active) closed.run(); return this; }
  public void hud(WorldHud presentation) {
    runtime.owner.check(viewer); this.presentation = Objects.requireNonNull(presentation);
    if (active && started) view.hud(hudRenderer.render(presentation));
  }
  public Statistics statistics() { return new Statistics(submitted.get(), sent.get(), mailbox.dropped(), bytes.get(), encodeNanos.get(), sequence.get()); }
  public boolean submit(VideoFrame frame) {
    spec.validate(frame);
    if (!active) return false;
    submitted.incrementAndGet(); boolean accepted = mailbox.submit(frame);
    if (accepted) schedule(0); return accepted;
  }
  private void schedule(long nanos) {
    if (active && started && scheduled.compareAndSet(false, true))
      try { runtime.workers.schedule(this::drain, Math.max(0, nanos), TimeUnit.NANOSECONDS); }
      catch (RejectedExecutionException ignored) { scheduled.set(false); }
  }
  private void drain() {
    VideoFrame frame = null;
    try {
      long now = System.nanoTime();
      if (!active || !started) return;
      if (now < pacer.nextSendNanos() || bridge == null || !bridge.ready()) return;
      frame = mailbox.poll(); if (frame == null) return;
      long begin = System.nanoTime();
      var tiles = MapVideoCodec.encode(spec, frame);
      byte[][] previous = lastSent, next = new byte[tiles.size()][];
      var packets = new ArrayList<Object>(); long count = 0;
      for (int i = 0; i < tiles.size(); i++) {
        next[i] = tiles.get(i).colors();
        var patch = MapVideoCodec.difference(previous == null ? null : previous[i], next[i]);
        if (patch != null) { count += (long) patch.width() * patch.height() + 32; packets.add(bridge.framePacket(i, patch)); }
      }
      encodeNanos.addAndGet(System.nanoTime() - begin);
      long acceptedSequence = frame.sequence(), acceptedBytes = count;
      if (packets.isEmpty()) { sequence.set(acceptedSequence); return; }
      if (bridge.sendFrame(packets, () -> { lastSent = next; sent.incrementAndGet(); bytes.addAndGet(acceptedBytes); sequence.set(acceptedSequence); }, this::fail)) {
        pacer.accepted(System.nanoTime(), count);
      } else mailbox.restore(frame);
    } catch (Throwable error) { fail(error); }
    finally {
      scheduled.set(false);
      if (active && mailbox.hasFrame()) schedule(Math.max(250_000L, pacer.nextSendNanos() - System.nanoTime()));
    }
  }
  private void fail(Throwable error) {
    if (active) viewer.scheduler().execute(() -> runtime.failure(viewer, error, () -> finish(true)));
  }
  void begin() {
    if (!active || started) return;
    try {
      if (!view.begin(false, options.anchorSlot(), () -> finish(true))) { finish(false); return; }
      int tileCount = MapVideoCodec.encode(spec, new VideoFrame(spec.width(), spec.height(), spec.format(), 0, new int[spec.width() * spec.height()])).size();
      bridge = view.video(runtime.reserve(viewer, tileCount), this::fail);
      started = true; if (presentation != null) view.hud(hudRenderer.render(presentation));
      if (pointer != null) createPointerTarget(); schedule(0);
    } catch (Exception error) { runtime.failure(viewer, error, () -> finish(true)); }
  }
  void emit(SurfaceInput event) { if (active && started) try { input.accept(event); } catch (RuntimeException e) { runtime.failure(viewer, e, () -> finish(true)); } }
  private void createPointerTarget() {
    pointerTarget = view.replaceInput(); lastYaw = view.yaw(); lastPitch = view.pitch();
  }
  private void look() {
    if (pointer == null || !active || !started) return;
    double yaw = WorldMapGeometry.relativeYaw(view.yaw(), lastYaw), pitch = view.pitch() - lastPitch;
    lastYaw = view.yaw(); lastPitch = view.pitch();
    if (yaw != 0 || pitch != 0) point(new SurfacePointerInput(SurfacePointerInput.Type.LOOK, yaw, pitch, 0, System.nanoTime()));
  }
  void clickPointer(UUID target, boolean secondary, long tick) {
    if (pointer == null || pointerTarget == null || !target.equals(pointerTarget) || !active || !started) return;
    if ((secondary ? lastSecondaryTick : lastPrimaryTick) == tick) return;
    if (secondary) lastSecondaryTick = tick; else lastPrimaryTick = tick;
    look();
    point(new SurfacePointerInput(secondary ? SurfacePointerInput.Type.SECONDARY : SurfacePointerInput.Type.PRIMARY, 0, 0, 0, System.nanoTime()));
  }
  void scrollPointer(int steps) {
    look(); if (steps != 0) point(new SurfacePointerInput(SurfacePointerInput.Type.SCROLL, 0, 0, steps, System.nanoTime()));
  }
  private void point(SurfacePointerInput event) {
    if (active && started && pointer != null) try { pointer.accept(event); }
    catch (RuntimeException e) { runtime.failure(viewer, e, () -> finish(true)); }
  }
  void scheduleTick() {
    if (!active) return;
    ticker = viewer.scheduler().later(1, () -> {
      try {
        if (started && (!viewer.online() || !view.valid())) finish(!view.dead()); else look();
      } catch (RuntimeException error) { runtime.failure(viewer, error, () -> finish(true)); }
      if (active) scheduleTick();
    });
  }
  void finish(boolean restore) {
    if (!active) return;
    active = false; mailbox.close(); runtime.remove(this);
    runtime.dispose(ticker::cancel);
    if (bridge != null) runtime.dispose(bridge::close);
    runtime.dispose(() -> input.accept(SurfaceInput.released()));
    runtime.dispose(() -> view.close(restore));
    runtime.dispose(closed);
  }
  @Override public void close() { runtime.owner.check(viewer); finish(true); }
}
