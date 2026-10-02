package gg.kembel.dui.paper;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.util.*;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import org.junit.jupiter.api.Test;

class NativeContentTest {
  @Test
  void headElementsBindToNativeProfilesAndRemainWithinTheirRows() throws Exception {
    var template =
        TestEnvironment.parse(
            "<dui-menu width='240' height='36'><dui-column><dui-head player='{{player}}'"
                + " hat='false' label='Viewer'/><dui-head"
                + " player='uuid:93c30a8c-3e31-4c22-ae48-8b769dc1236a' label='Friend'"
                + " height='18'/></dui-column></dui-menu>");
    var canvas = template.render(Map.of("player", "serkem"));
    assertEquals(2, canvas.heads.size());
    var rendered = (ObjectComponent) NativeHeads.render(canvas.heads.getFirst(), null);
    var head = (PlayerHeadObjectContents) rendered.contents();
    assertEquals("serkem", head.name());
    assertFalse(head.hat());
    var second =
        (PlayerHeadObjectContents)
            ((ObjectComponent) NativeHeads.render(canvas.heads.get(1), null)).contents();
    assertEquals(UUID.fromString("93c30a8c-3e31-4c22-ae48-8b769dc1236a"), second.id());
    assertThrows(IllegalArgumentException.class, () -> canvas.head(239, 0, "self", true));
  }

  @Test
  void skinTextureAndHatLayerUseNativeObjects() {
    var canvas =
        new Canvas(240, 18, RenderEnvironment.plain(new GlyphFont(java.util.Map.of("?", 6))));
    canvas.head(0, 0, "texture:minecraft:entity/player/slim/alex", false);
    var head =
        (PlayerHeadObjectContents)
            ((ObjectComponent) NativeHeads.render(canvas.heads.getFirst(), null)).contents();
    assertEquals("minecraft:entity/player/slim/alex", head.texture().asString());
    assertFalse(head.hat());
  }
}
