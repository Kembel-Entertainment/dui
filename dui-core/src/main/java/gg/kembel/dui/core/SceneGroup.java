package gg.kembel.dui.core;

import java.util.*;

/**
 * Shared group geometry. Raster children support affine transforms; native capabilities are
 * explicit.
 */
public record SceneGroup(Transform2D transform, Scene.Rect clip, double opacity) {
  public SceneGroup {
    Objects.requireNonNull(transform);
    if (!Double.isFinite(opacity)
        || opacity < 0
        || opacity > 1
        || clip != null && (clip.width() < 1 || clip.height() < 1))
      throw new IllegalArgumentException("Group opacity/clip");
  }

  public static SceneGroup translated(double x, double y) {
    return new SceneGroup(Transform2D.translation(x, y), null, 1);
  }

  public static Set<String> capabilities(Scene.Backend backend) {
    return switch (backend) {
      case FONT, IMAGE, BACKGROUND_IMAGE -> Set.of("affine", "opacity", "rectangle-clip");
      case NATIVE ->
          Set.of("translation", "uniform-scale", "rotation", "opacity", "rectangle-clip");
      case EFFECT -> Set.of("translation", "scale", "opacity");
      case HEAD, PLAYER_MODEL -> Set.of("translation");
      case HIT -> Set.of("axis-aligned-grid-transform", "rectangle-clip");
    };
  }
}
