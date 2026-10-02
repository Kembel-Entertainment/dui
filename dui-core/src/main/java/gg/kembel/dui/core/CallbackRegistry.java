package gg.kembel.dui.core;

import java.util.*;

/** Single-use, player-scoped capabilities. Each revision invalidates all earlier tokens. */
public final class CallbackRegistry<T> {
  private final Map<UUID, Map<UUID, T>> players = new HashMap<>();

  public UUID register(UUID player, T callback) {
    UUID token = UUID.randomUUID();
    players
        .computeIfAbsent(player, p -> new HashMap<>())
        .put(token, Objects.requireNonNull(callback));
    return token;
  }

  public T consume(UUID player, UUID token) {
    var map = players.get(player);
    if (map == null) return null;
    T result = map.get(token);
    if (result != null) players.remove(player);
    return result;
  }

  public void invalidate(UUID player) {
    players.remove(player);
  }

  /** Drop a removed animated action without invalidating unchanged actions in the same scene. */
  public void invalidate(UUID player,UUID token) {
    var map=players.get(player);
    if(map!=null){map.remove(token);if(map.isEmpty())players.remove(player);}
  }

  public void clear() {
    players.clear();
  }
}
