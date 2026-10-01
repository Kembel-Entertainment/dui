package gg.kembel.dui.paper;

import gg.kembel.dui.core.*;
import java.util.Objects;

/**
 * A pure projection result. Resources are already resolved; no I/O is performed by presentation.
 */
public record MenuView(Canvas canvas, ViewModel model, DialogOptions options) {
  public MenuView {
    Objects.requireNonNull(canvas);
    Objects.requireNonNull(model);
    Objects.requireNonNull(options);
  }

  public static MenuView of(MenuTemplate template, ViewModel model, DialogOptions options) {
    return new MenuView(template.render(model.data(), model.images()), model, options);
  }
}
