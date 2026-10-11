package gg.kembel.dui.runtime;

import gg.kembel.dui.core.*;
import gg.kembel.dui.core.world.*;
import gg.kembel.dui.testing.FakeScheduler;
import java.net.URI;
import java.util.*;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.object.*;

final class RuntimeFixture {
  static final class Viewer implements UiViewer {
    final UUID id = UUID.randomUUID();
    final FakeScheduler clock = new FakeScheduler();
    volatile Thread owner = Thread.currentThread();
    boolean online = true;
    int messages;
    public UUID id() { return id; }
    public boolean online() { return online; }
    public boolean isOwnerThread() { return Thread.currentThread() == owner; }
    public void checkThread() { if (!isOwnerThread()) throw new IllegalStateException("Viewer ownership"); }
    public UiScheduler scheduler() { return clock; }
    public PlayerHeadObjectContents head() { return ObjectContents.playerHead().name("Fixture").build(); }
    public void message(Component message) { messages++; }
  }
  record Item(Key model, List<Integer> colors) implements NativeItem {
    Item { colors = List.copyOf(colors); }
    public boolean empty() { return false; }
    public Key armorAsset() { return null; }
    public NativeItem withModel(Key model) { return new Item(model, colors); }
    public NativeItem withColors(List<Integer> colors) { return new Item(model, colors); }
    public NativeItem withoutGlint() { return this; }
    public HoverEvent<?> hover() { return HoverEvent.showText(Component.text("item")); }
  }
  static final class View implements SpatialView {
    boolean valid = true, began, closed, restored;
    int inputs, textCount;
    UUID target;
    public boolean begin(boolean reset, Integer slot, Runnable abort) { began = true; return true; }
    public boolean valid() { return valid; }
    public boolean dead() { return false; }
    public float yaw() { return 0; }
    public float pitch() { return 0; }
    public long gameTime() { return 100; }
    public void text(String id, Component contents, float translation, int opacity, boolean overlay) { textCount++; }
    public void removeText(String id) {}
    public UUID replaceInput() { inputs++; return target = UUID.randomUUID(); }
    public void hud(Component component) {}
    public MapVideoConnection video(int[] ids, java.util.function.Consumer<Throwable> failure) { throw new UnsupportedOperationException(); }
    public Map<String, Object> diagnostics() { return Map.of(); }
    public void close(boolean restore) { closed = true; restored = restore; }
  }
  static final class Transport implements DialogTransport {
    final Map<UUID, DialogPresentation> shown = new HashMap<>();
    final Map<UUID, View> views = new HashMap<>();
    int offers, closes;
    boolean failShow;
    public NativeItem item(Key key) { return new Item(key, List.of()); }
    public void offerPack(UiViewer viewer, PackDescriptor pack) { offers++; }
    public void show(UiViewer viewer, DialogPresentation presentation) {
      if (failShow) throw new IllegalStateException("Native show failure");
      shown.put(viewer.id(), presentation);
    }
    public void close(UiViewer viewer) { closes++; shown.remove(viewer.id()); }
    public SpatialView spatial(UiViewer viewer) { var view = new View(); views.put(viewer.id(), view); return view; }
  }
  static WorldMapDefinition definition() {
    return new WorldMapDefinition("fixture:map", 600, 300, 5, 100, 10, 3, 12,
        List.of(new WorldMapDefinition.Layer("layer", "image", 0, 0, 600, 300, WorldMapDefinition.Space.MAP, .9, 1, 1, 0)),
        new WorldMapDefinition.Opening(12, .9, 0, 1, Motion.Easing.LINEAR));
  }
  static PackMetadata metadata(boolean spatial) {
    var caps = new HashSet<>(Set.of("native", "shader-components-v1", "clips", "motion-tracks", "effects-32"));
    Map<String, WorldMapMetadata> maps = Map.of();
    if (spatial) {
      caps.add(WorldMapProtocol.CAPABILITY); caps.add(gg.kembel.dui.core.video.MapVideoCodec.CAPABILITY);
      var definition = definition();
      maps = Map.of(definition.id(), new WorldMapMetadata(definition, "dui:fixture", Map.of("image", new WorldMapMetadata.Glyph(0xE000, 1)),
          definition.geometryHash(), WorldMapProtocol.SHA256));
    }
    return new PackMetadata(MinecraftTarget.VERSION, MinecraftTarget.LIBRARY_VERSION, "a".repeat(40),
        Set.of("minecraft:paper"), Map.of("?", 6), RendererProtocol.VERSION, caps, RendererProtocol.SCHEMA_SHA256,
        maps, spatial ? Map.of("?", 6) : Map.of(), Map.of(), Map.of());
  }
  final Viewer viewer = new Viewer();
  final Transport transport = new Transport();
  final PackMetadata metadata;
  final PackDescriptor pack;
  final Dui dui;
  RuntimeFixture() { this(false); }
  RuntimeFixture(boolean spatial) {
    metadata = metadata(spatial); pack = PackDescriptor.of(URI.create("https://example.org/dui.zip"), metadata);
    dui = new Dui(pack, metadata, transport);
  }
  void loaded(Viewer viewer) { dui.resourcePack(viewer, pack.id(), "SUCCESSFULLY_LOADED"); }
  Canvas canvas() {
    var canvas = new Canvas(120, 18, RenderEnvironment.plain(metadata.font()));
    canvas.hit(new Canvas.Hit("select", "select", "value", "Fixture", 0, 0, 120, 9)); return canvas;
  }
  DialogSession open(Viewer viewer, ActionHandler action) { return dui.open(viewer, canvas(), ViewModel.data(Map.of()), DialogOptions.notice("Fixture", "Close", "close"), action); }
  Key hitKey(Viewer viewer) { return duiKey(transport.shown.get(viewer.id()).body().getFirst()); }
  private Key duiKey(DialogBody body) { return find(((DialogBody.Text)body).contents()); }
  private Key find(Component component) {
    if (component.clickEvent() != null && component.clickEvent().payload() instanceof net.kyori.adventure.text.event.ClickEvent.Payload.Custom custom) return custom.key();
    for (var child : component.children()) { var key = find(child); if (key != null) return key; } return null;
  }
  WorldMapFrame frame(String id) {
    return new WorldMapFrame(id, List.of("layer"), List.of(new WorldMapFrame.Region("target", "layer", "pick", "value", true)), WorldHud.EMPTY);
  }
}
