package gg.kembel.dui.paper;
import gg.kembel.dui.core.TaskScope;
import gg.kembel.dui.core.world.*;
import java.util.Map;

/** Paper facade for the shared map session. */
public final class WorldMapSession implements AutoCloseable {
  private final gg.kembel.dui.runtime.WorldMapSession session;
  WorldMapSession(gg.kembel.dui.runtime.WorldMapSession session) { this.session = session; }
  public gg.kembel.dui.runtime.WorldMapSession portable() { return session; }
  public boolean isActive() { return session.isActive(); }
  public boolean isStarted() { return session.isStarted(); }
  public long revision() { return session.revision(); }
  public int zoom() { return session.zoom(); }
  public WorldMapFrame frame() { return session.frame(); }
  public WorldMapDefinition definition() { return session.definition(); }
  public TaskScope tasks() { return session.tasks(); }
  public TaskScope viewTasks() { return session.viewTasks(); }
  public WorldMapSession onClose(Runnable callback) { session.onClose(callback); return this; }
  public WorldMapGeometry.Point cursor() { return session.cursor(); }
  public void update(WorldMapFrame frame) { session.update(frame); }
  public Map<String, Object> snapshot() { return session.snapshot(); }
  public void close() { session.close(); }
}
