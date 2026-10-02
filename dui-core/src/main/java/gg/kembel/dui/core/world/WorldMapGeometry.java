package gg.kembel.dui.core.world;

/** Server projection matching the generated GPU geometry. Coordinates are map pixels. */
public final class WorldMapGeometry {
  private WorldMapGeometry() {}

  public record Point(double x, double y) {
    public Point {
      if (!Double.isFinite(x) || !Double.isFinite(y))
        throw new IllegalArgumentException("Nonfinite point");
    }
  }

  public static double relativeYaw(double yaw, double reference) {
    return ((yaw - reference + 180) % 360 + 360) % 360 - 180;
  }

  public static Point cursor(WorldMapDefinition d, double yaw, double pitch, double reference) {
    return new Point(
        d.width() / 2.0 + relativeYaw(yaw, reference) * d.pixelsPerDegree(),
        d.height() / 2.0 + pitch * d.pixelsPerDegree());
  }

  public static Point screen(
      WorldMapDefinition d, Point point, Point cursor, int zoom, double width, double height) {
    if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid viewport");
    double scale = height / d.visibleHeight(zoom);
    return new Point(
        width / 2 + (point.x() - cursor.x()) * scale,
        height / 2 + (point.y() - cursor.y()) * scale);
  }

  public static int slotDelta(int previous, int next) {
    if (previous < 0 || previous > 8 || next < 0 || next > 8)
      throw new IllegalArgumentException("Invalid hotbar slot");
    return Math.floorMod(next - previous + 4, 9) - 4;
  }

  public static WorldMapFrame.Region hit(
      WorldMapDefinition definition, WorldMapFrame frame, Point point) {
    for (var layer : definition.layers().reversed())
      if (frame.visibleLayers().contains(layer.id())
          && (frame.layerStates().containsKey(layer.id())
              ? frame.layerStates().get(layer.id()).opacity() > 0
                  && frame.layerStates().get(layer.id()).contains(point)
              : layer.contains(point)))
        for (var region : frame.regions().reversed())
          if (region.layer().equals(layer.id())) return region;
    return null;
  }
}
