package gg.kembel.dui.paper;

import gg.kembel.dui.core.*;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.*;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.*;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.*;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.event.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

/** Embedded Paper adapter. It registers no commands and owns no application state. */
public final class Dui implements Listener, AutoCloseable {
  private final JavaPlugin plugin;
  private final PackDescriptor pack;
  private final PackMetadata metadata;
  private final String instance = UUID.randomUUID().toString();
  private final CanvasRenderer renderer = new CanvasRenderer();
  private final CallbackRegistry<Consumer<DialogResponseView>> callbacks = new CallbackRegistry<>();
  private final Map<UUID, DialogSession> sessions = new HashMap<>();
  private final Set<UUID> ready = new HashSet<>(), offered = new HashSet<>();
  private boolean stopped;
  private final WorldMapRuntime worldMaps;
  private final VideoSurfaceRuntime videoSurfaces;
  private final Map<String, BodyBackend> backends = new HashMap<>();

  public void registerBackend(String id, BodyBackend backend) {
    mainThread();
    if (!sessions.isEmpty())
      throw new IllegalStateException("Register backends before opening sessions");
    if (id == null || !id.matches("[a-z][a-z0-9_-]*:[a-z0-9_/-]+") || backends.size() >= 32)
      throw new IllegalArgumentException("Backend id/capacity");
    Objects.requireNonNull(backend);
    if (!metadata.capabilities().containsAll(backend.requiredPackCapabilities()))
      throw new IllegalArgumentException("Pack lacks backend requirements: " + id);
    if (backends.putIfAbsent(id, backend) != null)
      throw new IllegalArgumentException("Duplicate backend: " + id);
  }

  private Dui(JavaPlugin plugin, PackDescriptor pack, PackMetadata metadata) {
    this.plugin = Objects.requireNonNull(plugin);
    this.pack = Objects.requireNonNull(pack);
    this.metadata = Objects.requireNonNull(metadata);
    if (!pack.sha1().equals(metadata.sha1()))
      throw new IllegalArgumentException("Pack descriptor and metadata have different hashes");
    mainThread();
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    worldMaps = new WorldMapRuntime(this, plugin, metadata);
    videoSurfaces = new VideoSurfaceRuntime(this, plugin, metadata);
  }

  public static Dui create(JavaPlugin plugin, PackDescriptor pack, PackMetadata metadata) {
    return new Dui(plugin, pack, metadata);
  }

  void mainThread() {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalStateException("dui Paper API must run on the server thread");
    if (stopped) throw new IllegalStateException("dui is closed");
  }

  /** Compile a consumer-owned screen HUD using the shared template/component engine. */
  public gg.kembel.dui.core.world.WorldHudTemplate compileWorldHud(
      String source, String xml, ComponentRegistry registry) {
    return gg.kembel.dui.core.world.WorldHudTemplate.parse(
        xml,
        RenderEnvironment.plain(new GlyphFont(metadata.worldMapLegendMetrics()))
            .withResources(metadata.bitmapFonts(), metadata.glyphPixels())
            .withPlayerRenderers(metadata.playerRenderers()),
        registry,
        source);
  }

  public MenuTemplate compile(String source) throws Exception {
    return compile("<template>", source, ComponentRegistry.EMPTY);
  }

  public MenuTemplate compile(String name, String source, ComponentRegistry registry)
      throws Exception {
    return MenuTemplate.parse(
        source,
        RenderEnvironment.plain(metadata.font())
            .withResources(metadata.bitmapFonts(), metadata.glyphPixels())
            .withPlayerRenderers(metadata.playerRenderers()),
        registry,
        name);
  }

  public MenuTemplate compile(
      String name, String source, ComponentRegistry registry, RenderEnvironment environment)
      throws Exception {
    return MenuTemplate.parse(
        source,
        new RenderEnvironment(
            metadata.font(),
            environment.tokens(),
            environment.skins(),
            metadata.glyphs(),
            environment.colorTransform(),
            metadata.bitmapFonts(),
            metadata.glyphPixels(),
            metadata.playerRenderers()),
        registry,
        name);
  }

  public gg.kembel.dui.core.world.WorldHudTemplate compileWorldHud(
      String name, String source, ComponentRegistry registry, RenderEnvironment environment) {
    return gg.kembel.dui.core.world.WorldHudTemplate.parse(
        source,
        new RenderEnvironment(
            new GlyphFont(metadata.worldMapLegendMetrics()),
            environment.tokens(),
            environment.skins(),
            metadata.glyphs(),
            environment.colorTransform(),
            metadata.bitmapFonts(),
            metadata.glyphPixels(),
            metadata.playerRenderers()),
        registry,
        name);
  }

  UiScheduler scheduler() {
    return new UiScheduler() {
      public Cancellation later(long ticks, Runnable task) {
        mainThread();
        var scheduled = plugin.getServer().getScheduler().runTaskLater(plugin, task, ticks);
        return scheduled::cancel;
      }

      public void execute(Runnable task) {
        // May be called by a provider thread after shutdown; dropping UI work is intentional.
        if (!plugin.isEnabled()) return;
        try {
          plugin
              .getServer()
              .getScheduler()
              .runTask(
                  plugin,
                  () -> {
                    if (!stopped) task.run();
                  });
        } catch (org.bukkit.plugin.IllegalPluginAccessException ignored) {
          // Disable can race with enqueueing a completion. No provider is cancelled.
        }
      }
    };
  }

  public boolean packLoaded(Player player) {
    return ready.contains(player.getUniqueId());
  }

  public void offerPack(Player player) {
    mainThread();
    if (offered.add(player.getUniqueId()))
      player.addResourcePack(
          pack.id(),
          pack.uri().toString(),
          HexFormat.of().parseHex(pack.sha1()),
          "dui / Vanilla UI",
          pack.required());
  }

  public DialogSession open(
      Player player, MenuTemplate template, ViewModel model, ActionHandler handler) {
    return open(player, template, model, DialogOptions.notice("dui", "Close", "close"), handler);
  }

  public DialogSession open(
      Player player,
      MenuTemplate template,
      ViewModel model,
      DialogOptions options,
      ActionHandler handler) {
    return open(player, template.render(model.data(), model.images()), model, options, handler);
  }

  public DialogSession open(
      Player player, Canvas canvas, ViewModel model, DialogOptions options, ActionHandler handler) {
    mainThread();
    validate(canvas, model);
    worldMaps.closeViewer(player, true);
    videoSurfaces.closeViewer(player, true);
    var previous = sessions.get(player.getUniqueId());
    if (previous != null) dismiss(previous, false);
    var session = new DialogSession(this, player);
    sessions.put(player.getUniqueId(), session);
    session.update(canvas, model, options, handler);
    return session;
  }

  /** Opens a pack-declared map with normal in-game look controls, not a dialog screen. */
  public WorldMapSession openWorldMap(
      Player player,
      String mapId,
      gg.kembel.dui.core.world.WorldMapFrame frame,
      WorldMapOptions options,
      Consumer<gg.kembel.dui.core.world.WorldMapInput> handler) {
    mainThread();
    Objects.requireNonNull(player);
    Objects.requireNonNull(options);
    Objects.requireNonNull(handler);
    var map = metadata.worldMaps().get(mapId);
    if (map == null) throw new IllegalArgumentException("Pack has no world map: " + mapId);
    frame.validate(map.definition());
    var previous = sessions.get(player.getUniqueId());
    if (previous != null) dismiss(previous, true);
    videoSurfaces.closeViewer(player, true);
    return worldMaps.open(player, map, frame, options, handler);
  }

  /** Open a runtime pixel stream. submit() accepts provider threads; all other lifecycle methods are main-thread. */
  public VideoSurfaceSession openVideoSurface(org.bukkit.entity.Player player,
      gg.kembel.dui.core.video.VideoSurfaceSpec spec, VideoSurfaceOptions options,
      java.util.function.Consumer<gg.kembel.dui.core.video.SurfaceInput> input) {
    mainThread();
    java.util.Objects.requireNonNull(spec); java.util.Objects.requireNonNull(options); java.util.Objects.requireNonNull(input);
    if (!metadata.supports(gg.kembel.dui.core.video.MapVideoCodec.CAPABILITY))
      throw new IllegalArgumentException("Pack lacks map-video-v1; rebuild it with matching dui");
    worldMaps.closeViewer(player, true);
    var dialog = sessions.get(player.getUniqueId()); if (dialog != null) dismiss(dialog, true);
    return videoSurfaces.open(player, spec, options, input);
  }

  public gg.kembel.dui.core.video.VideoSurfaceTemplate compileVideoSurface(
      String name, String xml, ComponentRegistry components) {
    mainThread();
    return gg.kembel.dui.core.video.VideoSurfaceTemplate.parse(xml,
        RenderEnvironment.plain(new GlyphFont(metadata.worldMapLegendMetrics()))
            .withResources(metadata.bitmapFonts(), metadata.glyphPixels()), components, name);
  }

  void clearCallbacks(DialogSession session) {
    callbacks.invalidate(session.player.getUniqueId());
    session.actionKeys.clear();
    session.liveActions.clear();
  }

  private Key action(DialogSession session, Canvas.Hit hit, boolean closes, boolean canvasAction) {
    long revision = session.callbackRevision;
    var identity =
        new DialogSession.ActionIdentity(hit.id(), hit.action(), hit.value(), closes, canvasAction);
    session.liveActions.add(identity);
    var existing = session.actionKeys.get(identity);
    if (existing != null) return existing;
    var token =
        callbacks.register(
            session.player.getUniqueId(),
            response -> {
              if (!session.active
                  || session.callbackRevision != revision
                  || !session.liveActions.contains(identity)
                  || sessions.get(session.player.getUniqueId()) != session) return;
              if (closes) dismiss(session, true);
              session.handler.handle(new ActionContext(session.player, session, hit, response));
            });
    var key = Key.key("dui", instance + "/" + token);
    session.actionKeys.put(identity, key);
    return key;
  }

  private ActionButton button(DialogSession s, DialogOptions.Button button, boolean closes) {
    var hit = new Canvas.Hit(button.action(), button.action(), "", button.label(), 0, 0, 1, 9);
    return ActionButton.create(
        Component.text(button.label()),
        null,
        button.width(),
        DialogAction.customClick(action(s, hit, closes, false), null));
  }

  void validate(Canvas canvas, ViewModel model) {
    metadata.validate(canvas);
    model.validate(canvas);
    for (var primitive : canvas.primitives) {
      var backend = backends.get(primitive.type());
      if (backend == null)
        throw new IllegalArgumentException("Unregistered body backend: " + primitive.type());
      backend.validate(primitive);
    }
  }

  void display(DialogSession s) {
    validate(s.canvas, s.model);
    if (!s.sampling) clearCallbacks(s);
    s.liveActions.clear();
    if (!ready.contains(s.player.getUniqueId())) {
      offerPack(s.player);
      return;
    }
    long renderStarted = System.nanoTime();
    var c = s.canvas;
    var model = s.model;
    var itemSnapshots = model.items();
    s.component =
        renderer.renderActions(
            c,
            hit ->
                model.links().containsKey(hit.id())
                    ? ClickEvent.openUrl(model.links().get(hit.id()).toString())
                    : ClickEvent.custom(action(s, hit, false, true), null),
            head -> NativeHeads.render(head, s.player),
            hit ->
                itemSnapshots.containsKey(hit.id())
                    ? itemSnapshots.get(hit.id()).asHoverEvent()
                    : HoverEvent.showText(
                        Component.text(hit.tooltip().isBlank() ? hit.id() : hit.tooltip())),
            (player, band) ->
                NativePlayerModels.band(
                    player,
                    Objects.requireNonNull(
                        model.appearances().get(player.source()),
                        "Missing player appearance: " + player.source()),
                    band,
                    c.motionEnabled));
    var body = new ArrayList<DialogBody>();
    body.add(DialogBody.plainMessage(s.component, c.width + 12));
    body.addAll(
        ShaderItems.bodies(
            c,
            itemSnapshots,
            metadata.models(),
            metadata.supports("motion-tracks"),
            model.appearances(),
            metadata.shaders()));
    var o = s.options;
    for (var primitive : c.renderPlan().primitives) {
      var extra = List.copyOf(backends.get(primitive.type()).render(primitive, model));
      if (body.size() + extra.size() > 256)
        throw new IllegalArgumentException("Dialog body budget exceeded");
      body.addAll(extra);
    }
    var buttons = o.buttons().stream().map(b -> button(s, b, false)).toList();
    var exit = o.exit() == null ? null : button(s, o.exit(), true);
    for (var identity : java.util.List.copyOf(s.actionKeys.keySet()))
      if (!s.liveActions.contains(identity)) {
        var key = s.actionKeys.remove(identity);
        callbacks.invalidate(
            s.player.getUniqueId(), UUID.fromString(key.value().substring(instance.length() + 1)));
      }
    s.player.showDialog(
        Dialog.create(
            builder -> {
              var base =
                  DialogBase.builder(o.title())
                      .pause(false)
                      .afterAction(DialogBase.DialogAfterAction.NONE)
                      .body(body)
                      .inputs(o.inputs())
                      .build();
              var empty = builder.empty().base(base);
              if (o.confirmation())
                empty.type(DialogType.confirmation(buttons.get(0), buttons.get(1)));
              else if (buttons.isEmpty()) empty.type(DialogType.notice(exit));
              else
                empty.type(
                    DialogType.multiAction(buttons).columns(o.columns()).exitAction(exit).build());
            }));
    s.renderNanos = System.nanoTime() - renderStarted;
    s.bodyCount = body.size();
  }

  void dismiss(DialogSession s, boolean closeScreen) {
    if (!s.active) return;
    s.active = false;
    s.revision++;
    s.disposeTasks();
    callbacks.invalidate(s.player.getUniqueId());
    sessions.remove(s.player.getUniqueId(), s);
    if (closeScreen && s.player.isOnline()) s.player.closeDialog();
    s.closed.run();
  }

  @EventHandler
  public void resourcePack(PlayerResourcePackStatusEvent event) {
    if (!event.getID().equals(pack.id())) return;
    var id = event.getPlayer().getUniqueId();
    if (event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
      ready.add(id);
      worldMaps.loaded(event.getPlayer());
      videoSurfaces.loaded(event.getPlayer());
      var session = sessions.get(id);
      if (session != null && session.active) display(session);
    } else if (Set.of("DECLINED", "FAILED_DOWNLOAD", "FAILED_RELOAD", "INVALID_URL", "DISCARDED")
        .contains(event.getStatus().name())) {
      ready.remove(id);
      offered.remove(id);
      worldMaps.closeViewer(event.getPlayer(), true);
      videoSurfaces.closeViewer(event.getPlayer(), true);
      var s = sessions.get(id);
      if (s != null) dismiss(s, false);
      event
          .getPlayer()
          .sendMessage(
              Component.text(
                  "This UI requires the dui resource pack. Run the menu command to try again."));
    }
  }

  @EventHandler
  public void click(PlayerCustomClickEvent event) {
    if (!event.getIdentifier().namespace().equals("dui")
        || !event.getIdentifier().value().startsWith(instance + "/")
        || !(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
    UUID token;
    try {
      token = UUID.fromString(event.getIdentifier().value().substring(instance.length() + 1));
    } catch (IllegalArgumentException e) {
      return;
    }
    var callback = callbacks.consume(connection.getPlayer().getUniqueId(), token);
    if (callback != null) {
      var current = sessions.get(connection.getPlayer().getUniqueId());
      if (current != null) current.actionKeys.clear();
      plugin
          .getServer()
          .getScheduler()
          .runTask(
              plugin,
              () -> {
                if (!stopped && connection.getPlayer().isOnline())
                  callback.accept(event.getDialogResponseView());
              });
    }
  }

  @EventHandler
  public void quit(PlayerQuitEvent event) {
    var id = event.getPlayer().getUniqueId();
    worldMaps.closeViewer(event.getPlayer(), false);
    videoSurfaces.quit(event.getPlayer());
    var s = sessions.get(id);
    if (s != null) dismiss(s, false);
    ready.remove(id);
    offered.remove(id);
    callbacks.invalidate(id);
  }

  @Override
  public void close() {
    if (stopped) return;
    mainThread();
    worldMaps.close();
    videoSurfaces.close();
    for (var s : List.copyOf(sessions.values())) dismiss(s, true);
    HandlerList.unregisterAll(this);
    callbacks.clear();
    ready.clear();
    offered.clear();
    stopped = true;
  }
}
