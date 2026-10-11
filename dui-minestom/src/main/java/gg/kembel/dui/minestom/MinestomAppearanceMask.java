package gg.kembel.dui.minestom;

import java.util.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.*;
import net.minestom.server.event.*;
import net.minestom.server.event.player.PlayerPacketOutEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.packet.PacketWriting;
import net.minestom.server.network.packet.server.*;
import net.minestom.server.network.packet.server.play.*;

/** Connection-local presentation. Server inventory, equipment, potion effects and gamemode are untouched. */
final class MinestomAppearanceMask implements AutoCloseable {
  private final Player player;
  private final EventNode<Event> events;
  MinestomAppearanceMask(Player player) {
    this.player = player;
    events = EventNode.all("dui-appearance-" + UUID.randomUUID());
    events.addListener(PlayerPacketOutEvent.class, event -> {
      if (event.getPlayer() == player && hides(event.getPacket(), player.getEntityId())) event.setCancelled(true);
    });
    MinecraftServer.getGlobalEventHandler().addChild(events);
    var metadata = new HashMap<>(player.getMetadataPacket().entries());
    var flags = metadata.get(0);
    byte value = flags != null && flags.value() instanceof Byte b ? b : 0;
    metadata.put(0, Metadata.Byte((byte)(value | 0x20)));
    sendRaw(player, new EntityMetaDataPacket(player.getEntityId(), metadata));
    var inventory = player.getInventory().getItemStacks();
    var slots = new ArrayList<ItemStack>(Collections.nCopies(inventory.length, ItemStack.AIR));
    for (int i = 0; i < inventory.length; i++) slots.set(net.minestom.server.utils.inventory.PlayerInventoryUtils.convertMinestomSlotToWindowSlot(i), inventory[i]);
    for (int i = 36; i < Math.min(46, slots.size()); i++) slots.set(i, ItemStack.AIR);
    sendRaw(player, new WindowItemsPacket(0, 0, slots, player.getInventory().getCursorItem()));
    sendRaw(player, new EntityEquipmentPacket(player.getEntityId(), Map.of(EquipmentSlot.MAIN_HAND, ItemStack.AIR, EquipmentSlot.OFF_HAND, ItemStack.AIR)));
  }
  static boolean hides(ServerPacket packet, int playerId) {
    return switch (packet) {
      case EntityMetaDataPacket p -> p.entityId() == playerId;
      case EntityEquipmentPacket p -> p.entityId() == playerId;
      case WindowItemsPacket p -> p.windowId() == 0;
      case SetSlotPacket p -> p.windowId() == 0 && p.slot() >= 36 && p.slot() <= 45 || p.windowId() == -2 && handSlot(p.slot());
      case SetPlayerInventorySlotPacket p -> handSlot(p.slot());
      case ChangeGameStatePacket p -> Set.of(ChangeGameStatePacket.Reason.BEGIN_RAINING, ChangeGameStatePacket.Reason.RAIN_LEVEL_CHANGE, ChangeGameStatePacket.Reason.THUNDER_LEVEL_CHANGE).contains(p.reason());
      default -> false;
    };
  }
  private static boolean handSlot(int slot) { return slot >= 0 && slot < 9 || slot == 40; }
  /** Buffered packets bypass the outgoing event, including this presentation mask. */
  static void sendRaw(Player player, ServerPacket packet) {
    var buffer = PacketWriting.allocateTrimmedPacket(ConnectionState.PLAY, packet, MinecraftServer.getCompressionThreshold());
    player.sendPacket(new BufferedPacket(buffer, 0, buffer.capacity()));
  }
  @Override public void close() {
    MinecraftServer.getGlobalEventHandler().removeChild(events);
    if (player.isOnline()) {
      player.sendPacket(player.getMetadataPacket()); player.getInventory().update(player);
      player.sendPacket(new EntityEquipmentPacket(player.getEntityId(), Map.of(EquipmentSlot.MAIN_HAND, player.getItemInMainHand(), EquipmentSlot.OFF_HAND, player.getItemInOffHand())));
    }
  }
}
