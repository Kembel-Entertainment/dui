package gg.kembel.dui.core;

import java.util.*;

/** A registered GPU viewport/camera family. Pose and viewport choices remain runtime parameters. */
public record PlayerRenderSpec(String id, List<Viewport> viewports, List<Pose> poses) {
  public record Viewport(int width, int height) {
    public Viewport {
      if (width < 1 || width > 480 || height < 9 || height > 360 || height % 9 != 0)
        throw new IllegalArgumentException("Model viewport");
    }
  }

  public record Pose(
      double yawDegrees,
      double pitchDegrees,
      double worldHeight,
      List<Double> limbPitch,
      double idleDegrees,
      double idleSpeed) {
    public Pose {
      limbPitch = List.copyOf(limbPitch);
      if (limbPitch.size() != 6
          || !Double.isFinite(yawDegrees)
          || !Double.isFinite(pitchDegrees)
          || !Double.isFinite(worldHeight)
          || worldHeight < 1
          || worldHeight > 256
          || !Double.isFinite(idleDegrees)
          || !Double.isFinite(idleSpeed)
          || idleSpeed < 0) throw new IllegalArgumentException("Model camera/pose");
      for (double angle : limbPitch)
        if (!Double.isFinite(angle)) throw new IllegalArgumentException("Limb angle");
    }
  }

  public PlayerRenderSpec {
    viewports = List.copyOf(viewports);
    poses = List.copyOf(poses);
    if (id == null
        || !id.matches("[a-z][a-z0-9_-]*:[a-z0-9_/-]+")
        || viewports.isEmpty()
        || viewports.size() > 4
        || poses.isEmpty()
        || poses.size() > 8
        || viewports.stream().distinct().count() != viewports.size())
      throw new IllegalArgumentException(
          "Player renderer transport supports four viewports/eight poses per family");
  }

  public int fit(int width, int height) {
    int best = -1;
    for (int i = 0; i < viewports.size(); i++) {
      var v = viewports.get(i);
      if (v.width() <= width
          && v.height() <= height
          && (best < 0 || v.height() > viewports.get(best).height())) best = i;
    }
    if (best < 0)
      throw new IllegalArgumentException(
          "Allocation cannot contain a registered model viewport: " + id);
    return best;
  }

  /** Compatibility camera family; consumers may register independent alternatives. */
  public static PlayerRenderSpec standard() {
    var poses = new ArrayList<Pose>();
    for (int i = 0; i < 8; i++)
      poses.add(
          new Pose(
              i * 45,
              Math.toDegrees(-.1),
              36,
              Collections.nCopies(6, 0.),
              Math.toDegrees(.035),
              1.8));
    return new PlayerRenderSpec(
        "dui:player",
        List.of(
            new Viewport(48, 72),
            new Viewport(72, 108),
            new Viewport(108, 162),
            new Viewport(144, 216)),
        poses);
  }
}
