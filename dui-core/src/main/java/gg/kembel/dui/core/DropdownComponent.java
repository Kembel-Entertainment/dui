package gg.kembel.dui.core;

import java.util.*;

/** Canvas dropdown with a deferred popup layer and an outside-click dismiss target. */
public final class DropdownComponent {
  private DropdownComponent() {}

  public static void draw(
      Canvas c,
      MenuTemplate.Node n,
      int x,
      int y,
      int w,
      int h,
      List<Runnable> overlays,
      WidgetSkinRegistry.Skin skin) {
    String id = n.s("id", ""), value = n.s("value", "");
    if (id.isBlank())
      throw new IllegalArgumentException("Dropdown needs an id and positive dimensions");
    var values = new HashSet<String>();
    if (n.children().isEmpty() || n.children().size() > 8)
      throw new IllegalArgumentException("Dropdown needs 1..8 options");
    for (var option : n.children())
      if (!option.type().equals("option")
          || option.s("value", "").isBlank()
          || !values.add(option.s("value", "")))
        throw new IllegalArgumentException("Dropdown options need distinct values");
    String label =
        n.children().stream()
            .filter(o -> o.s("value", "").equals(value))
            .map(o -> o.s("label", value))
            .findFirst()
            .orElse(n.s("label", ""));
    boolean locked = n.b("locked"), open = n.b("open") && !locked;
    var fieldProps = new HashMap<>(n.props());
    fieldProps.put("label", label);
    skin.painter()
        .draw(
            new ComponentContext(
                c,
                new MenuTemplate.Node("dropdown", fieldProps, n.children()),
                x,
                y,
                w,
                h,
                Map.of(),
                (child, cx, cy, cw, ch) -> {
                  throw new IllegalArgumentException("Dropdown skin cannot add child layouts");
                },
                overlays::add),
            "field");
    var header =
        new Canvas.Hit(
            id, locked ? "" : n.s("action", ""), "", n.s("tooltip", n.s("label", "")), x, y, w, h);
    if (!open) {
      c.hit(header);
      return;
    }
    String dismiss = n.s("dismiss", ""), select = n.s("select", "");
    if (dismiss.isBlank() || select.isBlank())
      throw new IllegalArgumentException("Open dropdown needs select and dismiss actions");
    int popupHeight = n.children().size() * skin.optionHeight();
    int top = y + h + popupHeight <= c.height ? y + h : y - popupHeight;
    if (top < 0)
      throw new IllegalArgumentException("Dropdown popup does not fit above or below its field");
    overlays.add(
        () -> {
          var previous = c.style(n.props());
          try {
            c.cover(id + "_popup", x, top, w, popupHeight);
            c.hit(
                new Canvas.Hit(
                    id + "_dismiss",
                    dismiss,
                    "",
                    n.s("dismiss-tooltip", ""),
                    0,
                    0,
                    c.width,
                    c.height));
            c.hit(header);
            skin.painter()
                .draw(
                    new ComponentContext(
                        c,
                        n,
                        x,
                        top,
                        w,
                        popupHeight,
                        Map.of(),
                        (child, cx, cy, cw, ch) -> {},
                        overlays::add),
                    "popup");
            for (int i = 0; i < n.children().size(); i++) {
              var option = n.children().get(i);
              int oy = top + i * skin.optionHeight();
              var props = new HashMap<>(option.props());
              props.put("selected", Boolean.toString(option.s("value", "").equals(value)));
              skin.painter()
                  .draw(
                      new ComponentContext(
                          c,
                          new MenuTemplate.Node("dropdown", props, List.of()),
                          x,
                          oy,
                          w,
                          skin.optionHeight(),
                          Map.of(),
                          (child, cx, cy, cw, ch) -> {},
                          overlays::add),
                      "option");
              c.hit(
                  new Canvas.Hit(
                      id + "_option_" + i,
                      option.b("locked") ? "" : select,
                      option.s("value", ""),
                      option.s("tooltip", option.s("label", "")),
                      x,
                      oy,
                      w,
                      skin.optionHeight()));
            }
          } finally {
            c.style(previous);
          }
        });
  }

  private static boolean overlaps(int ax, int ay, int aw, int ah, int bx, int by, int bw, int bh) {
    return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
  }
}
