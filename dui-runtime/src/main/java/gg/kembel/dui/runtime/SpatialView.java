package gg.kembel.dui.runtime;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;

/** Platform-owned private entities and client appearance, confined to the viewer's owner context. */
public interface SpatialView {
  boolean begin(boolean resetLook, Integer anchorSlot, Runnable abort) throws Exception;
  boolean valid();
  boolean dead();
  float yaw();
  float pitch();
  long gameTime();
  void text(String id, Component contents, float translationY, int opacity, boolean overlay);
  void removeText(String id);
  UUID replaceInput();
  void hud(Component component);
  MapVideoConnection video(int[] mapIds, Consumer<Throwable> failure) throws Exception;
  Map<String, Object> diagnostics();
  void close(boolean restoreLocation);
}
