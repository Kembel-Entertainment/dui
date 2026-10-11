package gg.kembel.dui.paper;

import gg.kembel.dui.core.video.SurfaceInput;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;

/** Converts Paper events to the shared spatial input/lifecycle API. */
final class PaperSpatialEvents implements Listener {
  private final Dui owner;
  PaperSpatialEvents(Dui owner) { this.owner = owner; }
  private gg.kembel.dui.runtime.SpatialRuntime spatial() { return owner.runtime().spatial(); }
  private boolean active(Player player) { return spatial().active(owner.viewer(player)); }
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false) public void attack(PrePlayerAttackEntityEvent e) {
    if (!active(e.getPlayer())) return; e.setCancelled(true);
    spatial().click(owner.viewer(e.getPlayer()), e.getAttacked().getUniqueId(), false, e.getPlayer().getWorld().getGameTime());
  }
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false) public void interact(PlayerInteractEntityEvent e) {
    if (!active(e.getPlayer())) return; e.setCancelled(true);
    if (e.getHand() == EquipmentSlot.HAND) spatial().click(owner.viewer(e.getPlayer()), e.getRightClicked().getUniqueId(), true, e.getPlayer().getWorld().getGameTime());
  }
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false) public void interactAt(PlayerInteractAtEntityEvent e) { interact(e); }
  @EventHandler(priority = EventPriority.HIGHEST) public void use(PlayerInteractEvent e) { if (active(e.getPlayer())) e.setCancelled(true); }
  @EventHandler(priority = EventPriority.HIGHEST) public void slot(PlayerItemHeldEvent e) {
    if (spatial().slot(owner.viewer(e.getPlayer()), e.getPreviousSlot(), e.getNewSlot())) e.setCancelled(true);
  }
  @EventHandler public void input(PlayerInputEvent e) {
    var i = e.getInput();
    spatial().input(owner.viewer(e.getPlayer()), new SurfaceInput(SurfaceInput.Type.STATE, i.isForward(), i.isBackward(), i.isLeft(), i.isRight(), i.isJump(), i.isSneak(), i.isSprint(), -1, System.nanoTime()));
  }
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void teleport(PlayerTeleportEvent e) {
    if (active(e.getPlayer())) spatial().closeViewer(owner.viewer(e.getPlayer()), false);
  }
  @EventHandler public void death(PlayerDeathEvent e) { spatial().closeViewer(owner.viewer(e.getEntity()), false); }
  @EventHandler public void drop(PlayerDropItemEvent e) { if (active(e.getPlayer())) e.setCancelled(true); }
  @EventHandler public void swap(PlayerSwapHandItemsEvent e) { if (active(e.getPlayer())) e.setCancelled(true); }
  @EventHandler public void click(InventoryClickEvent e) { if (e.getWhoClicked() instanceof Player p && active(p)) e.setCancelled(true); }
  @EventHandler public void drag(InventoryDragEvent e) { if (e.getWhoClicked() instanceof Player p && active(p)) e.setCancelled(true); }
}
