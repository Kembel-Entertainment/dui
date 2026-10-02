package gg.kembel.dui.core.world;

/** Zero-advance pen metadata; full RGB remains available for consumer colors. */
public final class WorldHudCodec {
  private WorldHudCodec() {}

  public static int pen(WorldHud.Surface surface, int x, int y) {
    int anchor = surface.vertical().ordinal() * 3 + surface.horizontal().ordinal();
    int meta =
        ((anchor * 2 + (y < 0 ? 1 : 0)) * WorldMapProtocol.HUD_OPACITY_STEPS + surface.opacity());
    return meta * WorldMapProtocol.HUD_PEN_STRIDE + WorldMapProtocol.HUD_PEN_BIAS + x - 1;
  }

  public static String shift(int pixels) {
    if (Math.abs((long) pixels) >= (1 << 23))
      throw new IllegalArgumentException("HUD pen outside transport range");
    var result = new StringBuilder();
    int value = Math.abs(pixels);
    for (int bit = 22; bit >= 0; bit--)
      if ((value & (1 << bit)) != 0) result.append((char) ((pixels < 0 ? 0xE900 : 0xE800) + bit));
    return result.toString();
  }

  public static String font(int y) {
    if (Math.abs(y) > WorldMapProtocol.HUD_MAX_OFFSET_Y)
      throw new IllegalArgumentException("HUD vertical offset outside transport range");
    return "dui:world-hud/y_" + Math.abs(y);
  }
}
