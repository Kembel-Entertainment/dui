package gg.kembel.dui.minestom;

import gg.kembel.dui.runtime.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.resource.*;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.*;
import net.minestom.server.event.player.*;
import net.minestom.server.item.*;

/** Embedded Minestom adapter. It owns an isolated event node, never commands or application state. */
public final class Dui implements AutoCloseable {
  private final gg.kembel.dui.runtime.Dui runtime;
  private final EventNode<Event> events;
  private final Map<UUID, MinestomViewer> viewers = new ConcurrentHashMap<>();
  private final AtomicBoolean stopped = new AtomicBoolean();

  private Dui(PackDescriptor pack, PackMetadata metadata) {
    runtime = new gg.kembel.dui.runtime.Dui(pack, metadata, new DialogTransport() {
      public NativeItem item(Key material) {
        var type = Material.fromKey(material);
        if (type == null) throw new IllegalArgumentException("Unknown item: " + material);
        return MinestomItem.of(ItemStack.of(type));
      }
      public void offerPack(UiViewer viewer, PackDescriptor p) {
        ((MinestomViewer)viewer).player().sendResourcePacks(ResourcePackRequest.resourcePackRequest()
            .packs(ResourcePackInfo.resourcePackInfo(p.id(), p.uri(), p.sha1())).required(p.required()).prompt(Component.text("dui / Vanilla UI")).build());
      }
      public void show(UiViewer viewer, DialogPresentation p) { MinestomDialogPackets.show(((MinestomViewer)viewer).player(), p); }
      public void close(UiViewer viewer) { ((MinestomViewer)viewer).player().closeDialog(); }
      public SpatialView spatial(UiViewer viewer) { return new MinestomSpatialView(((MinestomViewer)viewer).player()); }
    });
    events = EventNode.all("dui-" + UUID.randomUUID());
    events.addListener(PlayerResourcePackStatusEvent.class, event -> inContext(event.getPlayer(), viewer ->
        runtime.resourcePack(viewer, event.getPackUuid(), event.getStatus().name())));
    events.addListener(PlayerCustomClickEvent.class, event -> {
      if (!stopped.get()) viewer(event.getPlayer()).execute(() -> { if (!stopped.get()) runtime.click(viewer(event.getPlayer()), event.getKey(), MinestomResponse.of(event.getPayload())); });
    });
    events.addListener(PlayerDisconnectEvent.class, event -> inContext(event.getPlayer(), viewer -> {
      runtime.quit(viewer); viewers.remove(viewer.id(), viewer);
    }));
    registerSpatialEvents();
    MinecraftServer.getGlobalEventHandler().addChild(events);
  }
  public static Dui create(PackDescriptor pack, PackMetadata metadata) { return new Dui(pack, metadata); }
  public gg.kembel.dui.runtime.Dui runtime() { return runtime; }
  public MinestomViewer viewer(Player player) {
    if (stopped.get()) throw new IllegalStateException("dui is closed");
    return viewers.computeIfAbsent(player.getUuid(), id -> new MinestomViewer(player));
  }
  private void inContext(Player player, java.util.function.Consumer<MinestomViewer> action) {
    if (stopped.get()) return;
    player.acquirable().sync(p -> { if (!stopped.get()) action.accept(viewer(p)); });
  }
  public void offerPack(Player player) { runtime.offerPack(viewer(player)); }
  public boolean packLoaded(Player player) { return runtime.packLoaded(viewer(player)); }
  public DialogSession open(Player player, gg.kembel.dui.core.MenuTemplate template, ViewModel model, ActionHandler action) {
    return runtime.open(viewer(player), template, model, action);
  }
  public DialogSession open(Player player, gg.kembel.dui.core.Canvas canvas, ViewModel model, DialogOptions options, ActionHandler action) {
    return runtime.open(viewer(player), canvas, model, options, action);
  }
  public WorldMapSession openWorldMap(Player player, String id, gg.kembel.dui.core.world.WorldMapFrame frame,
      WorldMapOptions options, java.util.function.Consumer<gg.kembel.dui.core.world.WorldMapInput> handler) {
    return runtime.spatial().openWorldMap(viewer(player), id, frame, options, handler);
  }
  public VideoSurfaceSession openVideoSurface(Player player, gg.kembel.dui.core.video.VideoSurfaceSpec spec,
      VideoSurfaceOptions options, java.util.function.Consumer<gg.kembel.dui.core.video.SurfaceInput> handler) {
    return runtime.spatial().openVideoSurface(viewer(player), spec, options, handler);
  }
  private void registerSpatialEvents() {
    events.addListener(net.minestom.server.event.entity.EntityAttackEvent.class, e -> {
      if (e.getEntity() instanceof Player p) inContext(p, viewer -> runtime.spatial().click(viewer, e.getTarget().getUuid(), false, p.getInstance().getWorldAge()));
    });
    events.addListener(PlayerEntityInteractEvent.class, e -> {
      if (e.getHand() == net.minestom.server.entity.PlayerHand.MAIN) inContext(e.getPlayer(), viewer -> runtime.spatial().click(viewer, e.getTarget().getUuid(), true, e.getPlayer().getInstance().getWorldAge()));
    });
    events.addListener(PlayerChangeHeldSlotEvent.class, e -> inContext(e.getPlayer(), viewer -> {
      if (runtime.spatial().slot(viewer, e.getOldSlot(), e.getNewSlot())) e.setCancelled(true);
    }));
    events.addListener(PlayerInputEvent.class, e -> inContext(e.getPlayer(), viewer -> {
      var i = e.getPlayer().inputs();
      runtime.spatial().input(viewer, new gg.kembel.dui.core.video.SurfaceInput(gg.kembel.dui.core.video.SurfaceInput.Type.STATE,
          i.forward(), i.backward(), i.left(), i.right(), i.jump(), i.shift(), i.sprint(), -1, System.nanoTime()));
    }));
    events.addListener(net.minestom.server.event.entity.EntityTeleportEvent.class, e -> {
      if (e.getEntity() instanceof Player p && !MinestomSpatialView.mutating(p)) inContext(p, viewer -> runtime.spatial().closeViewer(viewer, false));
    });
    events.addListener(net.minestom.server.event.instance.RemoveEntityFromInstanceEvent.class, e -> {
      if (e.getEntity() instanceof Player p) inContext(p, viewer -> runtime.spatial().closeViewer(viewer, false));
    });
    events.addListener(PlayerDeathEvent.class, e -> inContext(e.getPlayer(), viewer -> runtime.spatial().closeViewer(viewer, false)));
    events.addListener(net.minestom.server.event.item.ItemDropEvent.class, e -> inContext(e.getPlayer(), viewer -> {
      if (runtime.spatial().active(viewer)) e.setCancelled(true);
    }));
    events.addListener(net.minestom.server.event.inventory.InventoryPreClickEvent.class, e -> inContext(e.getPlayer(), viewer -> {
      if (runtime.spatial().active(viewer)) e.setCancelled(true);
    }));
    events.addListener(PlayerUseItemEvent.class, e -> inContext(e.getPlayer(), viewer -> { if (runtime.spatial().active(viewer)) e.setCancelled(true); }));
    events.addListener(PlayerSwapItemEvent.class, e -> inContext(e.getPlayer(), viewer -> { if (runtime.spatial().active(viewer)) e.setCancelled(true); }));
    events.addListener(PlayerBlockInteractEvent.class, e -> inContext(e.getPlayer(), viewer -> { if (runtime.spatial().active(viewer)) e.setCancelled(true); }));
  }
  @Override public void close() {
    if (!stopped.compareAndSet(false, true)) return;
    MinecraftServer.getGlobalEventHandler().removeChild(events);
    for (var viewer : List.copyOf(viewers.values())) viewer.player().acquirable().sync(p -> {
      runtime.spatial().closeViewer(viewer, true); runtime.closeViewer(viewer, true); runtime.quit(viewer); viewers.remove(viewer.id(), viewer);
    });
    runtime.close();
  }
}
