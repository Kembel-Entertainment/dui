package gg.kembel.dui.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CardFlightTest {
  private Canvas render(String attrs) throws Exception {
    return MenuTemplate.parse(
            "<dui-menu width='320' height='153'><dui-playing-card id='c' x='10' y='18' width='290'"
                + " height='90' animation='fly' "
                + attrs
                + " /></dui-menu>")
        .render(Map.of());
  }

  @Test
  void reusesCardTransportAndHasDelayedLifetime() throws Exception {
    var c = render("value='-1' face-down='true' duration='28' delay='24' card-height='30'");
    var e = c.effects.getFirst();
    assertEquals(ShaderEffect.Kind.PLAYING_CARD, e.kind());
    assertEquals(3, e.parameter0() >> 13);
    assertEquals(63, e.parameter0() & 63);
    assertEquals(30, (e.parameter1() >> 7) & 63);
    assertEquals(52, e.lifetimeTicks());
    assertEquals(1, c.items.size());
  }

  @Test
  void rejectsInvalidFaceSizeAndOddDelay() {
    assertThrows(IllegalArgumentException.class, () -> render("card-height='64'"));
    assertThrows(IllegalArgumentException.class, () -> render("delay='3'"));
    assertThrows(IllegalArgumentException.class, () -> render("card-height='12'"));
  }
}
