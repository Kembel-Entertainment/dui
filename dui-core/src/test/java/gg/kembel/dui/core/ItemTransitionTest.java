package gg.kembel.dui.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class ItemTransitionTest {
  private static Canvas render(String attrs, boolean motion) throws Exception {
    return MenuTemplate.parse(
            "<dui-menu width=\"300\" height=\"144\" motion=\""
                + motion
                + "\"><dui-layer height=\"fill\"><dui-item id=\"gift\" x=\"18\" y=\"27\""
                + " width=\"72\" height=\"72\" size=\"72\" "
                + attrs
                + " /></dui-layer></dui-menu>")
        .render(Map.of());
  }

  @Test
  void templateParametersAndTransportRoundTrip() throws Exception {
    for (var kind : ItemTransition.Kind.values()) {
      var c =
          render(
              "transition=\""
                  + kind.name().toLowerCase(Locale.ROOT)
                  + "\" transition-start=\"23999\" transition-duration=\"127\""
                  + " transition-distance=\"127\"",
              false);
      var t = c.transitions.get("gift");
      assertEquals(kind, t.kind());
      assertFalse(t.motion());
      var payload = ItemTransport.transitionPayload(c, c.items.getFirst(), t);
      assertEquals(24, payload.size());
      long header = decode(payload.subList(0, 18)), params = decode(payload.subList(18, 24));
      assertEquals(2, header >>> 51);
      assertEquals(23999, header & 32767);
      assertEquals(300, (header >>> 15) & 511);
      assertEquals(144, (header >>> 24) & 511);
      assertEquals(127, params & 127);
      assertEquals(kind.ordinal(), (params >>> 7) & 3);
      assertEquals(127, (params >>> 9) & 127);
      assertEquals(0, (params >>> 16) & 1);
    }
    assertTrue(
        render("transition=\"pop\" transition-start=\"24001\"", true)
            .transitions
            .get("gift")
            .motion());
    assertTrue(render("", true).transitions.isEmpty());
  }

  @Test
  void invalidTimingNamesAndConflictingCarriersFailClearly() {
    for (String attrs :
        List.of(
            "transition=\"pop\"",
            "transition=\"spin\" transition-start=\"0\"",
            "transition=\"lift\" transition-start=\"0\" transition-duration=\"0\"",
            "transition=\"pop\" transition-start=\"0\" transition-distance=\"128\"",
            "transition=\"pop\" transition-start=\"0\" burst-start=\"0\""))
      assertThrows(IllegalArgumentException.class, () -> render(attrs, true));
  }

  @Test
  void confettiIsAnIndependentBoundedEffect() throws Exception {
    var c =
        MenuTemplate.parse(
                "<dui-menu width=\"300\" height=\"144\" animation-start=\"40\""
                    + " motion=\"false\"><dui-layer height=\"fill\"><dui-particles id=\"party\""
                    + " effect=\"confetti\" width=\"300\" height=\"144\" count=\"52\" delay=\"24\""
                    + " origin-x=\"70\" origin-y=\"80\"/></dui-layer></dui-menu>")
            .render(Map.of());
    assertEquals(ShaderEffect.Kind.CONFETTI, c.effects.getFirst().kind());
    assertEquals(96, c.animation.durationTicks());
    assertFalse(c.animation.motion());
    assertEquals(1, c.items.size());
  }

  private static long decode(List<Integer> colors) {
    long data = 0;
    for (int i = 0; i < colors.size(); i++) {
      int c = colors.get(i),
          bits =
              ((c & 0xFF0000) != 0 ? 1 : 0)
                  | ((c & 0xFF00) != 0 ? 2 : 0)
                  | ((c & 255) != 0 ? 4 : 0);
      data |= (long) bits << (i * 3);
    }
    return data;
  }
}
