package gg.kembel.dui.core.video;

/** Minecraft logical inputs, not physical keyboard codes. Slot actions are edges, movement is held. */
public record SurfaceInput(Type type, boolean forward, boolean backward, boolean left,
    boolean right, boolean jump, boolean sneak, boolean sprint, int slot, long nanoTime) {
  public enum Type { STATE, SLOT }
  public static SurfaceInput released() {
    return new SurfaceInput(Type.STATE, false, false, false, false, false, false, false, -1,
        System.nanoTime());
  }
}
