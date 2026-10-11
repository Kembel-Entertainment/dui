package gg.kembel.dui.runtime;

import static org.junit.jupiter.api.Assertions.*;
import gg.kembel.dui.core.video.*;
import gg.kembel.dui.core.world.WorldMapInput;
import java.util.*;
import org.junit.jupiter.api.Test;

class SpatialLifecycleTest {
  @Test void mapsWaitForPackAndReplaceInputIdentityWhenTheViewChanges() {
    var f = new RuntimeFixture(true); var inputs = new ArrayList<WorldMapInput>();
    try (var dui = f.dui) {
      var s = dui.spatial().openWorldMap(f.viewer, "fixture:map", f.frame("first"), WorldMapOptions.still(), inputs::add);
      var view = f.transport.views.get(f.viewer.id());
      assertFalse(s.isStarted()); f.loaded(f.viewer); assertTrue(s.isStarted());
      f.viewer.clock.advance(40); assertEquals(1, view.inputs);
      var old = view.target; s.update(f.frame("second")); assertEquals(2, view.inputs);
      int count = inputs.size(); dui.spatial().click(f.viewer, old, false, 99); assertEquals(count, inputs.size());
      dui.spatial().click(f.viewer, view.target, false, 99); assertEquals(count + 1, inputs.size());
      dui.spatial().click(f.viewer, view.target, false, 99); assertEquals(count + 1, inputs.size());
      f.open(f.viewer, c -> {}); assertFalse(s.isActive()); assertTrue(view.closed); assertTrue(view.restored); assertEquals(0, f.viewer.clock.pending());
    }
  }
  @Test void quitAndShutdownCleanPrivateStateAndRestoreOnlyAtShutdown() {
    var f = new RuntimeFixture(true); f.loaded(f.viewer);
    var map = f.dui.spatial().openWorldMap(f.viewer, "fixture:map", f.frame("first"), WorldMapOptions.animated(), e -> {});
    var first = f.transport.views.get(f.viewer.id()); f.dui.quit(f.viewer);
    assertFalse(map.isActive()); assertTrue(first.closed); assertFalse(first.restored);
    f.loaded(f.viewer); f.dui.spatial().openWorldMap(f.viewer, "fixture:map", f.frame("second"), WorldMapOptions.still(), e -> {});
    var second = f.transport.views.get(f.viewer.id()); f.dui.close(); assertTrue(second.closed); assertTrue(second.restored);
  }
  @Test void rejectedPackClosesPendingVideoAndReleasesInputs() {
    var f = new RuntimeFixture(true); var releases = new ArrayList<SurfaceInput>();
    try (var dui = f.dui) {
      var spec = new VideoSurfaceSpec(16, 16, PixelFormat.RGB888, 30, VideoSurfaceSpec.Viewport.FULL, new VideoSurfaceSpec.Budget(4, 100_000), 0, true);
      var s = dui.spatial().openVideoSurface(f.viewer, spec, VideoSurfaceOptions.DEFAULT, releases::add);
      assertTrue(s.submit(new VideoFrame(16, 16, PixelFormat.RGB888, 0, new int[256])));
      dui.resourcePack(f.viewer, f.pack.id(), "FAILED_RELOAD");
      assertFalse(s.isActive()); assertFalse(s.submit(new VideoFrame(16, 16, PixelFormat.RGB888, 1, new int[256])));
      assertEquals(1, releases.size()); assertFalse(releases.getFirst().forward()); assertEquals(0, f.viewer.clock.pending());
    }
  }
}
