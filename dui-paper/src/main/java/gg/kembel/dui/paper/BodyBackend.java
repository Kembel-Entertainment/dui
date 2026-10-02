package gg.kembel.dui.paper;

import gg.kembel.dui.core.*;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import java.util.*;

/** Trusted consumer rendering adapter. It owns lowering, bounds and supported transformations. */
public interface BodyBackend {
  Set<String> requiredPackCapabilities();

  Set<String> geometryCapabilities();

  List<DialogBody> render(RenderPrimitive primitive, ViewModel model);

  default void validate(RenderPrimitive primitive) {
    if (!primitive.transform().equals(Transform2D.IDENTITY)
        && !geometryCapabilities().contains("affine"))
      throw new IllegalArgumentException(
          "Backend does not support affine placement: " + primitive.type());
    if (primitive.clip() != null && !geometryCapabilities().contains("rectangle-clip"))
      throw new IllegalArgumentException("Backend does not support clipping: " + primitive.type());
    if (primitive.opacity() != 1 && !geometryCapabilities().contains("opacity"))
      throw new IllegalArgumentException("Backend does not support opacity: " + primitive.type());
  }
}
