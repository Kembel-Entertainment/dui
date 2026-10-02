package gg.kembel.dui.paper;

import gg.kembel.dui.core.*;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public final class DialogSession {
  final Dui owner;
  final Player player;
  long revision;
  long callbackRevision;

  record ActionIdentity(String id, String action, String value, boolean closes, boolean canvas) {}

  final java.util.Map<ActionIdentity, net.kyori.adventure.key.Key> actionKeys =
      new java.util.HashMap<>();
  final java.util.Set<ActionIdentity> liveActions = new java.util.HashSet<>();
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
  private UiScheduler.Cancellation animation = () -> {};
  boolean sampling;

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
    animation.cancel();
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
    owner.validate(canvas, model);
    if (!sampling) {
      animation.cancel();
      callbackRevision++;
    }
    viewTasks.close();
    viewTasks = new TaskScope(owner.scheduler(), () -> active && player.isOnline());
    this.canvas = canvas;
    this.model = model;
    this.options = options;
    this.handler = handler;
    revision++;
    owner.display(this);
  }

  public record Frame(Canvas canvas, ViewModel model) {}

  /**
   * General frame sampler for text/image/group tracks; native shader Motion remains client-side.
   */
  public UiScheduler.Cancellation animate(
      long duration, int interval, java.util.function.LongFunction<Frame> frames) {
    return animate(duration, interval, frames, frame -> {});
  }

  /** Optional observer runs after a frame has been validated and displayed. */
  public UiScheduler.Cancellation animate(
      long duration,
      int interval,
      java.util.function.LongFunction<Frame> frames,
      java.util.function.Consumer<Frame> displayed) {
    owner.mainThread();
    if (!active) throw new IllegalStateException("Dialog session is closed");
    animation.cancel();
    callbackRevision++;
    owner.clearCallbacks(this);
    animation =
        tasks.sample(
            "__dui_animation",
            duration,
            interval,
            age -> {
              var frame = java.util.Objects.requireNonNull(frames.apply(age));
              sampling = true;
              try {
                update(frame.canvas(), frame.model(), options, handler);
              } finally {
                sampling = false;
              }
              displayed.accept(frame);
            });
    return animation;
  }

  public void close() {
    owner.mainThread();
    owner.dismiss(this, true);
  }
}
