package gg.kembel.dui.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class PlayerModelTest {
  @Test
  void containsAndQuantizesWithoutStretch() {
    var c = new Canvas(320, 180, RenderEnvironment.plain(TestEnvironment.font()));
    c.playerModel("hero", "viewer", 10, 9, 100, 135, 3, true, true);
    var p = c.playerModels.getFirst();
    assertEquals(72, p.width());
    assertEquals(108, p.height());
    assertEquals(24, p.x());
    assertEquals(18, p.y());
    assertEquals(1 | (3 << 2) | 32 | 64 | 128, PlayerModelCodec.flags(p, true, true));
    assertEquals(PlayerModelCodec.MARKER | 1 | 64 | (11 << 8), PlayerModelCodec.color(65, 11));
  }

  @Test
  void popupsHideTheWholeSkinAndItsArmorViewport() {
    var c = new Canvas(320, 180, RenderEnvironment.plain(TestEnvironment.font()));
    c.playerModel("before", "viewer", 0, 0, 72, 108, 0, true, false);
    c.cover("dropdown", 50, 9, 100, 27);
    c.playerModel("after", "viewer", 180, 0, 48, 72, 7, true, false);
    assertEquals(
        List.of("after"),
        c.renderPlan().playerModels.stream().map(Canvas.PlayerModel::id).toList());
    assertEquals(2, c.playerModels.size());
    assertEquals(2, RenderReport.of(c).playerModels());
  }

  @Test
  void popupKeepsTheCanvasBackground() {
    var c = new Canvas(320, 180, RenderEnvironment.plain(TestEnvironment.font()));
    c.image("background", 0, 0, 320, 180, 8, new RasterImage(1, 1, new int[] {0x17232A}), true);
    c.cover("picker", 100, 9, 100, 27);
    assertEquals(1, c.renderPlan().images.size());
  }

  @Test
  void invalidFacingGridBoundsAndDuplicateIdsFailEarly() {
    var c = new Canvas(320, 180, RenderEnvironment.plain(TestEnvironment.font()));
    assertThrows(
        IllegalArgumentException.class,
        () -> c.playerModel("hero", "viewer", 0, 0, 48, 72, 8, true, false));
    assertThrows(
        IllegalArgumentException.class,
        () -> c.playerModel("hero", "viewer", 0, 1, 48, 72, 0, true, false));
    assertThrows(
        IllegalArgumentException.class,
        () -> c.playerModel("hero", "viewer", 0, 0, 47, 72, 0, true, false));
    c.playerModel("hero", "viewer", 0, 0, 48, 72, 0, true, false);
    assertThrows(
        IllegalArgumentException.class,
        () -> c.playerModel("hero", "viewer", 100, 0, 48, 72, 0, true, false));
  }

  @Test
  void templatesCanDeclareRichPopupCoverage() throws Exception {
    var t =
        TestEnvironment.parse(
            "<dui-menu width=\"320\" height=\"180\"><dui-layer height=\"fill\"><dui-player-model"
                + " id=\"hero\" source=\"viewer\" width=\"72\" height=\"108\"/><dui-layer"
                + " id=\"picker\" x=\"50\" y=\"9\" width=\"100\" height=\"27\" cover=\"true\""
                + " dismiss=\"dismiss\"><dui-button id=\"choose\" action=\"choose\" width=\"fill\""
                + " height=\"27\" label=\"Pick\"/></dui-layer></dui-layer></dui-menu>");
    var c = t.render(Map.of());
    assertEquals(1, c.playerModels.size());
    assertTrue(c.renderPlan().playerModels.isEmpty());
    assertEquals("choose", c.at(55, 15).action());
    assertEquals("dismiss", c.at(300, 15).action());
  }
}
