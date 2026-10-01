package gg.kembel.dui.paper;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ProtocolCompatibilityTest {
  @Test
  void legacyIsExplicitAndCannotClaimModernFeatures() {
    var old =
        new PackMetadata(
            "26.2", "0.1.0-SNAPSHOT", "0".repeat(40), Set.of(), new GlyphFont().metrics());
    assertEquals(1, old.protocolVersion());
    var c = new Canvas(180, 90);
    c.item("a", 0, 0, 16);
    c.motion("a", Motion.pop(1, 24, 18, true));
    assertThrows(IllegalArgumentException.class, () -> old.validate(c));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PackMetadata(
                "26.2",
                "0.1.0-SNAPSHOT",
                "0".repeat(40),
                Set.of(),
                new GlyphFont().metrics(),
                1,
                Set.of("motion-tracks"),
                ""));
  }

  @Test
  void legacyPresetTransitionsRetainTheirFallbackTransport() throws Exception {
    var old =
        new PackMetadata(
            "26.2", "0.1.0-SNAPSHOT", "0".repeat(40), Set.of(), new GlyphFont().metrics());
    var c =
        MenuTemplate.parse(
                "<dui-menu width='180' height='90'><dui-layer height='fill'><dui-item id='old'"
                    + " size='18' height='18' transition='pop'"
                    + " transition-start='10'/></dui-layer></dui-menu>")
            .render(Map.of());
    assertTrue(c.legacyMotion("old"));
    assertDoesNotThrow(() -> old.validate(c));
  }

  @Test
  void v2RequiresMatchingSchemaAndCapabilities() {
    var caps = Set.of("native", "effects-8", "clips", "motion-tracks", "effects-32");
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PackMetadata(
                "26.2",
                "0.1.0-SNAPSHOT",
                "0".repeat(40),
                Set.of(),
                new GlyphFont().metrics(),
                2,
                caps,
                "wrong"));
    var modern =
        new PackMetadata(
            "26.2",
            "0.1.0-SNAPSHOT",
            "0".repeat(40),
            Set.of(),
            new GlyphFont().metrics(),
            2,
            caps,
            RendererProtocol.SCHEMA_SHA256);
    assertEquals(2, modern.protocolVersion());
    assertTrue(modern.supports("motion-tracks"));
  }
}
