package gg.kembel.dui.paper;

import gg.kembel.dui.core.video.MapVideoCodec;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.entity.Player;

/** Version-pinned packet bridge. Never registers maps/entities in a world or changes gamemode. */
final class MapVideoBridge implements AutoCloseable {
  private static final String GAME = "net.minecraft.network.protocol.game.";
  private final Object channel;
  private final Executor eventLoop;
  private final Method write, writable, addListener, futureSuccess, futureCause;
  private final Class<?> futureListener;
  private final Constructor<?> mapId, patch, mapPacket, bundle, remove;
  private final int[] entities, maps;
  private final AtomicBoolean pending = new AtomicBoolean();
  private volatile boolean closed;

  private static Class<?> type(String name) throws ClassNotFoundException { return Class.forName(name); }
  private static Object call(Object value, String name) throws ReflectiveOperationException { return value.getClass().getMethod(name).invoke(value); }
  MapVideoBridge(Player player, int[] mapIds, java.util.function.Consumer<Throwable> failure) throws Exception {
    Object playerHandle = call(player, "getHandle");
    Object listener = playerHandle.getClass().getField("connection").get(playerHandle);
    Object connection = listener.getClass().getField("connection").get(listener);
    channel = connection.getClass().getField("channel").get(connection);
    eventLoop = (Executor) type("io.netty.channel.Channel").getMethod("eventLoop").invoke(channel);
    writable = type("io.netty.channel.Channel").getMethod("isWritable");
    write = type("io.netty.channel.Channel").getMethod("writeAndFlush", Object.class);
    futureListener = type("io.netty.channel.ChannelFutureListener");
    addListener = type("io.netty.channel.ChannelFuture").getMethod("addListener", type("io.netty.util.concurrent.GenericFutureListener"));
    futureSuccess = type("io.netty.util.concurrent.Future").getMethod("isSuccess");
    futureCause = type("io.netty.util.concurrent.Future").getMethod("cause");
    mapId = type("net.minecraft.world.level.saveddata.maps.MapId").getConstructor(int.class);
    patch = type("net.minecraft.world.level.saveddata.maps.MapItemSavedData$MapPatch").getConstructor(int.class, int.class, int.class, int.class, byte[].class);
    mapPacket = type(GAME + "ClientboundMapItemDataPacket").getConstructor(mapId.getDeclaringClass(), byte.class, boolean.class, Collection.class, patch.getDeclaringClass());
    bundle = type(GAME + "ClientboundBundlePacket").getConstructor(Iterable.class);
    remove = type(GAME + "ClientboundRemoveEntitiesPacket").getConstructor(int[].class);
    maps = mapIds.clone(); entities = new int[maps.length];
    Object world = call(player.getWorld(), "getHandle");
    var eye = player.getEyeLocation();
    var entityClass = type("net.minecraft.world.entity.Entity");
    var levelClass = type("net.minecraft.world.level.Level");
    var blockClass = type("net.minecraft.core.BlockPos");
    Object block = blockClass.getConstructor(int.class, int.class, int.class).newInstance(eye.getBlockX(), eye.getBlockY(), eye.getBlockZ());
    var directionClass = type("net.minecraft.core.Direction");
    String direction = eye.getZ() - eye.getBlockZ() >= .5 ? "NORTH" : "SOUTH";
    Object facing = directionClass.getField(direction).get(null);
    int facingId = (int) directionClass.getMethod("get3DDataValue").invoke(facing);
    var add = type(GAME + "ClientboundAddEntityPacket").getConstructor(entityClass, int.class, blockClass);
    var metadata = type(GAME + "ClientboundSetEntityDataPacket").getConstructor(int.class, List.class);
    var frameClass = type("net.minecraft.world.entity.decoration.GlowItemFrame");
    var stackClass = type("net.minecraft.world.item.ItemStack");
    var itemClass = type("net.minecraft.world.level.ItemLike");
    var componentClass = type("net.minecraft.core.component.DataComponentType");
    Object filledMap = type("net.minecraft.world.item.Items").getField("FILLED_MAP").get(null);
    Object mapComponent = type("net.minecraft.core.component.DataComponents").getField("MAP_ID").get(null);
    var packets = new ArrayList<Object>();
    for (int i = 0; i < maps.length; i++) {
      Object frame = frameClass.getConstructor(levelClass, blockClass, directionClass).newInstance(world, block, facing);
      Object stack = stackClass.getConstructor(itemClass).newInstance(filledMap);
      stackClass.getMethod("set", componentClass, Object.class).invoke(stack, mapComponent, mapId.newInstance(maps[i]));
      frameClass.getMethod("setInvisible", boolean.class).invoke(frame, true);
      frameClass.getMethod("setItem", stackClass, boolean.class).invoke(frame, stack, false);
      int id = (int) call(frame, "getId"); entities[i] = id;
      packets.add(add.newInstance(frame, facingId, block));
      packets.add(metadata.newInstance(id, call(call(frame, "getEntityData"), "packAll")));
    }
    // Keep the local player camera: Vanilla gates keyboard polling on isControlledCamera().
    Object initial = bundle.newInstance(packets);
    eventLoop.execute(() -> {
      try { if (!closed) completion(write.invoke(channel, initial), () -> {}, failure); }
      catch (Throwable error) { failure.accept(error); }
    });
  }
  boolean ready() throws Exception { return !closed && !pending.get() && (boolean) writable.invoke(channel); }
  Object framePacket(int index, MapVideoCodec.Patch p) throws Exception {
    return mapPacket.newInstance(mapId.newInstance(maps[index]), (byte) 0, true, List.of(),
        patch.newInstance(p.x(), p.y(), p.width(), p.height(), p.colors()));
  }
  boolean sendFrame(List<Object> packets, Runnable success, java.util.function.Consumer<Throwable> failure) throws Exception {
    if (!ready() || !pending.compareAndSet(false, true)) return false;
    Object packet = bundle.newInstance(packets);
    eventLoop.execute(() -> {
      try {
        if (closed) { pending.set(false); return; }
        // Connection.send() queues non-whitelisted packets off the server thread until its next tick.
        // The active GAME pipeline already handles bundling, compression and connection-local masks.
        Object future = write.invoke(channel, packet);
        completion(future, () -> { try { success.run(); } finally { pending.set(false); } },
            error -> { try { failure.accept(error); } finally { pending.set(false); } });
      } catch (Throwable e) { pending.set(false); failure.accept(e); }
    });
    return true;
  }
  private void completion(Object future, Runnable success, java.util.function.Consumer<Throwable> failure) throws Exception {
    Object listener = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{futureListener},
        (proxy, method, arguments) -> {
          if (method.getDeclaringClass() == Object.class) return switch (method.getName()) {
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == arguments[0];
            default -> "dui video completion";
          };
          if (method.getName().equals("operationComplete")) {
            try {
              if ((boolean) futureSuccess.invoke(arguments[0])) success.run();
              else {
                Throwable cause = (Throwable) futureCause.invoke(arguments[0]);
                failure.accept(cause == null ? new IllegalStateException("Video write cancelled") : cause);
              }
            } catch (Throwable error) { failure.accept(error); }
          }
          return null;
        });
    addListener.invoke(future, listener);
  }
  @Override public void close() {
    closed = true;
    eventLoop.execute(() -> {
      try { write.invoke(channel, bundle.newInstance(List.of(remove.newInstance((Object) entities)))); }
      catch (ReflectiveOperationException ignored) { /* connection teardown has no client entities to restore */ }
    });
  }
}
