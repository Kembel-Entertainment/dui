package gg.kembel.dui.core;

/** Reusable shader component resolved by the layout engine. No menu or game-state dependencies. */
public record ShaderEffect(
    String id, Type kind, int x, int y, int width, int height, int parameter0, int parameter1) {
  public static final int LIMIT = 8, CELLS = 23;

  public sealed interface Type permits Kind, Extension {
    int code();
  }

  public record Extension(String id, int code, int lifetime) implements Type {
    public Extension {
      if (id == null
          || !id.matches("[a-z][a-z0-9_-]*:[a-z][a-z0-9_/-]*")
          || code < 8
          || code > 15
          || lifetime < 0
          || lifetime > 511) throw new IllegalArgumentException("Effect extension declaration");
    }
  }

  public enum Kind implements Type {
    WHEEL(0),
    REEL(1),
    LEVER(2),
    COINS(3),
    LIGHTS(4),
    CONFETTI(5),
    PLAYING_CARD(6),
    CHIP_STACK(7);
    public final int code;

    public int code() {
      return code;
    }

    Kind(int code) {
      this.code = code;
    }
  }

  public int lifetimeTicks() {
    if (kind instanceof Extension e) return e.lifetime();
    return switch ((Kind) kind) {
      case WHEEL -> (parameter1 & 512) == 0 ? 0 : parameter1 & 511;
      case REEL -> parameter1 & 127;
      case LEVER -> parameter0;
      case COINS -> (parameter0 >> 9) == 0 ? 0 : ((parameter1 >> 9) & 63) * 2 + 94;
      case LIGHTS -> 0;
      case PLAYING_CARD ->
          (parameter0 >> 13) == 0 ? 0 : ((parameter0 >> 8) & 31) * 2 + (parameter1 & 127);
      case CHIP_STACK ->
          ((parameter1 >> 7) & 3) == 0 || (parameter0 & 31) == 0
              ? 0
              : ((parameter1 >> 9) & 63) * 2
                  + (parameter1 & 127)
                  + (int) Math.ceil((Math.min(parameter0 & 31, 7) - 1) * .7);
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
