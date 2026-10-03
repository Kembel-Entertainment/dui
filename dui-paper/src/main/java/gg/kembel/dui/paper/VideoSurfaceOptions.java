package gg.kembel.dui.paper;

/** Minecraft-specific controls, independent of the application using the screen. */
public record VideoSurfaceOptions(int anchorSlot, boolean closeOnSneak) {
  public static final VideoSurfaceOptions DEFAULT = new VideoSurfaceOptions(8, true);
  public VideoSurfaceOptions { if (anchorSlot < 0 || anchorSlot > 8) throw new IllegalArgumentException("Hotbar slot"); }
}
