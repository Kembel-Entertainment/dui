package gg.kembel.dui.runtime;

import gg.kembel.dui.core.Canvas;

public record ActionContext(UiViewer viewer, DialogSession session, Canvas.Hit hit, DialogResponse response) {
  public String action() { return hit.action(); }
  public String value() { return hit.value(); }
  public String id() { return hit.id(); }
}
