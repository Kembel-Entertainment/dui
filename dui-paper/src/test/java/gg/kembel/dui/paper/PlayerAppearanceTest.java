package gg.kembel.dui.paper;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PlayerAppearanceTest {
  @Test
  void offlineAndExplicitSkinResourcesUseTheSameGpuProtocol() {
    var empty = Arrays.<org.bukkit.inventory.ItemStack>asList(null, null, null, null);
    var fallback = new PlayerAppearance(null, empty);
    assertTrue(fallback.fallback());
    assertFalse(fallback.slim());
    var alex = PlayerAppearance.skinResource("minecraft:entity/player/slim/alex", true, empty);
    assertFalse(alex.fallback());
    assertTrue(alex.slim());
    var c = new Canvas(320, 180, RenderEnvironment.plain(new GlyphFont(java.util.Map.of("?", 6))));
    c.playerModel("preview", "alex", 0, 0, 72, 108, 7, true, false);
    assertNotNull(NativePlayerModels.band(c.playerModels.getFirst(), alex, 11, false));
    assertThrows(IllegalArgumentException.class, () -> new PlayerAppearance(null, List.of()));
  }

  @Test
  void missingAppearancesAreRejectedBeforeTransport() {
    var canvas =
        new Canvas(320, 180, RenderEnvironment.plain(new GlyphFont(java.util.Map.of("?", 6))));
    canvas.playerModel("hero", "viewer", 0, 0, 72, 108, 0, true, false);
    assertThrows(IllegalArgumentException.class, () -> ViewModel.data(Map.of()).validate(canvas));
    var view =
        new ViewModel(
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of("viewer", new PlayerAppearance(null, Arrays.asList(null, null, null, null))));
    assertDoesNotThrow(() -> view.validate(canvas));
  }

  @Test
  void packCapabilityIsRequired() {
    var old =
        TestMetadata.create("26.2", "0.2.0-SNAPSHOT", "a".repeat(40), Set.of(), Map.of("?", 6));
    var c = new Canvas(320, 180, RenderEnvironment.plain(new GlyphFont(java.util.Map.of("?", 6))));
    c.playerModel("hero", "viewer", 0, 0, 72, 108, 0, true, false);
    assertThrows(IllegalArgumentException.class, () -> old.validate(c));
  }
}
