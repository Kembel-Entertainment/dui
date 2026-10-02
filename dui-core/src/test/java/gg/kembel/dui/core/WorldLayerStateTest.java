package gg.kembel.dui.core;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.world.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class WorldLayerStateTest {
  @Test
  void geometryWordSurvivesFloatAndRgbAtBoundaries() {
    for (int x : new int[] {-2048, 0, 2047})
      for (int y : new int[] {-2048, 0, 2047})
        for (int width : new int[] {1, 512})
          for (int height : new int[] {1, 512})
            for (int zoom : new int[] {0, 31})
              for (boolean reduced : new boolean[] {false, true}) {
                var state = new WorldLayerState(x, y, width, height, 1);
                int geometry = state.geometry(zoom, reduced);
                assertEquals(geometry, (int) (float) geometry);
                assertEquals(state, WorldLayerState.decode(state.color(), geometry, 255));
              }
  }

  @Test
  void hitTestingFollowsMovedAndHiddenGeometry() {
    var definition =
        new WorldMapDefinition(
            "test:map",
            600,
            400,
            8,
            200,
            15,
            0,
            31,
            List.of(
                new WorldMapDefinition.Layer(
                    "target",
                    "image",
                    100,
                    100,
                    50,
                    50,
                    WorldMapDefinition.Space.MAP,
                    .9,
                    1,
                    1,
                    0)),
            new WorldMapDefinition.Opening(18, 1, 1, 1, Motion.Easing.LINEAR));
    var hud = WorldHud.EMPTY;
    var frame =
        new WorldMapFrame(
            "map",
            List.of("target"),
            List.of(new WorldMapFrame.Region("target", "target", "pick", "", true)),
            hud,
            Map.of("target", new WorldLayerState(300, 200, 80, 40, 1)));
    frame.validate(definition);
    assertNull(WorldMapGeometry.hit(definition, frame, new WorldMapGeometry.Point(100, 100)));
    assertNotNull(WorldMapGeometry.hit(definition, frame, new WorldMapGeometry.Point(300, 200)));
    assertThrows(IllegalArgumentException.class, () -> new WorldLayerState(2048, 0, 1, 1, 1));
  }
}
