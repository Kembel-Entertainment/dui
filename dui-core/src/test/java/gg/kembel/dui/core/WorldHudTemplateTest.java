package gg.kembel.dui.core;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.world.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class WorldHudTemplateTest {
  private WorldHudTemplate parse(String body) {
    return WorldHudTemplate.parse("<dui-hud>" + body + "</dui-hud>", TestEnvironment.environment());
  }

  @Test
  void consumerControlsLayoutColorVisibilityAndArbitraryControlCount() {
    var template =
        parse(
            """
            <dui-surface anchor-x="right" anchor-y="bottom" offset-x="-12" offset-y="-9" opacity="{{opacity}}" visible="{{visible}}">
              <dui-menu width="180" height="99"><dui-column>
                <dui-repeat items="controls" as="control"><dui-text height="18" label="{{control.label}}" color="{{color}}"/></dui-repeat>
              </dui-column></dui-menu>
            </dui-surface>
            """);
    var controls =
        java.util.stream.IntStream.range(0, 5)
            .mapToObj(i -> Map.of("label", "Entry " + i))
            .toList();
    var input =
        Map.<String, Object>of(
            "opacity", 0.5, "visible", true, "color", "#EF5278", "controls", controls);
    var hud = template.render(input);
    var surface = hud.surfaces().getFirst();
    assertEquals(5, surface.paints().size());
    assertEquals(0xEF5278, surface.paints().getFirst().color());
    assertEquals(128, surface.opacity());
    assertEquals(-192, surface.x(0));
    assertEquals(-108, surface.y(0));
    var hidden = new HashMap<>(input);
    hidden.put("visible", false);
    assertTrue(template.render(hidden).surfaces().isEmpty());
    assertEquals(5, hud.surfaces().getFirst().paints().size());
  }

  @Test
  void runtimeImagesAndScopedConsumerComponentsUseTheExistingTemplateEngine() {
    var template =
        parse(
            """
            <dui-surface><dui-menu width="120" height="36" background="none">
              <dui-component name="acme-caption" props="label"><dui-text label="{{props.label}}" color="#CAFE42"/></dui-component>
              <dui-layer><dui-acme-caption width="100" height="18" label="{{label}}"/>
              <dui-image x="108" y="9" width="9" height="9" pixel-size="1" source="own-icon"/></dui-layer>
            </dui-menu></dui-surface>
            """);
    var hud =
        template.render(
            Map.of("label", "Custom"),
            Map.of("own-icon", new RasterImage(1, 1, new int[] {0x1278EF})),
            null);
    assertEquals("Custom", hud.surfaces().getFirst().paints().getFirst().text());
    assertEquals(1, hud.surfaces().getFirst().images().size());
    assertEquals(1, hud.surfaces().getFirst().paints().size());
    var themed =
        template.render(
            Map.of("label", "Custom"),
            Map.of("own-icon", new RasterImage(1, 1, new int[] {0x1278EF})),
            ThemeTokens.EMPTY);
    assertEquals(
        1,
        themed.surfaces().getFirst().paints().size(),
        "Transparent foreground must not acquire a theme background");
  }

  @Test
  void nativeBodiesAndDialogActionsFailExplicitlyInsteadOfSilentlyDisappearing() {
    var template =
        parse(
            "<dui-surface><dui-menu width='120' height='27'><dui-button id='b' action='bad'"
                + " label='Bad'/></dui-menu></dui-surface>");
    assertThrows(IllegalArgumentException.class, () -> template.render(Map.of()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            parse("<dui-surface opacity='NaN'><dui-menu width='120' height='9'/></dui-surface>")
                .render(Map.of()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            parse("<dui-surface offset-y='999'><dui-menu width='120' height='9'/></dui-surface>")
                .render(Map.of()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            parse(
                    "<dui-surface offset-x='2147483647'><dui-menu width='120'"
                        + " height='9'/></dui-surface>")
                .render(Map.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> parse("<dui-surface nope='true'><dui-menu/></dui-surface>"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            WorldHudTemplate.parse(
                "<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><dui-hud/>",
                TestEnvironment.environment()));
  }

  @Test
  void overlaySnapshotsDoNotRetainMutableCanvasLists() {
    var canvas = new Canvas(120, 18, TestEnvironment.environment());
    canvas.rect(0, 0, 10, 9, 0xFFFFFF);
    var surface =
        WorldHud.Surface.of(canvas, WorldHud.Horizontal.LEFT, WorldHud.Vertical.TOP, 0, 0, 1);
    canvas.paints.clear();
    assertEquals(1, surface.paints().size());
    assertThrows(UnsupportedOperationException.class, () -> surface.paints().clear());
  }

  @Test
  void transportPreservesPositionOpacityAndAnchorAtFloatPrecision() {
    for (var x : WorldHud.Horizontal.values())
      for (var y : WorldHud.Vertical.values())
        for (int opacity : List.of(1, 127, 254, 255))
          for (int sign : List.of(-1, 1)) {
            var surface = new WorldHud.Surface(x, y, 0, 0, opacity, 120, 18, List.of(), List.of());
            for (int dx : List.of(-480, 0, 480)) {
              int pen = WorldHudCodec.pen(surface, dx, sign * 27);
              float shaderOrigin = (float) (320.5 + pen + 1) - 320.5f;
              int code = (int) Math.floor(shaderOrigin / 1024.0);
              assertEquals(opacity, code & 255);
              assertEquals(y.ordinal() * 3 + x.ordinal(), (code >> 8) >> 1);
              assertEquals(sign < 0 ? 1 : 0, (code >> 8) & 1);
              assertEquals(dx, shaderOrigin - code * 1024.0 - 512.0);
            }
          }
  }
}
