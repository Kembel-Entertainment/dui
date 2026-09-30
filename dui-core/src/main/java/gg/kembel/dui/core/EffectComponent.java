package gg.kembel.dui.core;

/** Turns ordinary layout nodes into typed, bounded shader parameters. */
final class EffectComponent {
  private EffectComponent() {}

  static boolean supports(String tag) {
    return java.util.Set.of("reel", "lever", "particles", "lights").contains(tag);
  }

  private static int number(MenuTemplate.Node n, String key, int fallback, int min, int max) {
    int value = n.n(key, fallback);
    if (value < min || value > max)
      throw new IllegalArgumentException(key + " must be " + min + ".." + max);
    return value;
  }

  static void draw(Canvas c, MenuTemplate.Node n, int x, int y, int w, int h) {
    String id = n.s("id", "");
    ShaderEffect.Kind kind;
    int a, b;
    switch (n.type()) {
      case "reel" -> {
        if (!n.s("symbols", "arcade").equals("arcade"))
          throw new IllegalArgumentException("Unknown reel symbol set");
        int sequence = number(n, "sequence", 0, 0, 7);
        int value = number(n, "value", 0, 0, 5),
            previous = number(n, "previous", value, 0, 5),
            turns = number(n, "turns", 18 + sequence * 6, 6, 63);
        int duration = number(n, "duration", 45 + sequence * 11, 1, 127),
            size = number(n, "symbol-size", Math.max(1, (int) Math.min(w * .39, h * .42)), 1, 127);
        kind = ShaderEffect.Kind.REEL;
        a = value | (previous << 3) | (turns << 6);
        b = duration | (size << 7);
      }
      case "lever" -> {
        kind = ShaderEffect.Kind.LEVER;
        a = number(n, "duration", 18, 1, 127);
        b = 0;
      }
      case "particles" -> {
        if (!java.util.Set.of("coins", "confetti").contains(n.s("effect", "coins")))
          throw new IllegalArgumentException("Unknown particle effect");
        int count = number(n, "count", 24, 0, 63),
            ox = number(n, "origin-x", w / 2, 0, w - 1),
            oy = number(n, "origin-y", h - 1, 0, h - 1),
            delay = number(n, "delay", 70, 0, 126);
        if (delay % 2 != 0)
          throw new IllegalArgumentException("Particle delay uses even world ticks");
        kind =
            n.s("effect", "coins").equals("confetti")
                ? ShaderEffect.Kind.CONFETTI
                : ShaderEffect.Kind.COINS;
        a = ox | (count << 9);
        b = oy | ((delay / 2) << 9);
      }
      case "lights" -> {
        kind = ShaderEffect.Kind.LIGHTS;
        a = number(n, "count", 12, 1, 32);
        b = number(n, "radius", Math.max(1, h / 3), 1, 15);
      }
      default -> throw new IllegalArgumentException("Unknown effect component");
    }
    c.effect(new ShaderEffect(id, kind, x, y, w, h, a, b));
    if (!n.s("action", "").isBlank())
      c.hit(
          new Canvas.Hit(
              id,
              n.b("locked") ? "" : n.s("action", ""),
              n.s("payload", ""),
              n.s("tooltip", ""),
              x,
              y,
              w,
              h));
  }
}
