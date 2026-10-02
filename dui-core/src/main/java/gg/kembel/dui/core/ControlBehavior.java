package gg.kembel.dui.core;

import java.util.*;

/** State and action hit handling, independent of colours, symbols and skin dimensions. */
public final class ControlBehavior {
  private ControlBehavior() {}

  public static void hits(ComponentContext ctx, WidgetSkinRegistry.Skin skin) {
    var c = ctx.canvas();
    var n = ctx.node();
    String label = n.s("label", ""), action = n.s("action", ""), value = n.s("value", "");
    boolean locked = n.b("locked");
    if (n.type().equals("choice") && !locked && !action.isBlank()) {
      var parts = skin.geometry().parts(n, ctx.width(), ctx.height());
      for (String name : List.of("previous", "next")) {
        var p = Objects.requireNonNull(parts.get(name), "Choice skin part " + name);
        if (p.x() < 0
            || p.y() < 0
            || p.x() + p.width() > ctx.width()
            || p.y() + p.height() > ctx.height())
          throw new IllegalArgumentException("Choice skin part overflow");
        c.hit(
            new Canvas.Hit(
                n.s("id", action) + "_" + name,
                action,
                name.equals("previous") ? "-1" : "1",
                n.s(name + "-tooltip", label),
                ctx.x() + p.x(),
                ctx.y() + p.y(),
                p.width(),
                p.height()));
      }
    } else basicHits(c, n, ctx.x(), ctx.y(), ctx.width(), ctx.height());
  }

  public static void basicHits(Canvas c, MenuTemplate.Node n, int x, int y, int width, int height) {
    String label = n.s("label", ""), action = n.s("action", ""), value = n.s("value", "");
    boolean locked = n.b("locked");
    if (locked || !action.isBlank())
      c.hit(
          new Canvas.Hit(
              n.s("id", locked ? "locked:" + label : action + ":" + value),
              locked ? "" : action,
              locked ? "" : n.s("payload", value),
              n.s("tooltip", label),
              x,
              y,
              width,
              height));
  }
}
