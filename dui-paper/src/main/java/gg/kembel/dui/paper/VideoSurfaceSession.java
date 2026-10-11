package gg.kembel.dui.paper;
import gg.kembel.dui.core.video.*;
import gg.kembel.dui.core.world.WorldHud;
import java.util.function.Consumer;

/** Paper facade for the shared bounded streaming session. */
public final class VideoSurfaceSession implements AutoCloseable {
  public record Statistics(long submitted, long sent, long dropped, long bytes, long encodeNanos, long sequence) {}
  private final gg.kembel.dui.runtime.VideoSurfaceSession session;
  VideoSurfaceSession(gg.kembel.dui.runtime.VideoSurfaceSession session) { this.session = session; }
  public gg.kembel.dui.runtime.VideoSurfaceSession portable() { return session; }
  public boolean isActive() { return session.isActive(); }
  public boolean isStarted() { return session.isStarted(); }
  public VideoSurfaceSession pointerInput(Consumer<SurfacePointerInput> handler) { return pointerInput(handler, true); }
  public VideoSurfaceSession pointerInput(Consumer<SurfacePointerInput> handler, boolean wheel) { session.pointerInput(handler, wheel); return this; }
  public VideoSurfaceSpec specification() { return session.specification(); }
  public VideoSurfaceSession onClose(Runnable callback) { session.onClose(callback); return this; }
  public void hud(WorldHud presentation) { session.hud(presentation); }
  public Statistics statistics() {
    var s = session.statistics(); return new Statistics(s.submitted(), s.sent(), s.dropped(), s.bytes(), s.encodeNanos(), s.sequence());
  }
  public boolean submit(VideoFrame frame) { return session.submit(frame); }
  public void close() { session.close(); }
}
