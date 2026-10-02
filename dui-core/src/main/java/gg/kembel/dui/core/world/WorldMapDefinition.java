package gg.kembel.dui.core.world;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Immutable build-time geometry. Runtime permissions and menu flow belong to the consumer. */
public record WorldMapDefinition(
    String id,
    int width,
    int height,
    double pixelsPerDegree,
    double visibleMin,
    double visibleStep,
    int defaultZoom,
    int maxZoom,
    List<Layer> layers,
    Opening opening) {
  public enum Space {
    MAP,
    SCREEN
  }

  public record Opening(
      int ticks,
      double scaleFrom,
      double opacityFrom,
      double opacityTo,
      gg.kembel.dui.core.Motion.Easing easing) {
    public Opening {
      if (ticks < 1
          || ticks >= WorldMapProtocol.OPENING_MODULO
          || !Double.isFinite(scaleFrom)
          || scaleFrom <= 0
          || !unit(opacityFrom)
          || !unit(opacityTo)
          || easing == null) throw new IllegalArgumentException("Opening parameters");
    }
  }

  private static boolean unit(double v) {
    return Double.isFinite(v) && v >= 0 && v <= 1;
  }

  public record Layer(
      String id,
      String image,
      double x,
      double y,
      double width,
      double height,
      Space space,
      double depth,
      double opacityMin,
      double opacityMax,
      double pulseRadiansPerTick) {
    public Layer {
      if (id == null
          || !id.matches("[a-z][a-z0-9_/-]*")
          || image == null
          || !image.matches("[a-z][a-z0-9_/-]*")
          || space == null
          || !Double.isFinite(depth)
          || depth < -1
          || depth > 1
          || !unit(opacityMin)
          || !unit(opacityMax)
          || opacityMin > opacityMax
          || !Double.isFinite(pulseRadiansPerTick)
          || pulseRadiansPerTick < 0
          || !Double.isFinite(x)
          || !Double.isFinite(y)
          || !Double.isFinite(width)
          || !Double.isFinite(height)
          || width <= 0
          || height <= 0) throw new IllegalArgumentException("Invalid world-map layer");
    }

    public boolean contains(WorldMapGeometry.Point p) {
      return Math.abs(p.x() - x) <= width / 2 && Math.abs(p.y() - y) <= height / 2;
    }
  }

  public WorldMapDefinition {
    layers = List.copyOf(layers);
    Objects.requireNonNull(opening);
    if (id == null
        || !id.matches("[a-z][a-z0-9_-]*:[a-z][a-z0-9_/-]*")
        || width <= 0
        || height <= 0
        || !Double.isFinite(pixelsPerDegree)
        || pixelsPerDegree <= 0
        || !Double.isFinite(visibleMin)
        || visibleMin <= 0
        || !Double.isFinite(visibleStep)
        || visibleStep < 0
        || maxZoom < 0
        || maxZoom > WorldMapProtocol.MAX_ZOOM
        || defaultZoom < 0
        || defaultZoom > maxZoom
        || layers.isEmpty()
        || layers.size() > WorldMapProtocol.MAX_LAYERS
        || layers.stream().map(Layer::id).distinct().count() != layers.size())
      throw new IllegalArgumentException("Invalid world-map definition");
  }

  public Layer layer(String id) {
    return layers.stream()
        .filter(l -> l.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown map layer: " + id));
  }

  public double visibleHeight(int zoom) {
    if (zoom < 0 || zoom > maxZoom) throw new IllegalArgumentException("Zoom outside map range");
    return visibleMin + visibleStep * zoom;
  }

  public String geometryHash() {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(new Gson().toJson(this).getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }
}
