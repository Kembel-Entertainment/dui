package gg.kembel.dui.paper;

import gg.kembel.dui.core.*;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public final class DialogSession {
  final Dui owner;
  final Player player;
  long revision;
  boolean active = true;
  Runnable closed = () -> {};
  Canvas canvas;
  ViewModel model;
  DialogOptions options;
  ActionHandler handler;
  Component component;
  long renderNanos;
  int bodyCount;

  public long renderNanos() {
    return renderNanos;
  }

  public int bodyCount() {
    return bodyCount;
  }

  private final TaskScope tasks;
  private TaskScope viewTasks;

  DialogSession(Dui owner, Player player) {
    this.owner = owner;
    this.player = player;
    this.tasks = new TaskScope(owner.scheduler(), () -> active && player.isOnline());
    this.viewTasks = new TaskScope(owner.scheduler(), () -> active && player.isOnline());
  }

  /** Lives until close; replacing a named job prevents stale async results across updates. */
  public TaskScope tasks() {
    owner.mainThread();
    if (!active) throw new IllegalStateException("Dialog session is closed");
    return tasks;
  }

  /** Jobs are cancelled at the next update or close. Durable business work belongs elsewhere. */
  public TaskScope viewTasks() {
    owner.mainThread();
    if (!active) throw new IllegalStateException("Dialog session is closed");
    return viewTasks;
  }

  void disposeTasks() {
    tasks.close();
    viewTasks.close();
  }

  public boolean isActive() {
    return active;
  }

  public long revision() {
    return revision;
  }

  public Canvas canvas() {
    return canvas;
  }

  public Component component() {
    return component;
  }

  public DialogSession onClose(Runnable handler) {
    owner.mainThread();
    this.closed = java.util.Objects.requireNonNull(handler);
    return this;
  }

  public void update(
      MenuTemplate template, ViewModel model, DialogOptions options, ActionHandler handler) {
    update(template.render(model.data(), model.images()), model, options, handler);
  }

  public void update(Canvas canvas, ViewModel model, DialogOptions options, ActionHandler handler) {
    owner.mainThread();
    if (!active) throw new IllegalStateException("Dialog session is closed");
    viewTasks.close();
    viewTasks = new TaskScope(owner.scheduler(), () -> active && player.isOnline());
    this.canvas = canvas;
    this.model = model;
    this.options = options;
    this.handler = handler;
    revision++;
    owner.display(this);
  }

  public void close() {
    owner.mainThread();
    owner.dismiss(this, true);
  }
}
