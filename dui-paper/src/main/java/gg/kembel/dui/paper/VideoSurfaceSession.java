package gg.kembel.dui.paper;

import gg.kembel.dui.core.video.*;
import gg.kembel.dui.core.world.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;

/** One viewer's bounded streaming surface. Only submit()/statistics() may run off the main thread. */
public final class VideoSurfaceSession implements AutoCloseable {
  public record Statistics(long submitted, long sent, long dropped, long bytes, long encodeNanos, long sequence) {}
  final VideoSurfaceRuntime runtime;
  final Player player;
  final VideoSurfaceOptions options;
  private final VideoSurfaceSpec spec;
  private final Consumer<SurfaceInput> input;
  private final LatestFrame mailbox = new LatestFrame();
  private final AtomicBoolean scheduled = new AtomicBoolean();
  private final AtomicLong submitted = new AtomicLong(), sent = new AtomicLong(), bytes = new AtomicLong(), encodeNanos = new AtomicLong();
  private final AtomicLong sequence = new AtomicLong(-1);
  private volatile boolean active = true, started;
  private volatile MapVideoBridge bridge;
  private volatile byte[][] lastSent;
  private final VideoPacer pacer;
  private Location original;
  private int originalSlot;
  private ItemDisplay seat;
  private WorldMapClientAppearance appearance;
  private Runnable closed = () -> {};
  private final BossBar hud = BossBar.bossBar(Component.empty(), 0, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
  private final WorldMapHud hudRenderer;
  boolean ownTeleport;
  VideoSurfaceSession(VideoSurfaceRuntime runtime, Player player, VideoSurfaceSpec spec,
      VideoSurfaceOptions options, Consumer<SurfaceInput> input) {
    this.runtime = runtime; this.player = player; this.spec = spec; this.options = options; this.input = input;
    pacer = new VideoPacer(spec.maximumFps(), spec.budget().bytesPerSecond());
    hudRenderer = new WorldMapHud(runtime.metadata.worldMapLegendMetrics(), runtime.metadata.glyphs());
  }
  public boolean isActive() { return active; }
  public boolean isStarted() { return started; }
  public VideoSurfaceSpec specification() { return spec; }
  public VideoSurfaceSession onClose(Runnable callback) { runtime.owner.mainThread(); closed = Objects.requireNonNull(callback); if (!active) closed.run(); return this; }
  public void hud(WorldHud presentation) { runtime.owner.mainThread(); if (active) hud.name(hudRenderer.render(presentation)); }
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
    if (active) runtime.owner.scheduler().execute(() -> runtime.failure(this, error));
  }
  void begin() {
    if (!active || started) return;
    if (player.isInsideVehicle() || player.isDead() || player.isFlying() || player.isGliding() || player.isSleeping() || player.isSwimming()) {
      player.sendMessage("Stand on the ground outside a vehicle to open the screen."); finish(false); return;
    }
    original = player.getLocation().clone(); originalSlot = player.getInventory().getHeldItemSlot();
    try {
      seat = player.getWorld().spawn(original.clone().add(0, .6, 0), ItemDisplay.class, e -> {
        e.setVisibleByDefault(false); e.setPersistent(false); e.setGravity(false); e.setInvulnerable(true); e.setSilent(true);
      });
      player.showEntity(runtime.plugin, seat);
      if (!seat.addPassenger(player)) throw new IllegalStateException("Cannot mount video seat");
      appearance = new WorldMapClientAppearance(runtime.plugin, player, () -> finish(true));
      player.getInventory().setHeldItemSlot(options.anchorSlot());
      int tileCount = MapVideoCodec.encode(spec, new VideoFrame(spec.width(), spec.height(), spec.format(), 0, new int[spec.width() * spec.height()])).size();
      bridge = new MapVideoBridge(player, runtime.reserve(player, tileCount), this::fail);
      started = true; player.showBossBar(hud); schedule(0);
    } catch (Exception error) { runtime.failure(this, error); }
  }
  void emit(SurfaceInput event) { if (active && started) try { input.accept(event); } catch (RuntimeException e) { runtime.failure(this, e); } }
  boolean mountedOn(Entity e) { return e == seat; }
  void tick() { if (started && (!player.isOnline() || player.isDead() || seat == null || !seat.isValid() || player.getVehicle() != seat)) finish(!player.isDead()); }
  void finish(boolean restore) {
    if (!active) return;
    active = false; mailbox.close(); runtime.remove(this);
    if (bridge != null) bridge.close();
    try { input.accept(SurfaceInput.released()); } catch (RuntimeException ignored) {}
    player.hideBossBar(hud);
    if (appearance != null) appearance.close();
    if (seat != null) { seat.remove(); seat = null; }
    if (original != null && player.isOnline() && !player.isDead()) {
      player.getInventory().setHeldItemSlot(originalSlot);
      if (restore) { ownTeleport = true; try { player.teleport(original); } finally { ownTeleport = false; } }
    }
    closed.run();
  }
  @Override public void close() { runtime.owner.mainThread(); finish(true); }
}
