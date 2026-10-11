package gg.kembel.dui.runtime;

import gg.kembel.dui.core.video.*;
import gg.kembel.dui.core.world.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

/** Shared world-map/video ownership, pack gating, map reservations and per-viewer ticking. */
public final class SpatialRuntime implements AutoCloseable {
  final Dui owner;
  final ScheduledExecutorService workers = Executors.newScheduledThreadPool(2, r -> {
    var thread = new Thread(r, "dui-video"); thread.setDaemon(true); return thread;
  });
  private static final AtomicInteger MAP_IDS = new AtomicInteger(-1_000_000_000);
  private final Function<UiViewer, SpatialView> factory;
  private final Map<UUID, WorldMapSession> maps = new ConcurrentHashMap<>();
  private final Map<UUID, VideoSurfaceSession> videos = new ConcurrentHashMap<>();
  private final Map<UUID, int[]> reservations = new ConcurrentHashMap<>();
  SpatialRuntime(Dui owner, Function<UiViewer, SpatialView> factory) { this.owner = owner; this.factory = factory; }

  public WorldMapSession openWorldMap(UiViewer viewer, String mapId, WorldMapFrame frame, WorldMapOptions options, Consumer<WorldMapInput> handler) {
    owner.check(viewer); Objects.requireNonNull(options); Objects.requireNonNull(handler);
    var metadata = owner.metadata().worldMaps().get(mapId);
    if (metadata == null) throw new IllegalArgumentException("Pack has no world map: " + mapId);
    frame.validate(metadata.definition());
    var view = factory.apply(viewer);
    owner.closeViewer(viewer, true); closeViewer(viewer, true);
    var session = new WorldMapSession(this, viewer, view, metadata, frame, options, handler);
    maps.put(viewer.id(), session); session.scheduleTick();
    try { if (owner.packLoaded(viewer)) session.begin(); else owner.offerPack(viewer); }
    catch (RuntimeException error) { session.finish(false); throw error; }
    return session;
  }
  public VideoSurfaceSession openVideoSurface(UiViewer viewer, VideoSurfaceSpec spec, VideoSurfaceOptions options, Consumer<SurfaceInput> handler) {
    owner.check(viewer); Objects.requireNonNull(spec); Objects.requireNonNull(options); Objects.requireNonNull(handler);
    if (!owner.metadata().supports(MapVideoCodec.CAPABILITY)) throw new IllegalArgumentException("Pack lacks map-video-v1; rebuild it with matching dui");
    var view = factory.apply(viewer);
    owner.closeViewer(viewer, true); closeViewer(viewer, true);
    var session = new VideoSurfaceSession(this, viewer, view, spec, options, handler);
    videos.put(viewer.id(), session); session.scheduleTick();
    try { if (owner.packLoaded(viewer)) session.begin(); else owner.offerPack(viewer); }
    catch (RuntimeException error) { session.finish(false); throw error; }
    return session;
  }
  int[] reserve(UiViewer viewer, int size) {
    return Arrays.copyOf(reservations.compute(viewer.id(), (id, old) -> {
      if (old != null && old.length >= size) return old;
      int[] next = old == null ? new int[size] : Arrays.copyOf(old, size);
      for (int i = old == null ? 0 : old.length; i < size; i++) next[i] = MAP_IDS.getAndIncrement();
      return next;
    }), size);
  }
  void remove(WorldMapSession session) { maps.remove(session.viewer.id(), session); }
  void remove(VideoSurfaceSession session) { videos.remove(session.viewer.id(), session); }
  void loaded(UiViewer viewer) {
    var map = maps.get(viewer.id()); if (map != null) map.begin();
    var video = videos.get(viewer.id()); if (video != null) video.begin();
  }
  public boolean active(UiViewer viewer) {
    var map = maps.get(viewer.id()); var video = videos.get(viewer.id());
    return map != null && map.isStarted() || video != null && video.isStarted();
  }
  public void click(UiViewer viewer, UUID entity, boolean secondary, long tick) {
    owner.check(viewer); var map = maps.get(viewer.id()); if (map != null) map.click(entity, secondary, tick);
    var video = videos.get(viewer.id()); if (video != null) video.clickPointer(entity, secondary, tick);
  }
  public boolean slot(UiViewer viewer, int previous, int next) {
    owner.check(viewer); var map = maps.get(viewer.id());
    if (map != null) { map.scroll(WorldMapGeometry.slotDelta(previous, next)); return false; }
    var video = videos.get(viewer.id());
    if (video == null || !video.isStarted()) return false;
    if (video.capturesWheel()) video.scrollPointer(WorldMapGeometry.slotDelta(previous, next));
    else video.emit(new SurfaceInput(SurfaceInput.Type.SLOT, false, false, false, false, false, false, false, next, System.nanoTime()));
    return true;
  }
  public void input(UiViewer viewer, SurfaceInput input) {
    owner.check(viewer); var map = maps.get(viewer.id()); var video = videos.get(viewer.id());
    if (map != null && input.sneak()) map.finish(true);
    if (video != null) {
      if (input.sneak() && video.options.closeOnSneak()) video.finish(true); else video.emit(input);
    }
  }
  public void closeViewer(UiViewer viewer, boolean restore) {
    viewer.checkThread();
    var map = maps.get(viewer.id()); if (map != null) map.finish(restore);
    var video = videos.get(viewer.id()); if (video != null) video.finish(restore);
  }
  void quit(UiViewer viewer) { closeViewer(viewer, false); reservations.remove(viewer.id()); }
  void failure(UiViewer viewer, Throwable error, Runnable cleanup) {
    System.getLogger(SpatialRuntime.class.getName()).log(System.Logger.Level.ERROR, "dui spatial session failed", error);
    cleanup.run(); if (viewer.online()) viewer.message(net.kyori.adventure.text.Component.text("The display could not initialize or update."));
  }
  void dispose(Runnable action) {
    try { action.run(); } catch (RuntimeException error) { System.getLogger(SpatialRuntime.class.getName()).log(System.Logger.Level.WARNING, "dui cleanup callback failed", error); }
  }
  @Override public void close() { workers.shutdownNow(); reservations.clear(); }
}
