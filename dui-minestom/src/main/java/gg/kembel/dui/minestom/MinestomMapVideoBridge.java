package gg.kembel.dui.minestom;

import gg.kembel.dui.core.video.MapVideoCodec;
import gg.kembel.dui.runtime.MapVideoConnection;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.*;
import net.minestom.server.entity.*;
import net.minestom.server.entity.metadata.other.ItemFrameMeta;
import net.minestom.server.event.*;
import net.minestom.server.event.player.PlayerPacketOutEvent;
import net.minestom.server.item.*;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.*;

/** Native typed packets; one frame queued for serialization plus the socket's current buffer. */
final class MinestomMapVideoBridge implements MapVideoConnection {
  private record Pending(BundlePacket end, Runnable accepted) {}
  private final Player player;
  private final int[] maps, entities;
  private final EventNode<Event> events;
  private final AtomicReference<Pending> pending = new AtomicReference<>();
  private volatile boolean closed;
  MinestomMapVideoBridge(Player player, int[] maps, Consumer<Throwable> failure) {
    this.player = player; this.maps = maps.clone(); this.entities = new int[maps.length];
    events = EventNode.all("dui-video-" + UUID.randomUUID());
    events.addListener(PlayerPacketOutEvent.class, event -> {
      var frame = pending.get();
      if (event.getPlayer() == player && frame != null && event.getPacket() == frame.end() && pending.compareAndSet(frame, null)) {
        try { if (!closed) frame.accepted().run(); } catch (RuntimeException error) { failure.accept(error); }
      }
    });
    MinecraftServer.getGlobalEventHandler().addChild(events);
    var eye = player.getPosition().add(0, player.getEyeHeight(), 0);
    int direction = eye.z() - Math.floor(eye.z()) >= .5 ? 2 : 3;
    var packets = new ArrayList<ServerPacket>(); packets.add(new BundlePacket());
    for (int i = 0; i < maps.length; i++) {
      var frame = new Entity(EntityType.GLOW_ITEM_FRAME); entities[i] = frame.getEntityId();
      frame.setInvisible(true);
      ((ItemFrameMeta)frame.getEntityMeta()).setItem(ItemStack.of(Material.FILLED_MAP).with(DataComponents.MAP_ID, maps[i]));
      packets.add(new SpawnEntityPacket(frame.getEntityId(), frame.getUuid(), EntityType.GLOW_ITEM_FRAME,
          new Pos(Math.floor(eye.x()) + .5, Math.floor(eye.y()) + .5, Math.floor(eye.z()) + (direction == 2 ? .03125 : .96875)), 0, direction, Vec.ZERO));
      packets.add(frame.getMetadataPacket());
    }
    packets.add(new BundlePacket()); player.getPlayerConnection().sendPackets(packets);
  }
  public boolean ready() { return !closed && player.isOnline() && pending.get() == null; }
  public Object framePacket(int tile, MapVideoCodec.Patch p) {
    return new MapDataPacket(maps[tile], (byte)0, true, false, List.of(),
        new MapDataPacket.ColorContent((byte)p.width(), (byte)p.height(), (byte)p.x(), (byte)p.y(), p.colors()));
  }
  public boolean sendFrame(List<Object> source, Runnable accepted, Consumer<Throwable> failure) {
    if (!ready()) return false;
    var end = new BundlePacket(); var frame = new Pending(end, accepted);
    if (!pending.compareAndSet(null, frame)) return false;
    try {
      var packets = new ArrayList<ServerPacket>(source.size() + 2); packets.add(new BundlePacket());
      for (var packet : source) packets.add((ServerPacket)packet); packets.add(end);
      player.getPlayerConnection().sendPackets(packets); return true;
    } catch (RuntimeException error) { pending.compareAndSet(frame, null); failure.accept(error); return false; }
  }
  @Override public void close() {
    closed = true; pending.set(null); MinecraftServer.getGlobalEventHandler().removeChild(events);
    if (player.isOnline()) player.sendPacket(new DestroyEntitiesPacket(Arrays.stream(entities).boxed().toList()));
  }
}
