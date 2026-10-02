package gg.kembel.dui.core;

import java.util.*;

/** Pack-assigned address and fingerprint of the exact ordered parameter schema. */
public record ShaderBinding(ShaderSpec specification, int code, String schemaHash) {
  public ShaderBinding(ShaderSpec specification, int code) {
    this(specification, code, specification.hash());
  }

  public ShaderBinding {
    Objects.requireNonNull(specification);
    if (code < 0 || code >= 64) throw new IllegalArgumentException("Shader code outside profile");
    if (!specification.hash().equals(schemaHash))
      throw new IllegalArgumentException("Shader schema hash mismatch: " + specification.id());
  }
}
