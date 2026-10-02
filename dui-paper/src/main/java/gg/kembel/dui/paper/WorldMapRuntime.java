package gg.kembel.dui.paper;

import gg.kembel.dui.core.world.*;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import java.util.*;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Optional, embedded transport. No commands, pack hosting or application state. */
final class WorldMapRuntime implements Listener, AutoCloseable {
  final Dui owner;
  final JavaPlugin plugin;
  final PackMetadata metadata;
  private final Map<UUID, WorldMapSession> sessions = new HashMap<>();
  private BukkitTask ticker;
  private long tick;

  WorldMapRuntime(Dui owner, JavaPlugin plugin, PackMetadata metadata) {
    this.owner = owner;
    this.plugin = plugin;
    this.metadata = metadata;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  WorldMapSession open(
      Player player,
      WorldMapMetadata map,
      WorldMapFrame frame,
      WorldMapOptions options,
      Consumer<WorldMapInput> handler) {
    closeViewer(player, true);
    var session = new WorldMapSession(this, player, map, frame, options, handler);
    sessions.put(player.getUniqueId(), session);
    if (ticker == null)
      ticker =
          plugin
              .getServer()
              .getScheduler()
              .runTaskTimer(
                  plugin,
                  () -> {
                    tick++;
                    for (var s : List.copyOf(sessions.values()))
                      try {
                        s.tick();
                      } catch (RuntimeException e) {
                        failure(s, e);
                      }
                  },
                  1,
                  1);
    if (owner.packLoaded(player)) session.begin();
    else owner.offerPack(player);
    return session;
  }

  void loaded(Player player) {
    var session = sessions.get(player.getUniqueId());
    if (session != null) session.begin();
  }

  void remove(WorldMapSession session) {
    sessions.remove(session.player.getUniqueId(), session);
    if (sessions.isEmpty() && ticker != null) {
      ticker.cancel();
      ticker = null;
    }
  }

  void closeViewer(Player player, boolean restore) {
    var s = sessions.get(player.getUniqueId());
    if (s != null) s.finish(restore);
  }

  void failure(WorldMapSession session, Exception error) {
    plugin.getLogger().log(java.util.logging.Level.SEVERE, "dui world-map session failed", error);
    session.finish(true);
    session.player.sendMessage("The map could not initialize or update its display.");
  }

  private boolean active(Player player) {
    var s = sessions.get(player.getUniqueId());
    return s != null && s.isStarted();
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void attack(PrePlayerAttackEntityEvent event) {
    var s = sessions.get(event.getPlayer().getUniqueId());
    if (s == null || !s.isStarted()) return;
    event.setCancelled(true);
    s.click(event.getAttacked(), false, tick);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void interact(PlayerInteractEntityEvent event) {
    var s = sessions.get(event.getPlayer().getUniqueId());
    if (s == null || !s.isStarted()) return;
    event.setCancelled(true);
    if (event.getHand() == EquipmentSlot.HAND) s.click(event.getRightClicked(), true, tick);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void interactAt(PlayerInteractAtEntityEvent event) {
    interact(event);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void use(PlayerInteractEvent event) {
    if (active(event.getPlayer())) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void slot(PlayerItemHeldEvent event) {
    var s = sessions.get(event.getPlayer().getUniqueId());
    if (s != null)
      s.scroll(WorldMapGeometry.slotDelta(event.getPreviousSlot(), event.getNewSlot()));
  }

  @EventHandler
  public void dismount(EntityDismountEvent event) {
    if (event.getEntity() instanceof Player player) {
      var s = sessions.get(player.getUniqueId());
      if (s != null && s.mountedOn(event.getDismounted())) s.finish(true);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void teleport(PlayerTeleportEvent event) {
    var s = sessions.get(event.getPlayer().getUniqueId());
    if (s != null && !s.ownTeleport) s.finish(false);
  }

  @EventHandler
  public void death(PlayerDeathEvent event) {
    closeViewer(event.getEntity(), false);
  }

  @EventHandler
  public void drop(PlayerDropItemEvent event) {
    if (active(event.getPlayer())) event.setCancelled(true);
  }

  @EventHandler
  public void swap(PlayerSwapHandItemsEvent event) {
    if (active(event.getPlayer())) event.setCancelled(true);
  }

  @EventHandler
  public void click(InventoryClickEvent event) {
    if (event.getWhoClicked() instanceof Player p && active(p)) event.setCancelled(true);
  }

  @EventHandler
  public void drag(InventoryDragEvent event) {
    if (event.getWhoClicked() instanceof Player p && active(p)) event.setCancelled(true);
  }

  @Override
  public void close() {
    for (var s : List.copyOf(sessions.values())) s.finish(true);
    if (ticker != null) ticker.cancel();
    HandlerList.unregisterAll(this);
  }
}
