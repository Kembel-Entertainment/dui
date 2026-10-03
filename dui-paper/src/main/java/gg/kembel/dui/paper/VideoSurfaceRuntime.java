package gg.kembel.dui.paper;

import gg.kembel.dui.core.video.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Shared life cycle and logical controls; consumers own all button mappings and state. */
final class VideoSurfaceRuntime implements Listener, AutoCloseable {
  final Dui owner; final JavaPlugin plugin; final PackMetadata metadata;
  final ScheduledExecutorService workers = Executors.newScheduledThreadPool(2, r -> { var t = new Thread(r, "dui-video"); t.setDaemon(true); return t; });
  private static final AtomicInteger MAP_IDS = new AtomicInteger(-1_000_000_000);
  private final Map<UUID, VideoSurfaceSession> sessions = new HashMap<>();
  private final Map<UUID, int[]> reservations = new HashMap<>();
  private BukkitTask ticker;
  VideoSurfaceRuntime(Dui owner, JavaPlugin plugin, PackMetadata metadata) {
    this.owner = owner; this.plugin = plugin; this.metadata = metadata;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }
  VideoSurfaceSession open(Player player, VideoSurfaceSpec spec, VideoSurfaceOptions options, Consumer<SurfaceInput> input) {
    closeViewer(player, true); var s = new VideoSurfaceSession(this, player, spec, options, input);
    sessions.put(player.getUniqueId(), s);
    if (ticker == null) ticker = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
      for (var current : List.copyOf(sessions.values())) current.tick();
    }, 1, 1);
    if (owner.packLoaded(player)) s.begin(); else owner.offerPack(player);
    return s;
  }
  int[] reserve(Player p, int size) {
    int[] old = reservations.get(p.getUniqueId());
    if (old == null || old.length < size) {
      int[] next = old == null ? new int[size] : Arrays.copyOf(old, size);
      for (int i = old == null ? 0 : old.length; i < size; i++) next[i] = MAP_IDS.getAndIncrement();
      reservations.put(p.getUniqueId(), next); old = next;
    }
    return Arrays.copyOf(old, size);
  }
  void loaded(Player p) { var s = sessions.get(p.getUniqueId()); if (s != null) s.begin(); }
  void remove(VideoSurfaceSession s) { sessions.remove(s.player.getUniqueId(), s); if (sessions.isEmpty() && ticker != null) { ticker.cancel(); ticker = null; } }
  void closeViewer(Player p, boolean restore) { var s = sessions.get(p.getUniqueId()); if (s != null) s.finish(restore); }
  void quit(Player p) { closeViewer(p, false); reservations.remove(p.getUniqueId()); }
  void failure(VideoSurfaceSession s, Throwable error) {
    plugin.getLogger().log(java.util.logging.Level.SEVERE, "dui video surface failed", error);
    s.finish(true); if (s.player.isOnline()) s.player.sendMessage("The streaming screen could not start or update.");
  }
  private VideoSurfaceSession active(Player p) { var s = sessions.get(p.getUniqueId()); return s != null && s.isStarted() ? s : null; }
  @EventHandler public void input(PlayerInputEvent e) {
    var s = active(e.getPlayer()); if (s == null) return;
    var i = e.getInput();
    if (i.isSneak() && s.options.closeOnSneak()) { s.finish(true); return; }
    s.emit(new SurfaceInput(SurfaceInput.Type.STATE, i.isForward(), i.isBackward(), i.isLeft(), i.isRight(), i.isJump(), i.isSneak(), i.isSprint(), -1, System.nanoTime()));
  }
  @EventHandler(priority = EventPriority.HIGHEST) public void slot(PlayerItemHeldEvent e) {
    var s = active(e.getPlayer()); if (s == null) return; e.setCancelled(true);
    s.emit(new SurfaceInput(SurfaceInput.Type.SLOT, false, false, false, false, false, false, false, e.getNewSlot(), System.nanoTime()));
  }
  @EventHandler public void dismount(EntityDismountEvent e) { if (e.getEntity() instanceof Player p) { var s = active(p); if (s != null && s.mountedOn(e.getDismounted())) s.finish(true); } }
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void teleport(PlayerTeleportEvent e) { var s = active(e.getPlayer()); if (s != null && !s.ownTeleport) s.finish(false); }
  @EventHandler public void death(PlayerDeathEvent e) { closeViewer(e.getEntity(), false); }
  @EventHandler public void drop(PlayerDropItemEvent e) { if (active(e.getPlayer()) != null) e.setCancelled(true); }
  @EventHandler public void swap(PlayerSwapHandItemsEvent e) { if (active(e.getPlayer()) != null) e.setCancelled(true); }
  @EventHandler public void interact(PlayerInteractEvent e) { if (active(e.getPlayer()) != null) e.setCancelled(true); }
  @EventHandler public void click(InventoryClickEvent e) { if (e.getWhoClicked() instanceof Player p && active(p) != null) e.setCancelled(true); }
  @EventHandler public void drag(InventoryDragEvent e) { if (e.getWhoClicked() instanceof Player p && active(p) != null) e.setCancelled(true); }
  @Override public void close() { for (var s : List.copyOf(sessions.values())) s.finish(true); workers.shutdownNow(); reservations.clear(); HandlerList.unregisterAll(this); }
}
