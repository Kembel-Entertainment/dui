package gg.kembel.dui.core;

import java.util.*;

/** Canvas dropdown with a deferred popup layer and an outside-click dismiss target. */
public final class DropdownComponent {
  private DropdownComponent() {}

  public static void draw(
      Canvas c, MenuTemplate.Node n, int x, int y, int w, int h, List<Runnable> overlays) {
    String id = n.s("id", ""), value = n.s("value", "");
    if (id.isBlank() || h < 18 || w < 45)
      throw new IllegalArgumentException("Dropdown needs an id and at least 45x18 pixels");
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
            .orElse(n.s("label", "Select an option"));
    boolean locked = n.b("locked"), open = n.b("open") && !locked;
    c.panel(x, y, w, h - 1, locked ? 0x202127 : 0x292B35, open ? 0x58E6DB : 0x34343F);
    int ty = y + (h - 9) / 2;
    c.text(x + 6, ty, w - 30, label, locked ? 0x9697A5 : 0xEAEAF1);
    c.text(x + w - 17, ty, 12, open ? "^" : "v", locked ? 0x9697A5 : 0x58E6DB);
    var header =
        new Canvas.Hit(
            id,
            locked ? "" : n.s("action", ""),
            "",
            n.s("tooltip", n.s("label", "Select an option")),
            x,
            y,
            w,
            h);
    if (!open) {
      c.hit(header);
      return;
    }
    String dismiss = n.s("dismiss", ""), select = n.s("select", "");
    if (dismiss.isBlank() || select.isBlank())
      throw new IllegalArgumentException("Open dropdown needs select and dismiss actions");
    int popupHeight = n.children().size() * 18;
    int top = y + h + popupHeight <= c.height ? y + h : y - popupHeight;
    if (top < 0)
      throw new IllegalArgumentException("Dropdown popup does not fit above or below its field");
    overlays.add(
        () -> {
          // Native objects render after font paints; suppress objects covered by the popup.
          c.items.removeIf(i -> overlaps(i.x(), i.y(), i.size(), i.size(), x, top, w, popupHeight));
          c.heads.removeIf(head -> overlaps(head.x(), head.y(), 8, 8, x, top, w, popupHeight));
          c.hit(
              new Canvas.Hit(
                  id + "_dismiss", dismiss, "", "Close options", 0, 0, c.width, c.height));
          c.hit(header);
          c.panel(x, top, w, popupHeight, 0x22232B, 0x58E6DB);
          for (int i = 0; i < n.children().size(); i++) {
            var option = n.children().get(i);
            int oy = top + i * 18;
            boolean selected = option.s("value", "").equals(value);
            if (selected) c.rect(x + 1, oy + 1, w - 2, 16, 0x24504C);
            c.text(
                x + 7,
                oy + 4,
                w - 30,
                option.s("label", option.s("value", "")),
                option.b("locked") ? 0x9697A5 : selected ? 0x58E6DB : 0xEAEAF1);
            if (selected) c.icon(x + w - 17, oy + 4, "check", 0x58E6DB);
            if (option.b("locked")) c.icon(x + w - 17, oy + 4, "lock", 0x9697A5);
            c.hit(
                new Canvas.Hit(
                    id + "_option_" + i,
                    option.b("locked") ? "" : select,
                    option.s("value", ""),
                    option.s("tooltip", option.s("label", "")),
                    x,
                    oy,
                    w,
                    18));
          }
        });
  }

  private static boolean overlaps(int ax, int ay, int aw, int ah, int bx, int by, int bw, int bh) {
    return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
  }
}
