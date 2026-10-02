package gg.kembel.dui.core;

import java.util.*;

/** Intrinsic measurement in logical canvas pixels, before allocation or painting. */
public final class Measure {
  private Measure() {}

  public record Constraints(int minWidth, int maxWidth, int minHeight, int maxHeight) {
    public Constraints {
      if (minWidth < 0 || minHeight < 0 || maxWidth < minWidth || maxHeight < minHeight)
        throw new IllegalArgumentException("Invalid measurement constraints");
    }

    public static Constraints available(int width, int height) {
      return new Constraints(0, width, 0, height);
    }

    public Size constrain(Size size) {
      return new Size(
          Math.clamp(size.width(), minWidth, maxWidth),
          Math.clamp(size.height(), minHeight, maxHeight));
    }
  }

  public record Size(int width, int height) {
    public Size {
      if (width < 0 || height < 0) throw new IllegalArgumentException("Negative measured size");
    }
  }

  @FunctionalInterface
  public interface Children {
    Size measure(MenuTemplate.Node child, Constraints constraints);
  }

  public record Context(
      MenuTemplate.Node node,
      Constraints constraints,
      RenderEnvironment environment,
      Children children) {}
}
