package gg.kembel.dui.paper;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ProtocolCompatibilityTest {
  private static final Set<String> CAPS =
      Set.of("native", "shader-components-v1", "clips", "motion-tracks", "effects-32");

  @Test
  void rejectsOldAndMismatchedPacks() {
    for (int version : List.of(1, 2))
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new PackMetadata(
                  "26.2",
                  "0.2.0-SNAPSHOT",
                  "a".repeat(40),
                  Set.of(),
                  Map.of("?", 6),
                  version,
                  CAPS,
                  RendererProtocol.SCHEMA_SHA256));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PackMetadata(
                "26.2",
                "0.2.0-SNAPSHOT",
                "a".repeat(40),
                Set.of(),
                Map.of("?", 6),
                3,
                CAPS,
                "wrong"));
  }

  @Test
  void rejectsUnknownOrMismatchedShaderSchemas() {
    var spec = new ShaderSpec("acme:rect", List.of(ShaderSpec.Parameter.rgb("tint")));
    var registry = ShaderRegistry.bind(List.of(spec));
    var metadata =
        new PackMetadata(
            "26.2",
            "0.2.0-SNAPSHOT",
            "a".repeat(40),
            Set.of(),
            Map.of("?", 6),
            RendererProtocol.VERSION,
            CAPS,
            RendererProtocol.SCHEMA_SHA256,
            Map.of(),
            Map.of(),
            registry,
            Map.of());
    var c = TestEnvironment.canvas(180, 90);
    c.effect(new ShaderInvocation("a", spec, 0, 0, 18, 18, Map.of("tint", 0xFF0011), 0));
    assertDoesNotThrow(() -> metadata.validate(c));
    var other = TestEnvironment.canvas(180, 90);
    other.effect(
        new ShaderInvocation(
            "b", new ShaderSpec("acme:rect", List.of()), 0, 0, 18, 18, Map.of(), 0));
    assertThrows(IllegalArgumentException.class, () -> metadata.validate(other));
  }
}
