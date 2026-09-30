package gg.kembel.dui.core;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;

class EffectTemplateTest {
  @Test
  void oneComponentMovesResizesAndChangesTimingThroughTemplateOnly() throws Exception {
    String source =
        """
        <dui-menu width="300" height="144" animation-start="{{tick}}" motion="{{motion}}">
          <dui-layer height="fill"><dui-reel id="example" x="24" y="27" width="54" height="63" value="{{value}}" previous="1" duration="60" /></dui-layer>
        </dui-menu>
        """;
    var data = Map.<String, Object>of("tick", 23999, "motion", true, "value", 5);
    var before = MenuTemplate.parse(source).render(data);
    var moved =
        MenuTemplate.parse(
                source
                    .replace("x=\"24\"", "x=\"150\"")
                    .replace("width=\"54\"", "width=\"81\"")
                    .replace("duration=\"60\"", "duration=\"90\""))
            .render(data);
    assertEquals(24, before.effects.getFirst().x());
    assertEquals(150, moved.effects.getFirst().x());
    assertEquals(81, moved.effects.getFirst().width());
    assertEquals(90, moved.animation.durationTicks());
    assertEquals(before.effects.getFirst().parameter0(), moved.effects.getFirst().parameter0());
    assertEquals(1, moved.items.size());
    assertEquals(23999, moved.animation.startedAt());
    assertNotEquals(ItemTransport.animationPayload(before), ItemTransport.animationPayload(moved));
  }

  @Test
  void effectsUseRowLayoutRepeatAndShareOneCarrier() throws Exception {
    String source =
        """
        <dui-menu width="300" height="144"><dui-column>
          <dui-row height="72" gap="9"><dui-repeat items="values" as="r"><dui-reel id="reel_{{r.value}}" value="{{r.value}}" /></dui-repeat></dui-row>
          <dui-row height="72"><dui-lever id="lever" action="pull" /><dui-particles id="coins" count="8" delay="90" /></dui-row>
        </dui-column></dui-menu>
        """;
    var c =
        MenuTemplate.parse(source)
            .render(Map.of("values", List.of(Map.of("value", 2), Map.of("value", 4))));
    assertEquals(4, c.effects.size());
    assertEquals(0, c.effects.get(0).x());
    assertEquals(154, c.effects.get(1).x());
    assertEquals(145, c.effects.get(0).width());
    assertEquals(1, c.items.size());
    assertEquals("pull", c.at(5, 90).action());
    assertEquals(184, c.animation.durationTicks());
  }

  @Test
  void stylesResolveInOrderAndInlineOverridesWin() throws Exception {
    String source =
        """
        <dui-menu width="150" height="36">
          <dui-style id="base" fill="#123456" border="#ABCDEF" color="#FFFFFF" bevel="2" />
          <dui-style id="accent" fill="#654321" />
          <dui-layer height="fill"><dui-button class="base accent" x="9" y="9" width="126" height="18" id="button" action="act" fill="#010203" label="Example" /></dui-layer>
        </dui-menu>
        """;
    var c = MenuTemplate.parse(source).render(Map.of());
    assertTrue(c.paints.stream().anyMatch(p -> p.color() == 0x010203));
    assertFalse(c.paints.stream().anyMatch(p -> p.color() == 0x654321));
    assertEquals("act", c.at(15, 12).action());
    assertThrows(
        IllegalArgumentException.class,
        () -> MenuTemplate.parse(source.replace("base accent", "missing")).render(Map.of()));
  }

  @Test
  void invalidGeometryCountsAndSymbolsAreRejected() throws Exception {
    String shell =
        "<dui-menu width=\"300\" height=\"144\"><dui-layer"
            + " height=\"fill\">%s</dui-layer></dui-menu>";
    for (String component :
        List.of(
            "<dui-reel id=\"r\" width=\"54\" height=\"63\" value=\"6\"/>",
            "<dui-lever id=\"l\" y=\"1\" width=\"54\" height=\"63\" action=\"pull\"/>",
            "<dui-particles id=\"p\" width=\"54\" height=\"63\" count=\"64\"/>",
            "<dui-reel id=\"r\" x=\"270\" width=\"54\" height=\"63\"/>"))
      assertThrows(
          IllegalArgumentException.class,
          () -> MenuTemplate.parse(shell.formatted(component)).render(Map.of()));
    StringBuilder many = new StringBuilder();
    for (int i = 0; i < 9; i++)
      many.append("<dui-reel id=\"r" + i + "\" width=\"54\" height=\"63\"/>");
    assertThrows(
        IllegalArgumentException.class,
        () -> MenuTemplate.parse(shell.formatted(many)).render(Map.of()));
  }

  @Test
  void allTransportFieldsRoundTripAcrossWordBoundaries() {
    for (var kind : ShaderEffect.Kind.values()) {
      var effect = new ShaderEffect("e", kind, 479, 359, 480, 360, 32767, 32767);
      BigInteger data = decode(ItemTransport.effectPayload(effect));
      assertEquals(kind.code, field(data, 0, 3));
      assertEquals(479, field(data, 3, 9));
      assertEquals(359, field(data, 12, 9));
      assertEquals(480, field(data, 21, 9));
      assertEquals(360, field(data, 30, 9));
      assertEquals(32767, field(data, 39, 15));
      assertEquals(32767, field(data, 54, 15));
    }
  }

  private static int field(BigInteger data, int offset, int bits) {
    return data.shiftRight(offset)
        .and(BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE))
        .intValue();
  }

  private static BigInteger decode(List<Integer> colors) {
    var data = BigInteger.ZERO;
    for (int i = 0; i < colors.size(); i++) {
      int c = colors.get(i),
          bits =
              ((c & 0xFF0000) != 0 ? 1 : 0)
                  | ((c & 0xFF00) != 0 ? 2 : 0)
                  | ((c & 255) != 0 ? 4 : 0);
      data = data.or(BigInteger.valueOf(bits).shiftLeft(i * 3));
    }
    return data;
  }
}
