package gg.kembel.dui.core;

import java.util.*;

/** Consumer-owned visual skins. Library behavior uses the same named rectangles as the skin. */
public final class WidgetSkinRegistry {
  public record Part(int x, int y, int width, int height) {
    public Part {
      if (width < 1 || height < 1) throw new IllegalArgumentException("Skin part bounds");
    }
  }

  @FunctionalInterface
  public interface Painter {
    void draw(ComponentContext context, String part);
  }

  @FunctionalInterface
  public interface Geometry {
    Map<String, Part> parts(MenuTemplate.Node node, int width, int height);
  }

  public record Skin(
      int height,
      int paddingX,
      int paddingY,
      int optionHeight,
      Painter painter,
      Geometry geometry,
      PropertySchema properties) {
    public Skin(
        int height,
        int paddingX,
        int paddingY,
        int optionHeight,
        Painter painter,
        Geometry geometry) {
      this(
          height,
          paddingX,
          paddingY,
          optionHeight,
          painter,
          geometry,
          new PropertySchema(Map.of()));
    }

    public Skin {
      if (height < 1 || paddingX < 0 || paddingY < 0 || optionHeight < 9 || optionHeight % 9 != 0)
        throw new IllegalArgumentException("Skin metrics");
      Objects.requireNonNull(painter);
      Objects.requireNonNull(geometry);
      Objects.requireNonNull(properties);
    }
  }

  public static final WidgetSkinRegistry EMPTY = new WidgetSkinRegistry(Map.of());
  private final Map<String, Skin> skins;

  public WidgetSkinRegistry(Map<String, Skin> skins) {
    this.skins = Map.copyOf(skins);
  }

  public Map<String, Skin> all() {
    return skins;
  }

  public Optional<Skin> find(String name) {
    return Optional.ofNullable(skins.get(name));
  }

  public Skin require(String name) {
    return find(name)
        .orElseThrow(() -> new IllegalArgumentException("No consumer skin registered for " + name));
  }
}
