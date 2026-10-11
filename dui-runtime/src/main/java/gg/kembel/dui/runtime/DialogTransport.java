package gg.kembel.dui.runtime;

import net.kyori.adventure.key.Key;

/** Platform operations. All calls happen in the viewer's execution context. */
public interface DialogTransport {
  NativeItem item(Key material);
  void offerPack(UiViewer viewer, PackDescriptor pack);
  void show(UiViewer viewer, DialogPresentation presentation);
  void close(UiViewer viewer);
  default void beforeOpen(UiViewer viewer) {}
  default SpatialView spatial(UiViewer viewer) { throw new UnsupportedOperationException("This adapter has no spatial transport"); }
}
