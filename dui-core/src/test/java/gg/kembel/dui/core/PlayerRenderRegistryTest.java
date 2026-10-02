package gg.kembel.dui.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class PlayerRenderRegistryTest {
  private static PlayerRenderSpec spec(String id) {
    return new PlayerRenderSpec(
        id,
        List.of(new PlayerRenderSpec.Viewport(80, 135)),
        List.of(new PlayerRenderSpec.Pose(12, -9, 44, List.of(0., 0., 20., -20., 5., -5.), 3, 2)));
  }

  @Test
  void independentFamiliesFitAndUseNonOverlappingTransport() {
    var bindings = PlayerRenderBinding.bind(List.of(spec("z:pose"), spec("a:pose")));
    assertEquals(0, bindings.get("dui:player").code());
    assertEquals(1, bindings.get("a:pose").code());
    assertThrows(IllegalArgumentException.class, () -> spec("a:pose").fit(79, 135));
    var canvas =
        new Canvas(
            180,
            180,
            RenderEnvironment.plain(new GlyphFont(Map.of("?", 6))).withPlayerRenderers(bindings));
    canvas.playerModel("person", "self", 9, 9, 90, 144, "a:pose", 0, true, false);
    var model = canvas.playerModels.getFirst();
    assertEquals(80, model.width());
    assertEquals(135, model.height());
    for (int renderer = 0; renderer < 16; renderer++)
      for (int band = 0; band < 40; band++) {
        int code = PlayerModelCodec.color(255, band, renderer);
        assertEquals(
            RendererProtocol.PLAYER_SIGNATURE, code & RendererProtocol.PLAYER_SIGNATURE_MASK);
        assertEquals(renderer, (code >> 14) & 15);
        assertEquals(band, (code >> 8) & 63);
      }
    assertThrows(
        IllegalArgumentException.class,
        () -> canvas.playerModel("bad", "self", 9, 9, 90, 144, "a:pose", 1, true, false));
    assertThrows(
        IllegalArgumentException.class,
        () -> PlayerRenderBinding.bind(List.of(spec("a:pose"), spec("a:pose"))));
  }
}
