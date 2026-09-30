package gg.kembel.dui.paper;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.util.*;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class ImageLayerTest {
  @Test
  void backgroundPaintsAndForegroundKeepTheirExplicitDrawOrder() throws Exception {
    var c = new Canvas(120, 9);
    var back = new RasterImage(1, 1, new int[] {0x112233});
    var front = new RasterImage(1, 1, new int[] {0x778899});
    c.image("back", 0, 0, 8, 9, 3, back, true);
    c.rect(0, 0, 8, 9, 0x445566);
    c.image("front", 0, 0, 8, 9, 3, front);
    var rendered =
        new CanvasRenderer().render(c, h -> net.kyori.adventure.key.Key.key("test", "hit"));
    var colors = new ArrayList<Integer>();
    collect(rendered, colors);
    assertTrue(colors.indexOf(0x112233) < colors.indexOf(0x445566));
    assertTrue(colors.indexOf(0x445566) < colors.indexOf(0x778899));
    assertFalse(new Canvas.Image("old", 0, 0, 1, 1, 1, front).background());
    String xml =
        "<dui-menu width='120' height='9'><dui-image source='a' image-layer='%s' width='120'"
            + " height='9'/></dui-menu>";
    assertTrue(
        MenuTemplate.parse(xml.formatted("background"))
            .render(Map.of(), Map.of("a", back))
            .images
            .getFirst()
            .background());
    assertThrows(
        IllegalArgumentException.class,
        () -> MenuTemplate.parse(xml.formatted("top")).render(Map.of(), Map.of("a", back)));
  }

  private static void collect(Component c, List<Integer> colors) {
    if (c.color() != null) colors.add(c.color().value());
    for (var child : c.children()) collect(child, colors);
  }
}
