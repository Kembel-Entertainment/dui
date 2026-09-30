package gg.kembel.dui.core;

/** Reusable shader component resolved by the layout engine. No menu or game-state dependencies. */
public record ShaderEffect(
    String id, Kind kind, int x, int y, int width, int height, int parameter0, int parameter1) {
  public static final int LIMIT = 8, CELLS = 23;

  public enum Kind {
    REEL(1),
    LEVER(2),
    COINS(3),
    LIGHTS(4),
    CONFETTI(5);
    public final int code;

    Kind(int code) {
      this.code = code;
    }
  }

  public int lifetimeTicks() {
    return switch (kind) {
      case REEL -> parameter1 & 127;
      case LEVER -> parameter0;
      case COINS -> (parameter0 >> 9) == 0 ? 0 : ((parameter1 >> 9) & 63) * 2 + 94;
      case LIGHTS -> 0;
      case CONFETTI -> ((parameter1 >> 9) & 63) * 2 + 72;
    };
  }

  public ShaderEffect {
    if (id == null
        || id.isBlank()
        || kind == null
        || x < 0
        || x > 511
        || y < 0
        || y > 511
        || width < 1
        || width > 511
        || height < 1
        || height > 511
        || parameter0 < 0
        || parameter0 > 32767
        || parameter1 < 0
        || parameter1 > 32767)
      throw new IllegalArgumentException("Invalid shader component: " + id);
  }
}
