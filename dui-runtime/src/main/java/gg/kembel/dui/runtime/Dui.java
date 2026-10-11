package gg.kembel.dui.runtime;

import gg.kembel.dui.core.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

/** Shared embedded runtime. Per-viewer state is confined to that viewer's UI execution context. */
public final class Dui implements AutoCloseable {
  private static final class ViewerState {
    final UiViewer viewer;
    final CallbackRegistry<java.util.function.Consumer<DialogResponse>> callbacks = new CallbackRegistry<>();
    boolean ready, offered;
    DialogSession session;
    ViewerState(UiViewer viewer) { this.viewer = viewer; }
  }
  private final PackDescriptor pack;
  private final PackMetadata metadata;
  private final DialogTransport transport;
  private final boolean externalPack;
  private final SpatialRuntime spatial;
  private final String instance = UUID.randomUUID().toString();
  private final CanvasRenderer renderer = new CanvasRenderer();
  private final Map<UUID, ViewerState> viewers = new ConcurrentHashMap<>();
  private final Map<String, BodyBackend> backends = new HashMap<>();
  private final AtomicBoolean stopped = new AtomicBoolean();
  private boolean registrationsClosed;

  public Dui(PackDescriptor pack, PackMetadata metadata, DialogTransport transport) {
    this(pack, metadata, transport, false);
  }
  /** External mode never offers a pack or trusts uncorrelated client status events. */
  public Dui(PackDescriptor pack, PackMetadata metadata, DialogTransport transport, boolean externalPack) {
    this.externalPack = externalPack;
    this.pack = Objects.requireNonNull(pack); this.metadata = Objects.requireNonNull(metadata);
    this.transport = Objects.requireNonNull(transport);
    this.spatial = new SpatialRuntime(this, transport::spatial);
    if (!pack.sha1().equals(metadata.sha1())) throw new IllegalArgumentException("Pack descriptor and metadata have different hashes");
  }
  public PackMetadata metadata() { return metadata; }
  public SpatialRuntime spatial() { return spatial; }
  public synchronized void registerBackend(String id, BodyBackend backend) {
    if (stopped.get() || registrationsClosed) throw new IllegalStateException("Register backends before opening sessions");
    if (id == null || !id.matches("[a-z][a-z0-9_-]*:[a-z0-9_/-]+") || backends.size() >= 32) throw new IllegalArgumentException("Backend id/capacity");
    Objects.requireNonNull(backend);
    if (!metadata.capabilities().containsAll(backend.requiredPackCapabilities())) throw new IllegalArgumentException("Pack lacks backend requirements: " + id);
    if (backends.putIfAbsent(id, backend) != null) throw new IllegalArgumentException("Duplicate backend: " + id);
  }
  void check(UiViewer viewer) {
    viewer.checkThread();
    if (stopped.get()) throw new IllegalStateException("dui is closed");
  }
  private ViewerState state(UiViewer viewer) {
    return viewers.computeIfAbsent(viewer.id(), id -> new ViewerState(viewer));
  }
  public boolean packLoaded(UiViewer viewer) { check(viewer); return state(viewer).ready; }
  public void offerPack(UiViewer viewer) {
    check(viewer); if (externalPack) return; var state = state(viewer);
    if (!state.offered) { state.offered = true; try { transport.offerPack(viewer, pack); } catch (RuntimeException error) { state.offered = false; throw error; } }
  }
  public MenuTemplate compile(String source) throws Exception { return compile("<template>", source, ComponentRegistry.EMPTY); }
  public MenuTemplate compile(String name, String source) throws Exception { return compile(name, source, ComponentRegistry.EMPTY); }
  public gg.kembel.dui.core.world.WorldHudTemplate compileWorldHud(String name, String xml, ComponentRegistry registry) {
    return compileWorldHud(name, xml, registry, RenderEnvironment.plain(new GlyphFont(metadata.worldMapLegendMetrics())));
  }
  public gg.kembel.dui.core.world.WorldHudTemplate compileWorldHud(String name, String xml, ComponentRegistry registry, RenderEnvironment environment) {
    return gg.kembel.dui.core.world.WorldHudTemplate.parse(xml,
        new RenderEnvironment(new GlyphFont(metadata.worldMapLegendMetrics()), environment.tokens(), environment.skins(), metadata.glyphs(),
            environment.colorTransform(), metadata.bitmapFonts(), metadata.glyphPixels(), metadata.playerRenderers()), registry, name);
  }
  public gg.kembel.dui.core.video.VideoSurfaceTemplate compileVideoSurface(String name, String xml, ComponentRegistry registry) {
    return gg.kembel.dui.core.video.VideoSurfaceTemplate.parse(xml,
        RenderEnvironment.plain(new GlyphFont(metadata.worldMapLegendMetrics())).withResources(metadata.bitmapFonts(), metadata.glyphPixels()), registry, name);
  }
  public MenuTemplate compile(String name, String source, ComponentRegistry registry) throws Exception {
    return MenuTemplate.parse(source, environment(RenderEnvironment.plain(metadata.font())), registry, name);
  }
  public MenuTemplate compile(String name, String source, ComponentRegistry registry, RenderEnvironment environment) throws Exception {
    return MenuTemplate.parse(source, environment(environment), registry, name);
  }
  private RenderEnvironment environment(RenderEnvironment environment) {
    return new RenderEnvironment(metadata.font(), environment.tokens(), environment.skins(), metadata.glyphs(),
        environment.colorTransform(), metadata.bitmapFonts(), metadata.glyphPixels(), metadata.playerRenderers());
  }
  public DialogSession open(UiViewer viewer, MenuTemplate template, ViewModel model, ActionHandler handler) {
    return open(viewer, template, model, DialogOptions.notice("dui", "Close", "close"), handler);
  }
  public DialogSession open(UiViewer viewer, MenuTemplate template, ViewModel model, DialogOptions options, ActionHandler handler) {
    return open(viewer, template.render(model.data(), model.images()), model, options, handler);
  }
  public DialogSession open(UiViewer viewer, Canvas canvas, ViewModel model, DialogOptions options, ActionHandler handler) {
    check(viewer); validate(canvas, model); Objects.requireNonNull(options); Objects.requireNonNull(handler);
    synchronized (this) { registrationsClosed = true; }
    spatial.closeViewer(viewer, true); transport.beforeOpen(viewer);
    var state = state(viewer);
    if (state.session != null) dismiss(state.session, false);
    var session = new DialogSession(this, viewer); state.session = session;
    try { session.update(canvas, model, options, handler); }
    catch (RuntimeException error) { dismiss(session, false); throw error; }
    return session;
  }
  void clearCallbacks(DialogSession session) {
    state(session.player).callbacks.invalidate(session.player.id()); session.actionKeys.clear(); session.liveActions.clear();
  }
  private Key action(DialogSession session, Canvas.Hit hit, boolean closes, boolean canvasAction) {
    long revision = session.callbackRevision;
    var identity = new DialogSession.ActionIdentity(hit.id(), hit.action(), hit.value(), closes, canvasAction);
    session.liveActions.add(identity);
    var existing = session.actionKeys.get(identity); if (existing != null) return existing;
    var token = state(session.player).callbacks.register(session.player.id(), response -> {
      if (!session.active || session.callbackRevision != revision || !session.liveActions.contains(identity)
          || state(session.player).session != session) return;
      if (closes) dismiss(session, true);
      session.handler.handle(new ActionContext(session.player, session, hit, response));
    });
    var key = Key.key("dui", instance + "/" + token); session.actionKeys.put(identity, key); return key;
  }
  private DialogPresentation.Button button(DialogSession session, DialogOptions.Button button, boolean closes) {
    var hit = new Canvas.Hit(button.action(), button.action(), "", button.label(), 0, 0, 1, 9);
    return new DialogPresentation.Button(button, action(session, hit, closes, false));
  }
  void validate(Canvas canvas, ViewModel model) {
    metadata.validate(canvas); model.validate(canvas);
    for (var primitive : canvas.primitives) {
      var backend = backends.get(primitive.type());
      if (backend == null) throw new IllegalArgumentException("Unregistered body backend: " + primitive.type());
      backend.validate(primitive);
    }
  }
  void display(DialogSession session) {
    validate(session.canvas, session.model);
    if (!session.sampling) clearCallbacks(session); session.liveActions.clear();
    if (!state(session.player).ready) { offerPack(session.player); return; }
    long started = System.nanoTime(); var canvas = session.canvas; var model = session.model;
    session.component = renderer.renderActions(canvas,
        hit -> model.links().containsKey(hit.id()) ? ClickEvent.openUrl(model.links().get(hit.id()).toString()) : ClickEvent.custom(action(session, hit, false, true), net.kyori.adventure.nbt.api.BinaryTagHolder.binaryTagHolder("{}")),
        head -> NativeHeads.render(head, session.player),
        hit -> model.items().containsKey(hit.id()) ? model.items().get(hit.id()).hover() : HoverEvent.showText(Component.text(hit.tooltip().isBlank() ? hit.id() : hit.tooltip())),
        (player, band) -> NativePlayerModels.band(player, Objects.requireNonNull(model.appearances().get(player.source()), "Missing player appearance: " + player.source()), band, canvas.motionEnabled));
    var bodies = new ArrayList<DialogBody>(); bodies.add(new DialogBody.Text(session.component, canvas.width + 12));
    bodies.addAll(ShaderItems.bodies(canvas, model, metadata, transport::item));
    for (var primitive : canvas.renderPlan().primitives) bodies.addAll(List.copyOf(backends.get(primitive.type()).render(primitive, model)));
    if (bodies.size() > 256) throw new IllegalArgumentException("Dialog body budget exceeded");
    var buttons = session.options.buttons().stream().map(b -> button(session, b, false)).toList();
    var exit = session.options.exit() == null ? null : button(session, session.options.exit(), true);
    for (var identity : List.copyOf(session.actionKeys.keySet())) if (!session.liveActions.contains(identity)) {
      var key = session.actionKeys.remove(identity);
      state(session.player).callbacks.invalidate(session.player.id(), UUID.fromString(key.value().substring(instance.length() + 1)));
    }
    transport.show(session.player, new DialogPresentation(session.options, bodies, buttons, exit));
    session.renderNanos = System.nanoTime() - started; session.bodyCount = bodies.size();
  }
  void dismiss(DialogSession session, boolean closeScreen) {
    if (!session.active) return;
    session.active = false; session.revision++; spatial.dispose(session::disposeTasks);
    var state = viewers.get(session.player.id());
    if (state != null) { state.callbacks.invalidate(session.player.id()); if (state.session == session) state.session = null; }
    if (closeScreen && session.player.online()) spatial.dispose(() -> transport.close(session.player));
    spatial.dispose(session.closed);
  }
  public void closeViewer(UiViewer viewer, boolean closeScreen) {
    check(viewer); var state = viewers.get(viewer.id()); if (state != null && state.session != null) dismiss(state.session, closeScreen);
  }
  public void resourcePack(UiViewer viewer, UUID packId, String status) {
    check(viewer); if (externalPack || !pack.id().equals(packId)) return;
    var state = state(viewer);
    if (status.equals("SUCCESSFULLY_LOADED")) { state.ready = true; spatial.loaded(viewer); if (state.session != null && state.session.active) display(state.session); }
    else if (Set.of("DECLINED", "FAILED_DOWNLOAD", "FAILED_RELOAD", "INVALID_URL", "DISCARDED").contains(status)) {
      state.ready = false; state.offered = false; spatial.closeViewer(viewer, true); if (state.session != null) dismiss(state.session, false);
      viewer.message(Component.text("This UI requires the dui resource pack. Run the menu command to try again."));
    }
  }
  /** Set readiness only after the external pack owner verified this exact UUID and hash. */
  public void externalPackLoaded(UiViewer viewer, UUID packId, String sha1, boolean loaded) {
    check(viewer);
    if (!externalPack) throw new IllegalStateException("Pack is managed by dui");
    if (!pack.id().equals(packId) || !pack.sha1().equals(sha1)) return;
    var state = state(viewer);
    if (state.ready == loaded) return;
    state.ready = loaded;
    if (loaded) {
      spatial.loaded(viewer);
      if (state.session != null && state.session.active) display(state.session);
    } else {
      spatial.closeViewer(viewer, true);
      if (state.session != null) dismiss(state.session, true);
    }
  }
  public boolean click(UiViewer viewer, Key key, DialogResponse response) {
    check(viewer);
    if (!key.namespace().equals("dui") || !key.value().startsWith(instance + "/")) return false;
    UUID token;
    try { token = UUID.fromString(key.value().substring(instance.length() + 1)); } catch (IllegalArgumentException error) { return false; }
    var state = viewers.get(viewer.id()); if (state == null) return false;
    var callback = state.callbacks.consume(viewer.id(), token); if (callback == null) return false;
    if (state.session != null) state.session.actionKeys.clear();
    callback.accept(response == null ? DialogResponse.EMPTY : response); return true;
  }
  public void quit(UiViewer viewer) {
    viewer.checkThread(); spatial.quit(viewer); var state = viewers.get(viewer.id()); if (state == null) return;
    if (state.session != null) dismiss(state.session, false); state.callbacks.clear(); viewers.remove(viewer.id(), state);
  }
  @Override public void close() {
    if (!stopped.compareAndSet(false, true)) return;
    for (var state : List.copyOf(viewers.values())) {
      Runnable cleanup = () -> { try { spatial.closeViewer(state.viewer, true); spatial.quit(state.viewer); if (state.session != null) dismiss(state.session, true); } finally { state.callbacks.clear(); viewers.remove(state.viewer.id(), state); } };
      spatial.dispose(() -> {
        if (state.viewer.isOwnerThread() || !state.viewer.online()) cleanup.run();
        else state.viewer.scheduler().execute(cleanup);
      });
    }
    spatial.close();
  }
}
