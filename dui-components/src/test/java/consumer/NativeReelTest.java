package consumer;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.components.*;
import gg.kembel.dui.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class NativeReelTest {
  @Test
  void arbitraryModelResourcesUseBoundedMotionAndClips() {
    var c = new Canvas(300, 180);
    var slots =
        NativeReel.draw(
            c,
            "inventory",
            List.of("example:apple", "minecraft:diamond", "minecraft:grass_block", "example:head"),
            2,
            30,
            27,
            54,
            90,
            30,
            100,
            24,
            30,
            true);
    assertFalse(slots.isEmpty());
    assertEquals(slots.size(), c.items.size());
    assertEquals(slots.size(), c.motions.size());
    assertEquals(slots.size(), c.clips.size());
    assertTrue(slots.stream().anyMatch(s -> s.resourceKey().equals("minecraft:grass_block")));
  }
}
