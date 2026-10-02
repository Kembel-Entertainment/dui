package gg.kembel.dui.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class ItemClipTest {
  private static Canvas render(String attrs) throws Exception {
    return TestEnvironment.parse(
            "<dui-menu width=\"320\" height=\"126\"><dui-layer height=\"fill\"><dui-item"
                + " id=\"card\" x=\"-8\" y=\"27\" width=\"72\" height=\"72\" size=\"72\" "
                + attrs
                + "/></dui-layer></dui-menu>")
        .render(Map.of());
  }

  @Test
  void aFixedViewportAllowsOffscreenCarriersAndSignedSlides() throws Exception {
    var c =
        render(
            "clip-x=\"66\" clip-y=\"27\" clip-width=\"188\" clip-height=\"72\" translate-x=\"-66\""
                + " motion-start=\"23999\"");
    var item = c.items.getFirst();
    assertEquals(-8, item.x());
    var p = ItemTransport.motionPayload(c, item, c.motions.get("card"));
    assertEquals(85, p.size());
    assertEquals(5, decode(p.subList(0, 18)) >>> 51);
    long clip = decode(p.subList(24, 37));
    assertEquals(74, (clip & 1023) - 512);
    assertEquals(0, ((clip >>> 10) & 1023) - 512);
    assertEquals(188, (clip >>> 20) & 511);
    assertEquals(72, (clip >>> 29) & 511);
    var noMotion = Motion.slide(24001, 24, 66, false);
    assertEquals(1, decode(ItemTransport.motionPayload(c, item, noMotion).subList(0, 18)) & 32767);
  }

  @Test
  void clipsDoNotChangeHitGeometryAndInvalidBoundsFail() throws Exception {
    var c = render("clip-x=\"66\" clip-y=\"27\" clip-width=\"188\" clip-height=\"72\"");
    assertTrue(c.hits.isEmpty());
    assertTrue(c.motions.isEmpty());
    assertThrows(IllegalArgumentException.class, () -> render(""));
    assertThrows(IllegalArgumentException.class, () -> render("clip-width=\"188\""));
    assertThrows(
        IllegalArgumentException.class,
        () -> render("clip-x=\"66\" clip-y=\"27\" clip-width=\"280\" clip-height=\"72\""));
    assertThrows(IllegalArgumentException.class, () -> Motion.slide(0, 24, -257, true));
    assertThrows(
        IllegalArgumentException.class,
        () -> c.item("oops", -500, 0, 72, new ItemClip(66, 27, 188, 72)));
    assertThrows(IllegalArgumentException.class, () -> c.item("card", 0, 0, 72));
  }

  private static long decode(List<Integer> p) {
    long result = 0;
    for (int i = 0; i < p.size(); i++) {
      int c = p.get(i);
      result |=
          (long)
                  (((c & 0xFF0000) != 0 ? 1 : 0)
                      | ((c & 0xFF00) != 0 ? 2 : 0)
                      | ((c & 255) != 0 ? 4 : 0))
              << (i * 3);
    }
    return result;
  }
}
