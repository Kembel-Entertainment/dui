package gg.kembel.dui.pack;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class PackContributionTest {
  @Test
  void ownersAndCollisionsAreCheckedBeforePackEmission() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PackContribution(
                "example", Map.of("assets/minecraft/fonts/default.json", new byte[0]), List.of()));
    var effect =
        new PackContribution.Effect(
            "example:pulse",
            8,
            "examplePulse",
            "vec4 examplePulse(vec2 q,vec2 size,int a,int b,float t,bool live){return vec4(1);}");
    var p =
        new PackContribution(
            "example", Map.of("assets/example/font/text.json", new byte[0]), List.of(effect));
    assertEquals(1, PackContribution.merge(List.of(p), new TreeMap<>()).size());
    assertThrows(
        IllegalArgumentException.class,
        () -> PackContribution.merge(List.of(p, p), new TreeMap<>()));
  }
}
