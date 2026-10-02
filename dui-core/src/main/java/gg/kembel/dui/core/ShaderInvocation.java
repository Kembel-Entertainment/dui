package gg.kembel.dui.core;

import java.util.*;

/** Resolved consumer shader instance; timing and data are supplied by the owning component. */
public record ShaderInvocation(
    String id,
    ShaderSpec shader,
    int x,
    int y,
    int width,
    int height,
    Map<String, Object> parameters,
    int lifetimeTicks) {
  public static final int LIMIT = 8;

  public ShaderInvocation {
    Objects.requireNonNull(shader);
    parameters = shader.validate(parameters);
    if (id == null
        || id.isBlank()
        || x < 0
        || x > 511
        || y < 0
        || y > 511
        || width < 1
        || width > 511
        || height < 1
        || height > 511
        || lifetimeTicks < 0
        || lifetimeTicks >= 12000)
      throw new IllegalArgumentException("Invalid shader invocation: " + id);
  }
}
