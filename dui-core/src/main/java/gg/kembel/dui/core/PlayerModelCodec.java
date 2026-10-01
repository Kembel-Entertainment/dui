package gg.kembel.dui.core;

/** Version-one transport for the marked native skin glyph. Independent of item protocol v2. */
public final class PlayerModelCodec {
  public static final String CAPABILITY = "player-model-v1";
  public static final int MARKER = 0xE7A000;
  private static final int[] HEIGHTS = {72, 108, 162, 216};

  private PlayerModelCodec() {}

  public static int height(int available) {
    for (int i = HEIGHTS.length - 1; i >= 0; i--) if (HEIGHTS[i] <= available) return HEIGHTS[i];
    throw new IllegalArgumentException("Player model needs at least 48x72 pixels");
  }

  public static int flags(Canvas.PlayerModel model, boolean slim, boolean motion) {
    int code = -1;
    for (int i = 0; i < HEIGHTS.length; i++) if (HEIGHTS[i] == model.height()) code = i;
    if (code < 0) throw new IllegalArgumentException("Unsupported player model height");
    return code
        | (model.facing() << 2)
        | (slim ? 32 : 0)
        | (model.outerLayer() ? 64 : 0)
        | (model.idle() && motion ? 128 : 0);
  }

  public static int color(int flags, int band) {
    if (flags < 0 || flags > 255 || band < 0 || band >= 24)
      throw new IllegalArgumentException("Player model transport range");
    return MARKER | flags | (band << 8);
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
