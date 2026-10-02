package gg.kembel.dui.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class GlyphRegistryTest {
  @Test
  void registriesAreStableBoundedAndRejectMissingAssets() {
    var first = new GlyphSpec("acme:arrow", 9, 9);
    var picture = new GlyphSpec("other:picture", 18, 18, false);
    var bindings = GlyphRegistry.bind(List.of(picture, first));
    assertEquals(bindings, GlyphRegistry.bind(List.of(first, picture)));
    assertEquals(0xEB00, bindings.get(first.id()).codePoint());
    assertThrows(UnsupportedOperationException.class, () -> bindings.clear());
    assertThrows(IllegalArgumentException.class, () -> GlyphRegistry.bind(List.of(first, first)));
    assertThrows(
        IllegalArgumentException.class, () -> GlyphRegistry.require(bindings, "acme:missing"));
    assertThrows(IllegalArgumentException.class, () -> new GlyphSpec("acme:tall", 9, 19));
    assertThrows(
        IllegalArgumentException.class,
        () -> GlyphRegistry.validate(Map.of("acme:wrong", bindings.get(first.id()))));
  }

  @Test
  void colorPicturesKeepTheirOriginalPixelsAndAllVerticalBands() {
    var icon = new GlyphSpec("acme:picture", 18, 18, false);
    var bindings = GlyphRegistry.bind(List.of(icon));
    var canvas =
        new Canvas(
            180,
            90,
            new RenderEnvironment(
                new GlyphFont(Map.of("?", 6)),
                ThemeTokens.EMPTY,
                WidgetSkinRegistry.EMPTY,
                bindings,
                (data, tokens) -> color -> 0x123456));
    canvas.icon(9, 9, icon.id(), 0xAA0000);
    var paint = canvas.paints.getFirst();
    assertEquals(0xFFFFFF, paint.color());
    assertEquals(18, paint.height());
    assertEquals(19, bindings.get(icon.id()).advance());
    assertEquals(256, bindings.get(icon.id()).character(1) - bindings.get(icon.id()).character(0));
    assertEquals(512, bindings.get(icon.id()).character(2) - bindings.get(icon.id()).character(0));
  }
}
