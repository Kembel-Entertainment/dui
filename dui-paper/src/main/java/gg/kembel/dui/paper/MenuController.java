package gg.kembel.dui.paper;

import gg.kembel.dui.core.*;
import java.util.*;
import org.bukkit.entity.Player;

/** Shared presentation/lifecycle boundary. Durable work must be owned by the application. */
public final class MenuController<S> implements AutoCloseable {
  private final Dui dui;
  private final Player player;
  private final S state;
  private final MenuDefinition<S> definition;
  private DialogSession session;
  private java.util.function.Consumer<DialogSession> presented = ignored -> {};

  public MenuController(Dui dui, Player player, S state, MenuDefinition<S> definition) {
    this.dui = Objects.requireNonNull(dui);
    this.player = Objects.requireNonNull(player);
    this.state = Objects.requireNonNull(state);
    this.definition = Objects.requireNonNull(definition);
  }

  /** Observe completed presentation (diagnostics/application effects stay outside projection). */
  public MenuController<S> onPresented(java.util.function.Consumer<DialogSession> observer) {
    presented = Objects.requireNonNull(observer);
    return this;
  }

  public S state() {
    return state;
  }

  public boolean active() {
    return session != null && session.isActive();
  }

  public DialogSession session() {
    if (!active()) throw new IllegalStateException("Menu is closed");
    return session;
  }

  public void refresh() {
    present(
        definition.project().apply(state),
        ctx -> {
          definition.action().accept(state, ctx);
          if (active()) refresh();
        });
  }

  /**
   * Compatibility entry point for consumers that already compose Canvas and native input options.
   */
  public void present(MenuView view, ActionHandler action) {
    if (active()) session.update(view.canvas(), view.model(), view.options(), action);
    else
      session =
          dui.open(player, view.canvas(), view.model(), view.options(), action)
              .onClose(() -> definition.closed().accept(state));
    presented.accept(session);
  }

  public TaskScope tasks() {
    return session().tasks();
  }

  public TaskScope viewTasks() {
    return session().viewTasks();
  }

  @Override
  public void close() {
    if (active()) session.close();
  }
}
