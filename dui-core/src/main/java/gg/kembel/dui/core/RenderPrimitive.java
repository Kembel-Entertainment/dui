package gg.kembel.dui.core;

import java.util.*;

/** Consumer primitive lowered by a registered platform backend, after the built-in body phases. */
public record RenderPrimitive(
    String id,
    String type,
    Scene.Rect bounds,
    Map<String, Object> data,
    Transform2D transform,
    Scene.Rect clip,
    double opacity) {
  public RenderPrimitive(String id, String type, Scene.Rect bounds, Map<String, Object> data) {
    this(id, type, bounds, data, Transform2D.IDENTITY, null, 1);
  }

  public RenderPrimitive {
    if (id == null
        || id.isBlank()
        || type == null
        || !type.matches("[a-z][a-z0-9_-]*:[a-z0-9_/-]+")
        || bounds == null
        || bounds.width() < 1
        || bounds.height() < 1
        || transform == null
        || !Double.isFinite(opacity)
        || opacity < 0
        || opacity > 1) throw new IllegalArgumentException("Extension primitive");
    data = ValueCodec.object().decode(data);
  }
}
