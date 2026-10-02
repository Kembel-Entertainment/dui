package gg.kembel.dui.paper;

/** Per-session presentation. Geometry and gameplay stay outside these options. */
public record WorldMapOptions(boolean reducedMotion) {
  public static WorldMapOptions animated() {
    return new WorldMapOptions(false);
  }

  public static WorldMapOptions still() {
    return new WorldMapOptions(true);
  }
}
