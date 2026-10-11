package gg.kembel.dui.runtime;

import gg.kembel.dui.core.*;
import gg.kembel.dui.core.world.*;
import java.util.*;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

/** One portable private map presentation. All mutations run in the viewer's owner context. */
public final class WorldMapSession implements AutoCloseable {
  final SpatialRuntime runtime;
  final UiViewer viewer;
  private final SpatialView view;
  private final WorldMapMetadata metadata;
  private final WorldMapOptions options;
  private final Consumer<WorldMapInput> handler;
  private final TaskScope tasks;
  private TaskScope viewTasks;
  private WorldMapFrame frame;
  private Runnable closed = () -> {};
  private boolean active = true, started;
  private float reference;
  private int zoom;
  private long age, revision;
  private final WorldMapClickGate clicks = new WorldMapClickGate();
  private final WorldMapHud hud;
  private gg.kembel.dui.core.UiScheduler.Cancellation ticker = () -> {};
  private final Set<String> dynamic = new HashSet<>();
  WorldMapSession(SpatialRuntime runtime, UiViewer viewer, SpatialView view, WorldMapMetadata metadata,
      WorldMapFrame frame, WorldMapOptions options, Consumer<WorldMapInput> handler) {
    this.runtime = runtime; this.viewer = viewer; this.view = view; this.metadata = metadata; this.frame = frame; this.options = options; this.handler = handler;
    zoom = definition().defaultZoom();
    tasks = new TaskScope(viewer.scheduler(), () -> active && viewer.online(), viewer::checkThread);
    viewTasks = new TaskScope(viewer.scheduler(), () -> active && viewer.online(), viewer::checkThread);
    hud = new WorldMapHud(runtime.owner.metadata().worldMapLegendMetrics(), runtime.owner.metadata().glyphs());
  }
  public boolean isActive() { return active; }
  public boolean isStarted() { return started; }
  public long revision() { return revision; }
  public int zoom() { return zoom; }
  public WorldMapFrame frame() { return frame; }
  public WorldMapDefinition definition() { return metadata.definition(); }
  private void check() { runtime.owner.check(viewer); if (!active) throw new IllegalStateException("World-map session is closed"); }
  public TaskScope tasks() { check(); return tasks; }
  public TaskScope viewTasks() { check(); return viewTasks; }
  public WorldMapSession onClose(Runnable callback) { check(); closed = Objects.requireNonNull(callback); return this; }
  public WorldMapGeometry.Point cursor() { check(); return WorldMapGeometry.cursor(definition(), view.yaw(), view.pitch(), reference); }
  public void update(WorldMapFrame next) {
    check(); next.validate(definition()); if (next.equals(frame)) return;
    boolean viewChanged = !frame.viewId().equals(next.viewId());
    boolean inputsChanged = viewChanged || !frame.regions().equals(next.regions()) || !inputGeometry(frame).equals(inputGeometry(next));
    if (viewChanged) { viewTasks.close(); viewTasks = new TaskScope(viewer.scheduler(), () -> active && viewer.online(), viewer::checkThread); }
    frame = next; revision++;
    if (inputsChanged && age >= 10) clicks.replace(view.replaceInput());
    render();
  }
  private static Map<String, WorldLayerState> inputGeometry(WorldMapFrame frame) {
    var result = new HashMap<String, WorldLayerState>();
    for (var region : frame.regions()) if (frame.layerStates().containsKey(region.layer())) result.put(region.layer(), frame.layerStates().get(region.layer()));
    return result;
  }
  void begin() {
    if (!active || started) return;
    reference = view.yaw();
    try {
      if (!view.begin(true, null, () -> finish(true))) { finish(false); return; }
      started = true; render();
    } catch (Exception error) { runtime.failure(viewer, error, () -> finish(true)); }
  }
  void scheduleTick() {
    if (!active) return;
    ticker = viewer.scheduler().later(1, () -> {
      try { tick(); } catch (RuntimeException error) { runtime.failure(viewer, error, () -> finish(true)); }
      if (active) scheduleTick();
    });
  }
  private void tick() {
    if (!active || !started) return;
    if (!viewer.online() || !view.valid()) { finish(!view.dead()); return; }
    age++;
    if (age == 10) { clicks.replace(view.replaceInput()); render(); }
    if (age == 40) render();
    if (age >= 40) emit(WorldMapInput.Type.AIM);
  }
  private Component scene() {
    var result = Component.text().font(Key.key(metadata.font()));
    for (int index = 0; index < definition().layers().size(); index++) {
      var layer = definition().layers().get(index);
      if (!frame.visibleLayers().contains(layer.id()) || frame.layerStates().containsKey(layer.id())) continue;
      var glyph = metadata.glyphs().get(layer.image());
      result.append(Component.text(Character.toString(glyph.character()) + GlyphFont.shift(-glyph.advance()))
          .color(TextColor.color(WorldMapProtocol.WORLD_COLOR, zoom << 3, index | (options.reducedMotion() ? 128 : 0))));
    }
    return result.build();
  }
  private void render() {
    if (!started) return;
    view.hud(hud.render(frame.hud()));
    if (age < 10) return;
    int opacity = age >= 40 || options.reducedMotion() ? 255 : WorldMapProtocol.OPENING_OFFSET + (int)Math.floorMod(view.gameTime() - (age - 10), WorldMapProtocol.OPENING_MODULO);
    view.text("static", scene(), 0, opacity, false);
    for (String id : List.copyOf(dynamic)) if (!frame.visibleLayers().contains(id) || !frame.layerStates().containsKey(id)) { view.removeText(id); dynamic.remove(id); }
    for (var entry : frame.layerStates().entrySet()) {
      String id = entry.getKey(); if (!frame.visibleLayers().contains(id)) continue;
      var state = entry.getValue();
      var glyph = Objects.requireNonNull(metadata.glyphs().get("@runtime/" + id), "Pack lacks runtime layer glyph");
      view.text(id, Component.text(Character.toString(glyph.character())).font(Key.key(metadata.font())).color(TextColor.color(state.color())),
          state.geometry(zoom, options.reducedMotion()), (int)Math.round(state.opacity() * 255), true);
      dynamic.add(id);
    }
  }
  void click(UUID entity, boolean secondary, long tick) {
    if (!active || age < 40 || !clicks.accept(entity, tick)) return;
    emit(secondary ? WorldMapInput.Type.SECONDARY : WorldMapInput.Type.PRIMARY);
  }
  void scroll(int delta) {
    if (!active || !started) return; int next = Math.clamp(zoom + delta, 0, definition().maxZoom());
    if (next != zoom) { zoom = next; render(); emit(WorldMapInput.Type.ZOOM); }
  }
  private void emit(WorldMapInput.Type type) {
    var point = cursor(); var region = WorldMapGeometry.hit(definition(), frame, point);
    try { handler.accept(new WorldMapInput(type, point, region, zoom, revision, System.nanoTime())); }
    catch (RuntimeException error) { runtime.failure(viewer, error, () -> finish(true)); }
  }
  public Map<String, Object> snapshot() {
    check(); var data = new LinkedHashMap<String, Object>(view.diagnostics());
    data.put("view", frame.viewId()); data.put("zoom", zoom); data.put("revision", revision); data.put("referenceYaw", reference);
    data.put("cursor", cursor()); data.put("age", age); data.put("started", started); data.put("map", definition().id());
    data.put("layerStates", frame.layerStates()); data.put("glyphs", frame.visibleLayers().size()); data.put("hudSurfaces", frame.hud().surfaces().size());
    return data;
  }
  void finish(boolean restore) {
    if (!active) return; active = false; runtime.remove(this);
    runtime.dispose(ticker::cancel); runtime.dispose(tasks::close); runtime.dispose(viewTasks::close);
    runtime.dispose(() -> view.close(restore)); runtime.dispose(closed);
  }
  @Override public void close() { runtime.owner.check(viewer); finish(true); }
}
