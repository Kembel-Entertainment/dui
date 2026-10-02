package gg.kembel.dui.paper;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper 26.2 adapter: mask only this connection's self flags and hand inventory. No potion,
 * gamemode, equipment or inventory is changed on the server. Keeping the mask in the outbound path
 * also prevents later inventory/flag sync flicker. Reflection keeps server internals isolated; this
 * is deliberately version pinned.
 */
final class WorldMapClientAppearance implements AutoCloseable {
  private static final String GAME = "net.minecraft.network.protocol.game.";
  private final Player player;
  private final JavaPlugin plugin;
  private final int id;
  private final String name;
  private final Object handle, listener, channel, pipeline, empty;
  private final Class<?> outbound, inbound, pipelineType;
  private final AtomicBoolean active = new AtomicBoolean(true), reported = new AtomicBoolean();
  private final Runnable failure;

  WorldMapClientAppearance(JavaPlugin plugin, Player player, Runnable failure) throws Exception {
    this.plugin = plugin;
    this.player = player;
    this.failure = failure;
    id = player.getEntityId();
    name = "dui-world-appearance-" + player.getUniqueId();
    handle = call(player, "getHandle");
    listener = handle.getClass().getField("connection").get(handle);
    Object connection = listener.getClass().getField("connection").get(listener);
    channel = connection.getClass().getField("channel").get(connection);
    Class<?> channelType = type("io.netty.channel.Channel");
    pipeline = channelType.getMethod("pipeline").invoke(channel);
    pipelineType = type("io.netty.channel.ChannelPipeline");
    outbound = type("io.netty.channel.ChannelOutboundInvoker");
    inbound = type("io.netty.channel.ChannelInboundInvoker");
    empty = type("net.minecraft.world.item.ItemStack").getField("EMPTY").get(null);
    Object handler =
        Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[] {type("io.netty.channel.ChannelOutboundHandler")},
            this::forward);
    try {
      onLoop(
          () -> {
            if (active.get())
              pipelineType
                  .getMethod(
                      "addBefore",
                      String.class,
                      String.class,
                      type("io.netty.channel.ChannelHandler"))
                  .invoke(pipeline, "packet_handler", name, handler);
          });
      sendFlags(true);
      player.updateInventory();
    } catch (Exception e) {
      close();
      throw e;
    }
  }

  private Object forward(Object proxy, Method method, Object[] args) throws Throwable {
    String methodName = method.getName();
    if (method.getDeclaringClass() == Object.class)
      return switch (methodName) {
        case "toString" -> name;
        case "hashCode" -> System.identityHashCode(proxy);
        case "equals" -> proxy == args[0];
        default -> null;
      };
    if (methodName.equals("handlerAdded") || methodName.equals("handlerRemoved")) return null;
    Object context = args[0];
    if (methodName.equals("write") && active.get()) {
      try {
        args[1] = mask(args[1]);
      } catch (Exception e) {
        if (reported.compareAndSet(false, true)) {
          plugin
              .getLogger()
              .log(
                  java.util.logging.Level.SEVERE,
                  "Map appearance packet adapter failed; closing the menu",
                  e);
          plugin.getServer().getScheduler().runTask(plugin, failure);
        }
      }
    }
    Class<?>[] parameters =
        Arrays.copyOfRange(method.getParameterTypes(), 1, method.getParameterCount());
    Object[] values = Arrays.copyOfRange(args, 1, args.length);
    if (methodName.equals("exceptionCaught"))
      return inbound.getMethod("fireExceptionCaught", Throwable.class).invoke(context, values);
    return outbound.getMethod(methodName, parameters).invoke(context, values);
  }

  private Object mask(Object packet) throws Exception {
    return switch (packet.getClass().getSimpleName()) {
      case "ClientboundBundlePacket" -> {
        List<Object> items = new ArrayList<>();
        for (Object item : (Iterable<?>) call(packet, "subPackets")) items.add(mask(item));
        yield packet.getClass().getConstructor(Iterable.class).newInstance(items);
      }
      case "ClientboundSetEntityDataPacket" -> {
        if ((int) call(packet, "id") != id) yield packet;
        List<Object> items = new ArrayList<>();
        for (Object value : (List<?>) call(packet, "packedItems")) {
          if ((int) call(value, "id") == 0 && call(value, "value") instanceof Byte flags)
            value =
                value
                    .getClass()
                    .getConstructor(
                        int.class,
                        type("net.minecraft.network.syncher.EntityDataSerializer"),
                        Object.class)
                    .newInstance(0, call(value, "serializer"), (byte) (flags | 0x20));
          items.add(value);
        }
        yield packet.getClass().getConstructor(int.class, List.class).newInstance(id, items);
      }
      case "ClientboundContainerSetContentPacket" -> {
        if ((int) call(packet, "containerId") != 0) yield packet;
        List<Object> items = new ArrayList<>((List<?>) call(packet, "items"));
        for (int slot = 36; slot < Math.min(46, items.size()); slot++) items.set(slot, empty);
        yield packet
            .getClass()
            .getConstructor(int.class, int.class, List.class, empty.getClass())
            .newInstance(0, call(packet, "stateId"), items, call(packet, "carriedItem"));
      }
      case "ClientboundContainerSetSlotPacket" -> {
        int container = (int) call(packet, "getContainerId"), slot = (int) call(packet, "getSlot");
        // container -2 uses direct PlayerInventory numbering in older packet paths.
        if (!(container == 0 && slot >= 36 && slot <= 45 || container == -2 && handSlot(slot)))
          yield packet;
        yield packet
            .getClass()
            .getConstructor(int.class, int.class, int.class, empty.getClass())
            .newInstance(container, call(packet, "getStateId"), slot, empty);
      }
      case "ClientboundSetPlayerInventoryPacket" -> {
        int slot = (int) call(packet, "slot");
        yield handSlot(slot)
            ? packet.getClass().getConstructor(int.class, empty.getClass()).newInstance(slot, empty)
            : packet;
      }
      case "ClientboundSetEquipmentPacket" -> {
        if ((int) call(packet, "getEntity") != id) yield packet;
        List<Object> slots = new ArrayList<>();
        for (Object pair : (List<?>) call(packet, "getSlots")) {
          Object slot = call(pair, "getFirst");
          if (slot.toString().equals("MAINHAND") || slot.toString().equals("OFFHAND"))
            pair =
                pair.getClass()
                    .getMethod("of", Object.class, Object.class)
                    .invoke(null, slot, empty);
          slots.add(pair);
        }
        yield packet.getClass().getConstructor(int.class, List.class).newInstance(id, slots);
      }
      default -> packet;
    };
  }

  private static boolean handSlot(int slot) {
    return slot >= 0 && slot < 9 || slot == 40;
  }

  private void sendFlags(boolean hide) throws Exception {
    Field field =
        type("net.minecraft.world.entity.Entity").getDeclaredField("DATA_SHARED_FLAGS_ID");
    field.setAccessible(true);
    Object accessor = field.get(null), data = call(handle, "getEntityData");
    byte flags =
        (byte)
            data.getClass()
                .getMethod("get", type("net.minecraft.network.syncher.EntityDataAccessor"))
                .invoke(data, accessor);
    Object value =
        type("net.minecraft.network.syncher.SynchedEntityData$DataValue")
            .getMethod(
                "create", type("net.minecraft.network.syncher.EntityDataAccessor"), Object.class)
            .invoke(null, accessor, hide ? (byte) (flags | 0x20) : flags);
    Object packet =
        type(GAME + "ClientboundSetEntityDataPacket")
            .getConstructor(int.class, List.class)
            .newInstance(id, List.of(value));
    listener
        .getClass()
        .getMethod("send", type("net.minecraft.network.protocol.Packet"))
        .invoke(listener, packet);
  }

  @FunctionalInterface
  private interface Checked {
    void run() throws Exception;
  }

  private void onLoop(Checked work) throws Exception {
    Object loop = type("io.netty.channel.Channel").getMethod("eventLoop").invoke(channel);
    if ((boolean)
        type("io.netty.util.concurrent.EventExecutor").getMethod("inEventLoop").invoke(loop)) {
      work.run();
      return;
    }
    CompletableFuture<Void> done = new CompletableFuture<>();
    ((Executor) loop)
        .execute(
            () -> {
              try {
                work.run();
                done.complete(null);
              } catch (Exception e) {
                done.completeExceptionally(e);
              }
            });
    done.get(2, TimeUnit.SECONDS);
  }

  @Override
  public void close() {
    active.set(false);
    try {
      onLoop(
          () -> {
            if (pipelineType.getMethod("get", String.class).invoke(pipeline, name) != null)
              pipelineType.getMethod("remove", String.class).invoke(pipeline, name);
          });
      if (player.isOnline()) {
        sendFlags(false);
        player.updateInventory();
      }
    } catch (Exception e) {
      plugin
          .getLogger()
          .log(
              java.util.logging.Level.WARNING,
              "Could not restore client appearance for " + player.getName(),
              e);
    }
  }

  private static Class<?> type(String name) throws ClassNotFoundException {
    return Class.forName(name);
  }

  private static Object call(Object object, String name) throws Exception {
    return object.getClass().getMethod(name).invoke(object);
  }
}
