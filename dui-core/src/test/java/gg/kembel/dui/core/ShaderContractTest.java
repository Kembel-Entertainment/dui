package gg.kembel.dui.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class ShaderContractTest {
  private static ShaderSpec spec(String id) {
    return new ShaderSpec(
        id,
        List.of(
            ShaderSpec.Parameter.integer("count", 0, 1000),
            ShaderSpec.Parameter.bool("enabled"),
            ShaderSpec.Parameter.enumeration("mode", "a", "b", "c"),
            ShaderSpec.Parameter.decimal("gain", -.5, 1.5, .01),
            ShaderSpec.Parameter.rgb("tint")));
  }

  private static ShaderInvocation call(String id, ShaderSpec spec) {
    return new ShaderInvocation(
        id,
        spec,
        1,
        9,
        18,
        18,
        Map.of("count", 999, "enabled", true, "mode", "b", "gain", .25, "tint", 0xFB1452),
        20);
  }

  @Test
  void typedRoundTripAndUnknownKeys() {
    var s = spec("acme:surface");
    var call = call("i", s);
    assertTrue(s.bits() > 30);
    assertEquals(
        call.parameters(),
        ShaderParameters.decode(s, ShaderParameters.encode(s, call.parameters())));
    var bad = new HashMap<>(call.parameters());
    bad.put("typo", 1);
    assertThrows(IllegalArgumentException.class, () -> s.validate(bad));
    bad.remove("typo");
    bad.put("gain", .253);
    assertThrows(IllegalArgumentException.class, () -> s.validate(bad));
  }

  @Test
  void registryOrderAndCollision() {
    var a = spec("a:first");
    var z = spec("z:last");
    var registry = ShaderRegistry.bind(List.of(z, a));
    assertEquals(0, registry.get(a.id()).code());
    assertEquals(registry, ShaderRegistry.bind(List.of(a, z)));
    assertThrows(IllegalArgumentException.class, () -> ShaderRegistry.bind(List.of(a, a)));
    assertThrows(
        IllegalArgumentException.class,
        () -> ShaderRegistry.require(registry, new ShaderSpec("a:first", List.of())));
  }

  @Test
  void batchesUseActualBitWidthsAndRoundTripTransport() {
    var c = TestEnvironment.canvas(180, 90);
    c.effectLimit = 32;
    c.animation = new Canvas.Animation("carrier", 3, true, 20);
    var s = spec("acme:surface");
    for (int i = 0; i < 7; i++) c.effect(call("i" + i, s));
    var batches = EffectBatches.of(c);
    assertEquals(2, batches.size());
    var payload =
        ItemTransport.animationPayload(c, batches.getFirst(), ShaderRegistry.bind(List.of(s)));
    var bits = new ShaderParameters.Bits();
    bits.cells(payload.subList(18, payload.size()), batches.getFirst().size() * (42 + s.bits()));
    assertEquals(0, bits.read(0, 6));
    assertEquals(1, bits.read(6, 9));
    assertTrue(payload.size() <= 18 + 192);
  }

  @Test
  void trackedBatchesRespectMotionCost() {
    var c = TestEnvironment.canvas(180, 90);
    c.effectLimit = 32;
    c.animation = new Canvas.Animation("carrier", 10, true, 20);
    var s = spec("acme:surface");
    for (int i = 0; i < 7; i++) {
      c.effect(call("i" + i, s));
      c.effectMotion("i" + i, Motion.slide(10, 20, 12, true));
    }
    assertEquals(3, EffectBatches.of(c).size());
    assertTrue(EffectBatches.of(c).stream().allMatch(batch -> batch.size() <= 3));
  }

  @Test
  void schemaFingerprintsAndRangesRejectAmbiguity() {
    var a =
        new ShaderSpec("acme:a", List.of(ShaderSpec.Parameter.enumeration("mode", "x, y", "z")));
    var b =
        new ShaderSpec("acme:a", List.of(ShaderSpec.Parameter.enumeration("mode", "x", "y, z")));
    assertNotEquals(a.hash(), b.hash());
    assertThrows(IllegalArgumentException.class, () -> new ShaderBinding(a, 0, b.hash()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ShaderSpec.Parameter("bad", ShaderSpec.Kind.BOOLEAN, 0, 7, 1, List.of()));
    assertThrows(
        IllegalArgumentException.class, () -> ShaderSpec.Parameter.decimal("fraction", 0, 1, .3));
    assertThrows(
        IllegalArgumentException.class,
        () -> ShaderSpec.Parameter.decimal("tooLargeForGlsl", 1e100, 1e100, 1));
    assertThrows(
        IllegalArgumentException.class,
        () -> ShaderSpec.Parameter.decimal("underflow", 0, 0, 1e-100));
  }
}
