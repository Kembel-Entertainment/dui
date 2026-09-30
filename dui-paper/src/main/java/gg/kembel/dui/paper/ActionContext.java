package gg.kembel.dui.paper;

import gg.kembel.dui.core.Canvas;
import io.papermc.paper.dialog.DialogResponseView;
import org.bukkit.entity.Player;

public record ActionContext(
    Player player, DialogSession session, Canvas.Hit hit, DialogResponseView response) {
  public String action() {
    return hit.action();
  }

  public String value() {
    return hit.value();
  }

  public String id() {
    return hit.id();
  }
}
