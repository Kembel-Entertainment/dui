package gg.kembel.dui.paper;

import gg.kembel.dui.core.*;
import java.util.*;
import java.util.function.Consumer;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

/** Embedded Paper adapter; dialogs use the shared platform-independent runtime. */
public final class Dui implements Listener, AutoCloseable {
  private final JavaPlugin plugin;
  private final PackDescriptor pack;
  private final PackMetadata metadata;
  private final gg.kembel.dui.runtime.Dui runtime;
  private final Map<UUID, PaperViewer> viewers = new HashMap<>();
  private final PaperSpatialEvents spatialEvents;
  private boolean stopped;

  private Dui(JavaPlugin plugin, PackDescriptor pack, PackMetadata metadata, boolean externalPack) {
    this.plugin = Objects.requireNonNull(plugin); this.pack = Objects.requireNonNull(pack); this.metadata = Objects.requireNonNull(metadata);
    mainThread();
    runtime = new gg.kembel.dui.runtime.Dui(pack.portable(), metadata.portable(), new gg.kembel.dui.runtime.DialogTransport() {
      public gg.kembel.dui.runtime.NativeItem item(net.kyori.adventure.key.Key material) {
        var type = org.bukkit.Material.matchMaterial(material.asString());
        if (type == null || !type.isItem()) throw new IllegalArgumentException("Unknown item: " + material);
        return PaperItem.of(new org.bukkit.inventory.ItemStack(type));
      }
      public void offerPack(gg.kembel.dui.runtime.UiViewer viewer, gg.kembel.dui.runtime.PackDescriptor p) {
        ((PaperViewer)viewer).player().addResourcePack(p.id(), p.uri().toString(), HexFormat.of().parseHex(p.sha1()), "dui / Vanilla UI", p.required());
      }
      public void show(gg.kembel.dui.runtime.UiViewer viewer, gg.kembel.dui.runtime.DialogPresentation p) { ((PaperViewer)viewer).player().showDialog(PaperDialogs.lower(p)); }
      public void close(gg.kembel.dui.runtime.UiViewer viewer) { ((PaperViewer)viewer).player().closeDialog(); }
      public gg.kembel.dui.runtime.SpatialView spatial(gg.kembel.dui.runtime.UiViewer viewer) {
        return new PaperSpatialView(plugin, ((PaperViewer)viewer).player());
      }
    }, externalPack);
    spatialEvents = new PaperSpatialEvents(this);
    plugin.getServer().getPluginManager().registerEvents(spatialEvents, plugin);
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }
  public static Dui create(JavaPlugin plugin, PackDescriptor pack, PackMetadata metadata) { return new Dui(plugin, pack, metadata, false); }
  /** Attach to an existing pack owner; this adapter never sends a second resource pack. */
  public static Dui createExternallyManaged(JavaPlugin plugin, PackDescriptor pack, PackMetadata metadata) {
    return new Dui(plugin, pack, metadata, true);
  }
  /** Must be called on the server thread after UUID/hash verification by the pack owner. */
  public void externalPackLoaded(Player player, UUID packId, String sha1, boolean loaded) {
    runtime.externalPackLoaded(viewer(player), packId, sha1, loaded);
  }
  /** Portable entry point for menus shared with Minestom. */
  public gg.kembel.dui.runtime.Dui runtime() { return runtime; }
  public PaperViewer viewer(Player player) {
    mainThread(); return viewers.computeIfAbsent(player.getUniqueId(), id -> new PaperViewer(player, scheduler()));
  }
  void mainThread() {
    if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("dui Paper API must run on the server thread");
    if (stopped) throw new IllegalStateException("dui is closed");
  }
  UiScheduler scheduler() {
    return new UiScheduler() {
      public Cancellation later(long ticks, Runnable task) { mainThread(); var scheduled = plugin.getServer().getScheduler().runTaskLater(plugin, task, ticks); return scheduled::cancel; }
      public void execute(Runnable task) {
        if (!plugin.isEnabled()) return;
        try { plugin.getServer().getScheduler().runTask(plugin, () -> { if (!stopped) task.run(); }); }
        catch (org.bukkit.plugin.IllegalPluginAccessException ignored) { }
      }
    };
  }
  public void registerBackend(String id, BodyBackend backend) {
    mainThread(); Objects.requireNonNull(backend);
    runtime.registerBackend(id, new gg.kembel.dui.runtime.BodyBackend() {
      public Set<String> requiredPackCapabilities() { return backend.requiredPackCapabilities(); }
      public Set<String> geometryCapabilities() { return backend.geometryCapabilities(); }
      public void validate(RenderPrimitive primitive) { backend.validate(primitive); }
      public List<gg.kembel.dui.runtime.DialogBody> render(RenderPrimitive primitive, gg.kembel.dui.runtime.ViewModel model) {
        return backend.render(primitive, ViewModel.fromPortable(model)).stream().map(b -> (gg.kembel.dui.runtime.DialogBody)new PaperDialogs.Body(b)).toList();
      }
    });
  }
  public MenuTemplate compile(String source) throws Exception { return runtime.compile(source); }
  public MenuTemplate compile(String name, String source, ComponentRegistry registry) throws Exception { return runtime.compile(name, source, registry); }
  public MenuTemplate compile(String name, String source, ComponentRegistry registry, RenderEnvironment environment) throws Exception { return runtime.compile(name, source, registry, environment); }
  public gg.kembel.dui.core.world.WorldHudTemplate compileWorldHud(String name, String xml, ComponentRegistry registry) { return runtime.compileWorldHud(name, xml, registry); }
  public gg.kembel.dui.core.world.WorldHudTemplate compileWorldHud(String name, String xml, ComponentRegistry registry, RenderEnvironment environment) { return runtime.compileWorldHud(name, xml, registry, environment); }
  public gg.kembel.dui.core.video.VideoSurfaceTemplate compileVideoSurface(String name, String xml, ComponentRegistry components) { return runtime.compileVideoSurface(name, xml, components); }
  public boolean packLoaded(Player player) { return runtime.packLoaded(viewer(player)); }
  public void offerPack(Player player) { runtime.offerPack(viewer(player)); }
  public DialogSession open(Player player, MenuTemplate template, ViewModel model, ActionHandler handler) { return open(player, template, model, DialogOptions.notice("dui", "Close", "close"), handler); }
  public DialogSession open(Player player, MenuTemplate template, ViewModel model, DialogOptions options, ActionHandler handler) { return open(player, template.render(model.data(), model.images()), model, options, handler); }
  public DialogSession open(Player player, Canvas canvas, ViewModel model, DialogOptions options, ActionHandler handler) {
    mainThread(); Objects.requireNonNull(handler);
    var holder = new DialogSession[1];
    var shared = runtime.open(viewer(player), canvas, model.portable(), options.portable(), context -> handler.handle(new ActionContext(player, holder[0], context.hit(),
        context.response() instanceof PaperResponse p ? p.nativeResponse() : null)));
    return holder[0] = new DialogSession(this, player, shared);
  }
  public WorldMapSession openWorldMap(Player player, String mapId, gg.kembel.dui.core.world.WorldMapFrame frame,
      WorldMapOptions options, Consumer<gg.kembel.dui.core.world.WorldMapInput> handler) {
    return new WorldMapSession(runtime.spatial().openWorldMap(viewer(player), mapId, frame,
        new gg.kembel.dui.runtime.WorldMapOptions(options.reducedMotion()), handler));
  }
  public VideoSurfaceSession openVideoSurface(Player player, gg.kembel.dui.core.video.VideoSurfaceSpec spec,
      VideoSurfaceOptions options, Consumer<gg.kembel.dui.core.video.SurfaceInput> input) {
    return new VideoSurfaceSession(runtime.spatial().openVideoSurface(viewer(player), spec,
        new gg.kembel.dui.runtime.VideoSurfaceOptions(options.anchorSlot(), options.closeOnSneak()), input));
  }
  @EventHandler public void resourcePack(PlayerResourcePackStatusEvent event) {
    if (stopped || !event.getID().equals(pack.id())) return;
    runtime.resourcePack(viewer(event.getPlayer()), event.getID(), event.getStatus().name());
  }
  @EventHandler public void click(PlayerCustomClickEvent event) {
    if (stopped || !(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
    var key = event.getIdentifier(); var response = new PaperResponse(event.getDialogResponseView());
    scheduler().execute(() -> { if (connection.getPlayer().isOnline()) runtime.click(viewer(connection.getPlayer()), key, response); });
  }
  @EventHandler public void quit(PlayerQuitEvent event) {
    if (stopped) return;
    var viewer = viewers.remove(event.getPlayer().getUniqueId()); if (viewer != null) runtime.quit(viewer);
  }
  @Override public void close() {
    if (stopped) return; mainThread(); runtime.close();
    HandlerList.unregisterAll(this); HandlerList.unregisterAll(spatialEvents); viewers.clear(); stopped = true;
  }
}
