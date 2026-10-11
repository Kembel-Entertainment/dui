package gg.kembel.dui.paper;

import gg.kembel.dui.core.*;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/** Source-compatible facade for the shared dialog lifecycle. */
public final class DialogSession {
  private final Dui owner;
  private final Player player;
  private final gg.kembel.dui.runtime.DialogSession session;
  DialogSession(Dui owner, Player player, gg.kembel.dui.runtime.DialogSession session) { this.owner = owner; this.player = player; this.session = session; }
  public gg.kembel.dui.runtime.DialogSession portable() { return session; }
  public long renderNanos() { return session.renderNanos(); }
  public int bodyCount() { return session.bodyCount(); }
  public TaskScope tasks() { return session.tasks(); }
  public TaskScope viewTasks() { return session.viewTasks(); }
  public boolean isActive() { return session.isActive(); }
  public long revision() { return session.revision(); }
  public Canvas canvas() { return session.canvas(); }
  public Component component() { return session.component(); }
  public DialogSession onClose(Runnable handler) { session.onClose(handler); return this; }
  public void update(MenuTemplate template, ViewModel model, DialogOptions options, ActionHandler handler) { update(template.render(model.data(), model.images()), model, options, handler); }
  public void update(Canvas canvas, ViewModel model, DialogOptions options, ActionHandler handler) {
    owner.mainThread(); java.util.Objects.requireNonNull(handler);
    session.update(canvas, model.portable(), options.portable(), context -> handler.handle(new ActionContext(player, this, context.hit(),
        context.response() instanceof PaperResponse p ? p.nativeResponse() : null)));
  }
  public record Frame(Canvas canvas, ViewModel model) {}
  public UiScheduler.Cancellation animate(long duration, int interval, java.util.function.LongFunction<Frame> frames) { return animate(duration, interval, frames, frame -> {}); }
  public UiScheduler.Cancellation animate(long duration, int interval, java.util.function.LongFunction<Frame> frames, java.util.function.Consumer<Frame> displayed) {
    var latest = new Frame[1];
    return session.animate(duration, interval, age -> {
      var frame = java.util.Objects.requireNonNull(frames.apply(age)); latest[0] = frame;
      return new gg.kembel.dui.runtime.DialogSession.Frame(frame.canvas(), frame.model().portable());
    }, frame -> displayed.accept(latest[0]));
  }
  public void close() { session.close(); }
}
