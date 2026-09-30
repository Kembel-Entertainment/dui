package gg.kembel.dui.core;

/** A client-rendered, one-shot transition. World ticks are supplied by the controller. */
public record ItemTransition(
    Kind kind, long startedAt, int durationTicks, int distance, boolean motion) {
  public enum Kind {
    POP,
    BOUNCE,
    LIFT,
    SLIDE
  }

  public ItemTransition {
    if (kind == null
        || startedAt < 0
        || durationTicks < 1
        || durationTicks > 127
        || distance < (kind == Kind.SLIDE ? -127 : 0)
        || distance > 127)
      throw new IllegalArgumentException(
          "Item transition requires a nonnegative world tick, duration 1..127; distance 0..127"
              + " (slide: -127..127)");
  }
}
