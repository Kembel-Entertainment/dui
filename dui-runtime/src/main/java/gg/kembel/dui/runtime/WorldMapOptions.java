package gg.kembel.dui.runtime;

public record WorldMapOptions(boolean reducedMotion) {
  public static WorldMapOptions animated() { return new WorldMapOptions(false); }
  public static WorldMapOptions still() { return new WorldMapOptions(true); }
}
