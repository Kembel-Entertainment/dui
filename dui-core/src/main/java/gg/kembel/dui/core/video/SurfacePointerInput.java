package gg.kembel.dui.core.video;

/** Vanilla look deltas and button/slot pulses. Consumers choose pixel mapping and actions. */
public record SurfacePointerInput(Type type, double deltaYaw, double deltaPitch, int scrollSteps, long nanoTime) {
  public enum Type { LOOK, PRIMARY, SECONDARY, SCROLL }
  public SurfacePointerInput {
    if (type == null || !Double.isFinite(deltaYaw) || !Double.isFinite(deltaPitch))
      throw new IllegalArgumentException("Pointer input");
  }
}
