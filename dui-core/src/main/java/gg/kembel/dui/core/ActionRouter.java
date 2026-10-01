package gg.kembel.dui.core;

import java.util.*;
import java.util.function.*;

/** Typed routing with an explicit rejection policy. Business authorization belongs in handlers. */
public final class ActionRouter<C> {
  private final Map<String, BiConsumer<C, String>> routes = new LinkedHashMap<>();
  private final BiConsumer<C, String> rejected;

  public ActionRouter(BiConsumer<C, String> rejected) {
    this.rejected = Objects.requireNonNull(rejected);
  }

  public <T> ActionRouter<C> on(
      String action, Function<String, T> decoder, BiConsumer<C, T> handler) {
    if (action.isBlank() || routes.containsKey(action))
      throw new IllegalArgumentException("Duplicate/blank action: " + action);
    routes.put(
        action,
        (context, payload) -> {
          T value;
          try {
            value = decoder.apply(payload);
          } catch (IllegalArgumentException e) {
            rejected.accept(context, action);
            return;
          }
          handler.accept(context, value);
        });
    return this;
  }

  public ActionRouter<C> on(String action, Consumer<C> handler) {
    return on(action, p -> p, (c, p) -> handler.accept(c));
  }

  public void dispatch(C context, String action, String payload) {
    var handler = routes.get(action);
    if (handler == null) rejected.accept(context, action);
    else handler.accept(context, payload);
  }

  public Set<String> actions() {
    return Set.copyOf(routes.keySet());
  }
}
