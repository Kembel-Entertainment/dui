package gg.kembel.dui.paper;
import gg.kembel.dui.core.Canvas;
import java.util.function.*;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.*;

/** Compatibility facade for the platform-independent Adventure canvas renderer. */
public final class CanvasRenderer {
  private final gg.kembel.dui.runtime.CanvasRenderer shared = new gg.kembel.dui.runtime.CanvasRenderer();
  public Component render(Canvas canvas, Function<Canvas.Hit, Key> callback) { return shared.render(canvas, callback); }
  public Component render(Canvas canvas, Function<Canvas.Hit, Key> callback, Function<Canvas.Head, Component> heads) { return shared.render(canvas, callback, heads); }
  public Component render(Canvas canvas, Function<Canvas.Hit, Key> callback, Function<Canvas.Head, Component> heads, Function<Canvas.Hit, HoverEvent<?>> tooltip) { return shared.render(canvas, callback, heads, tooltip); }
  public Component renderActions(Canvas canvas, Function<Canvas.Hit, ClickEvent> callback, Function<Canvas.Head, Component> heads, Function<Canvas.Hit, HoverEvent<?>> tooltip) { return shared.renderActions(canvas, callback, heads, tooltip); }
  public Component renderActions(Canvas canvas, Function<Canvas.Hit, ClickEvent> callback, Function<Canvas.Head, Component> heads, Function<Canvas.Hit, HoverEvent<?>> tooltip, BiFunction<Canvas.PlayerModel, Integer, Component> players) { return shared.renderActions(canvas, callback, heads, tooltip, players); }
}
