package gg.kembel.dui.core.world;

/** Owner-thread input. Enabled is presentation information, never gameplay authorization. */
public record WorldMapInput(
    Type type,
    WorldMapGeometry.Point point,
    WorldMapFrame.Region region,
    int zoom,
    long revision,
    long nanoTime) {
  public enum Type {
    AIM,
    PRIMARY,
    SECONDARY,
    ZOOM
  }
}
