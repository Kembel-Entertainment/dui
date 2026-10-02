package gg.kembel.dui.core;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.world.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class WorldMapTest {
  private WorldMapDefinition map() {
    return new WorldMapDefinition(
        "example:small",
        600,
        300,
        5,
        100,
        10,
        3,
        12,
        List.of(
            new WorldMapDefinition.Layer(
                "a", "a", 200, 100, 60, 40, WorldMapDefinition.Space.MAP, .9, 1, 1, 0),
            new WorldMapDefinition.Layer(
                "b", "b", 210, 100, 60, 40, WorldMapDefinition.Space.MAP, .9, 1, 1, 0)),
        new WorldMapDefinition.Opening(12, .9, 0, 1, gg.kembel.dui.core.Motion.Easing.LINEAR));
  }

  @Test
  void projectionUsesDefinitionAndWrapsAcrossNorth() {
    var d = map();
    var point = WorldMapGeometry.cursor(d, -173, -10, 175);
    assertEquals(new WorldMapGeometry.Point(360, 100), point);
    assertEquals(
        new WorldMapGeometry.Point(800, 400),
        WorldMapGeometry.screen(d, point, point, 3, 1600, 800));
    assertEquals(-1, WorldMapGeometry.slotDelta(0, 8));
    assertEquals(1, WorldMapGeometry.slotDelta(8, 0));
    assertEquals(-4, WorldMapGeometry.slotDelta(0, 5));
  }

  @Test
  void disabledTargetsRemainHoverableAndTopLayerWins() {
    var d = map();
    var a = new WorldMapFrame.Region("a", "a", "pick", "a", true);
    var b = new WorldMapFrame.Region("b", "b", "pick", "b", false);
    var frame = new WorldMapFrame("overview", List.of("a", "b"), List.of(b, a), WorldHud.EMPTY);
    frame.validate(d);
    assertEquals(b, WorldMapGeometry.hit(d, frame, new WorldMapGeometry.Point(200, 100)));
    assertNull(WorldMapGeometry.hit(d, frame, new WorldMapGeometry.Point(300, 100)));
    assertThrows(
        IllegalArgumentException.class,
        () -> new WorldMapFrame("o", List.of("a"), List.of(b), frame.hud()).validate(d));
  }

  @Test
  void viewChangesRejectOldEntityAndDuplicateClicks() {
    var gate = new WorldMapClickGate();
    var first = UUID.randomUUID();
    var next = UUID.randomUUID();
    gate.replace(first);
    assertTrue(gate.accept(first, 5));
    assertFalse(gate.accept(first, 5));
    gate.replace(next);
    assertFalse(gate.accept(first, 6));
    assertTrue(gate.accept(next, 6));
  }

  @Test
  void metadataRejectsGeometryDriftAndProtocolDrift() {
    var d = map();
    var glyphs =
        Map.of(
            "a",
            new WorldMapMetadata.Glyph(0xE000, 63),
            "b",
            new WorldMapMetadata.Glyph(0xE001, 63));
    assertDoesNotThrow(
        () ->
            new WorldMapMetadata(
                d, "example:map", glyphs, d.geometryHash(), WorldMapProtocol.SHA256));
    assertThrows(
        IllegalArgumentException.class,
        () -> new WorldMapMetadata(d, "example:map", glyphs, "wrong", WorldMapProtocol.SHA256));
    assertThrows(
        IllegalArgumentException.class,
        () -> new WorldMapMetadata(d, "example:map", glyphs, d.geometryHash(), "wrong"));
  }
}
