package gg.kembel.dui.core;

/** Registered camera-family transport for marked native skin glyphs. */
public final class PlayerModelCodec {
  public static final String CAPABILITY = "player-model-v2";
  public static final int MARKER = RendererProtocol.PLAYER_SIGNATURE;

  private PlayerModelCodec() {}

  public static int height(int available) {
    return PlayerRenderSpec.standard()
        .viewports()
        .get(PlayerRenderSpec.standard().fit(480, available))
        .height();
  }

  public static int flags(Canvas.PlayerModel model, boolean slim, boolean motion) {
    int code = model.viewportIndex();
    return code
        | (model.facing() << 2)
        | (slim ? 32 : 0)
        | (model.outerLayer() ? 64 : 0)
        | (model.idle() && motion ? 128 : 0);
  }

  public static int color(int flags, int band) {
    if (flags < 0 || flags > 255 || band < 0 || band >= (1 << RendererProtocol.PLAYER_BAND_BITS))
      throw new IllegalArgumentException("Player model transport range");
    return MARKER | flags | (band << 8);
  }

  public static int color(int flags, int band, int renderer) {
    if (renderer < 0 || renderer >= (1 << RendererProtocol.PLAYER_RENDERER_BITS))
      throw new IllegalArgumentException("Player renderer transport");
    return color(flags, band) | (renderer << (8 + RendererProtocol.PLAYER_BAND_BITS));
  }

  public static String armorModel(String material, boolean leggings) {
    if (!java.util.Set.of(
            "leather",
            "chainmail",
            "copper",
            "iron",
            "gold",
            "diamond",
            "netherite",
            "turtle_scute")
        .contains(material))
      throw new IllegalArgumentException("Unsupported vanilla armor: " + material);
    return "dui:player/armor/" + material + (leggings ? "_leggings" : "");
  }
}
