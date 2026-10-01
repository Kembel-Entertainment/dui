package gg.kembel.dui.paper;

import java.util.*;
import java.util.function.*;

/** Optional state controller contract. The application owns its state and eligibility checks. */
public record MenuDefinition<S>(
    String id,
    Function<S, MenuView> project,
    BiConsumer<S, ActionContext> action,
    Consumer<S> closed) {
  public MenuDefinition {
    if (id == null || id.isBlank()) throw new IllegalArgumentException("Menu id");
    Objects.requireNonNull(project);
    Objects.requireNonNull(action);
    Objects.requireNonNull(closed);
  }
}
