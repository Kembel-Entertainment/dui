package gg.kembel.dui.paper;
import gg.kembel.dui.core.GlyphBinding;
import gg.kembel.dui.core.world.WorldHud;
import java.util.Map;
import net.kyori.adventure.text.Component;

final class WorldMapHud {
  private final gg.kembel.dui.runtime.WorldMapHud shared;
  WorldMapHud(Map<String, Integer> metrics, Map<String, GlyphBinding> glyphs) { shared = new gg.kembel.dui.runtime.WorldMapHud(metrics, glyphs); }
  Component render(WorldHud hud) { return shared.render(hud); }
}
