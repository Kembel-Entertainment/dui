package gg.kembel.dui.core;

import java.util.*;
import java.util.function.Consumer;

/** Allocated absolute GUI coordinates, shared resources and child/overlay composition. */
public record ComponentContext(
    Canvas canvas,
    MenuTemplate.Node node,
    int x,
    int y,
    int width,
    int height,
    Map<String, RasterImage> images,
    ChildRenderer children,
    Consumer<Runnable> overlays) {
  @FunctionalInterface
  public interface ChildRenderer {
    void draw(MenuTemplate.Node child, int x, int y, int width, int height);

    default Measure.Size measure(MenuTemplate.Node child, Measure.Constraints constraints) {
      throw new UnsupportedOperationException("Child measurement requires a compiled template");
    }
  }
}
