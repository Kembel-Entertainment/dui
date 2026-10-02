package gg.kembel.dui.paper;

import gg.kembel.dui.core.*;
import gg.kembel.dui.core.world.*;
import java.util.*;
import java.util.function.Consumer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** One private world-map presentation. All public operations run on Paper's main thread. */
public final class WorldMapSession implements AutoCloseable {
  final WorldMapRuntime runtime;
  final Player player;
  final WorldMapMetadata metadata;
  final WorldMapOptions options;
  private final Consumer<WorldMapInput> handler;
  private final TaskScope tasks;
  private TaskScope viewTasks;
  private WorldMapFrame frame;
  private Runnable closed = () -> {};
  boolean active = true, started, ownTeleport;
  private Location original;
  private WeatherType originalWeather;
  private int originalSlot;
  private float reference;
  private int zoom;
  private long age, revision;
  private ItemDisplay seat;
  private TextDisplay display;
  private final Map<String, TextDisplay> dynamicDisplays = new LinkedHashMap<>();
  private Interaction input;
  private WorldMapClientAppearance appearance;
  private final WorldMapClickGate clicks = new WorldMapClickGate();
  private final BossBar hud =
      BossBar.bossBar(Component.empty(), 0, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
  private final WorldMapHud hudRenderer;

  WorldMapSession(
      WorldMapRuntime runtime,
      Player player,
      WorldMapMetadata metadata,
      WorldMapFrame frame,
      WorldMapOptions options,
      Consumer<WorldMapInput> handler) {
    this.runtime = runtime;
    this.player = player;
    this.metadata = metadata;
    this.frame = frame;
    this.options = Objects.requireNonNull(options);
    this.handler = Objects.requireNonNull(handler);
    zoom = metadata.definition().defaultZoom();
    tasks = new TaskScope(runtime.owner.scheduler(), () -> active && player.isOnline());
    viewTasks = new TaskScope(runtime.owner.scheduler(), () -> active && player.isOnline());
    hudRenderer =
        new WorldMapHud(runtime.metadata.worldMapLegendMetrics(), runtime.metadata.glyphs());
  }

  public boolean isActive() {
    return active;
  }

  public boolean isStarted() {
    return started;
  }

  public long revision() {
    return revision;
  }

  public int zoom() {
    return zoom;
  }

  public WorldMapFrame frame() {
    return frame;
  }

  public WorldMapDefinition definition() {
    return metadata.definition();
  }

  public TaskScope tasks() {
    check();
    return tasks;
  }

  public TaskScope viewTasks() {
    check();
    return viewTasks;
  }

  public WorldMapSession onClose(Runnable callback) {
    check();
    closed = Objects.requireNonNull(callback);
    return this;
  }

  public WorldMapGeometry.Point cursor() {
    check();
    var location = player.getLocation();
    return WorldMapGeometry.cursor(definition(), location.getYaw(), location.getPitch(), reference);
  }

  private void check() {
    runtime.owner.mainThread();
    if (!active) throw new IllegalStateException("World-map session is closed");
  }

  public void update(WorldMapFrame next) {
    check();
    next.validate(definition());
    if (next.equals(frame)) return;
    boolean viewChanged = !frame.viewId().equals(next.viewId());
    boolean inputsChanged =
        viewChanged
            || !frame.regions().equals(next.regions())
            || !inputGeometry(frame).equals(inputGeometry(next));
    if (viewChanged) {
      viewTasks.close();
      viewTasks = new TaskScope(runtime.owner.scheduler(), () -> active && player.isOnline());
    }
    frame = next;
    revision++;
    if (inputsChanged && input != null) replaceInput();
    render();
  }

  private static Map<String, WorldLayerState> inputGeometry(WorldMapFrame frame) {
    var result = new HashMap<String, WorldLayerState>();
    for (var region : frame.regions())
      if (frame.layerStates().containsKey(region.layer()))
        result.put(region.layer(), frame.layerStates().get(region.layer()));
    return result;
  }

  void begin() {
    if (!active || started) return;
    if (player.isInsideVehicle()
        || player.isDead()
        || player.isGliding()
        || player.isFlying()
        || player.isSleeping()
        || player.isSwimming()) {
      player.sendMessage("Stand on the ground outside a vehicle to open the map.");
      finish(false);
      return;
    }
    original = player.getLocation().clone();
    originalSlot = player.getInventory().getHeldItemSlot();
    originalWeather = player.getPlayerWeather();
    reference = original.getYaw();
    started = true;
    try {
      seat =
          player
              .getWorld()
              .spawn(
                  original.clone().add(0, .6, 0),
                  ItemDisplay.class,
                  e -> {
                    e.setVisibleByDefault(false);
                    e.setPersistent(false);
                    e.setGravity(false);
                    e.setInvulnerable(true);
                    e.setSilent(true);
                  });
      player.showEntity(runtime.plugin, seat);
      player.setRotation(reference, 0);
      if (!seat.addPassenger(player))
        throw new IllegalStateException("Cannot mount world-map seat");
      player.setPlayerWeather(WeatherType.CLEAR);
      appearance = new WorldMapClientAppearance(runtime.plugin, player, () -> finish(true));
      render();
      player.showBossBar(hud);
    } catch (Exception error) {
      runtime.failure(this, error);
    }
  }

  void tick() {
    if (!active || !started) return;
    if (!player.isOnline()
        || player.isDead()
        || seat == null
        || !seat.isValid()
        || player.getVehicle() != seat) {
      finish(!player.isDead());
      return;
    }
    age++;
    if (age == 10) spawnDisplay();
    if (age == 40 && display != null) display.setTextOpacity((byte) 255);
    if (age >= 40) emit(WorldMapInput.Type.AIM);
  }

  private void spawnDisplay() {
    var anchor = player.getEyeLocation();
    double radians = Math.toRadians(reference);
    anchor.add(-Math.sin(radians), 0, Math.cos(radians));
    anchor.setYaw(reference);
    anchor.setPitch(0);
    byte phase =
        options.reducedMotion()
            ? (byte) 255
            : (byte)
                (WorldMapProtocol.OPENING_OFFSET
                    + Math.floorMod(
                        player.getWorld().getGameTime(), WorldMapProtocol.OPENING_MODULO));
    display =
        player
            .getWorld()
            .spawn(
                anchor,
                TextDisplay.class,
                e -> {
                  e.setVisibleByDefault(false);
                  e.setPersistent(false);
                  e.setGravity(false);
                  e.setInvulnerable(true);
                  e.setBillboard(Display.Billboard.FIXED);
                  e.setDisplayWidth(8);
                  e.setDisplayHeight(8);
                  e.setViewRange(1);
                  e.setTransformation(
                      new Transformation(
                          new Vector3f(),
                          new Quaternionf(),
                          // The shader expands stamped corners. Keep native glyph offsets
                          // negligible when it derives the camera anchor from Position.
                          new Vector3f(1e-8f),
                          new Quaternionf()));
                  e.setBrightness(new Display.Brightness(15, 15));
                  e.setShadowed(false);
                  e.setSeeThrough(false);
                  e.setDefaultBackground(false);
                  e.setBackgroundColor(Color.fromARGB(0));
                  e.setLineWidth(1_000_000);
                  e.setTextOpacity(phase);
                  e.text(scene());
                });
    player.showEntity(runtime.plugin, display);
    replaceInput();
    renderDynamic();
  }

  private Component scene() {
    var result = Component.text().font(Key.key(metadata.font()));
    for (int index = 0; index < definition().layers().size(); index++) {
      var layer = definition().layers().get(index);
      if (!frame.visibleLayers().contains(layer.id())) continue;
      if (frame.layerStates().containsKey(layer.id())) continue;
      var glyph = metadata.glyphs().get(layer.image());
      result.append(
          Component.text(Character.toString(glyph.character()) + GlyphFont.shift(-glyph.advance()))
              .color(
                  TextColor.color(
                      WorldMapProtocol.WORLD_COLOR,
                      zoom << 3,
                      index | (options.reducedMotion() ? 128 : 0))));
    }
    return result.build();
  }

  private void renderDynamic() {
    for (String key : List.copyOf(dynamicDisplays.keySet()))
      if (!frame.visibleLayers().contains(key) || !frame.layerStates().containsKey(key))
        dynamicDisplays.remove(key).remove();
    for (var entry : frame.layerStates().entrySet()) {
      String key = entry.getKey();
      if (!frame.visibleLayers().contains(key)) continue;
      var state = entry.getValue();
      var entity = dynamicDisplays.get(key);
      if (entity == null) {
        var anchor = player.getEyeLocation();
        double radians = Math.toRadians(reference);
        anchor.add(-Math.sin(radians), 0, Math.cos(radians));
        anchor.setYaw(reference);
        anchor.setPitch(0);
        entity =
            player
                .getWorld()
                .spawn(
                    anchor,
                    TextDisplay.class,
                    e -> {
                      e.setVisibleByDefault(false);
                      e.setPersistent(false);
                      e.setGravity(false);
                      e.setInvulnerable(true);
                      e.setBillboard(Display.Billboard.FIXED);
                      e.setDisplayWidth(8);
                      e.setDisplayHeight(8);
                      e.setViewRange(1);
                      e.setBrightness(new Display.Brightness(15, 15));
                      e.setShadowed(false);
                      // Overlay pass blends runtime alpha over the static map instead of the world.
                      e.setSeeThrough(true);
                      e.setDefaultBackground(false);
                      e.setBackgroundColor(Color.fromARGB(0));
                      e.setLineWidth(1_000_000);
                    });
        dynamicDisplays.put(key, entity);
        player.showEntity(runtime.plugin, entity);
      }
      var transform =
          new Transformation(
              new Vector3f(0, state.geometry(zoom, options.reducedMotion()), 0),
              new Quaternionf(),
              new Vector3f(1e-8f),
              new Quaternionf());
      if (!transform.equals(entity.getTransformation())) entity.setTransformation(transform);
      byte opacity = (byte) Math.round(state.opacity() * 255);
      if (entity.getTextOpacity() != opacity) entity.setTextOpacity(opacity);
      var glyph =
          Objects.requireNonNull(
              metadata.glyphs().get("@runtime/" + key), "Pack lacks runtime layer glyph");
      var text =
          Component.text(Character.toString(glyph.character()))
              .font(Key.key(metadata.font()))
              .color(TextColor.color(state.color()));
      if (!text.equals(entity.text())) entity.text(text);
    }
  }

  private void render() {
    if (!started) return;
    var name = hudRenderer.render(frame.hud());
    if (!name.equals(hud.name())) hud.name(name);
    float progress = 1;
    if (hud.progress() != progress) hud.progress(progress);
    if (display != null) {
      var next = scene();
      if (!next.equals(display.text())) display.text(next);
      renderDynamic();
    }
  }

  private void replaceInput() {
    if (input != null) input.remove();
    input =
        player
            .getWorld()
            .spawn(
                player.getEyeLocation().add(0, -2, 0),
                Interaction.class,
                e -> {
                  e.setVisibleByDefault(false);
                  e.setPersistent(false);
                  e.setGravity(false);
                  e.setInvulnerable(true);
                  e.setInteractionWidth(4);
                  e.setInteractionHeight(4);
                  e.setResponsive(true);
                });
    player.showEntity(runtime.plugin, input);
    clicks.replace(input.getUniqueId());
  }

  void click(Entity entity, boolean secondary, long tick) {
    if (!active || age < 40 || input == null || !clicks.accept(entity.getUniqueId(), tick)) return;
    emit(secondary ? WorldMapInput.Type.SECONDARY : WorldMapInput.Type.PRIMARY);
  }

  void scroll(int delta) {
    if (!active || !started) return;
    int next = Math.clamp(zoom + delta, 0, definition().maxZoom());
    if (next == zoom) return;
    zoom = next;
    render();
    emit(WorldMapInput.Type.ZOOM);
  }

  private void emit(WorldMapInput.Type type) {
    var point = cursor();
    var region = WorldMapGeometry.hit(definition(), frame, point);
    try {
      handler.accept(new WorldMapInput(type, point, region, zoom, revision, System.nanoTime()));
    } catch (RuntimeException error) {
      runtime.failure(this, error);
    }
  }

  boolean mountedOn(Entity entity) {
    return seat == entity;
  }

  /** Diagnostics have no effect on rendering, input or application state. */
  public Map<String, Object> snapshot() {
    check();
    var data = new LinkedHashMap<String, Object>();
    data.put("view", frame.viewId());
    data.put("zoom", zoom);
    data.put("revision", revision);
    data.put("referenceYaw", reference);
    data.put("cursor", cursor());
    data.put("age", age);
    data.put("started", started);
    data.put("map", definition().id());
    data.put("seat", seat == null ? null : seat.getEntityId());
    data.put("input", input == null ? null : input.getEntityId());
    data.put("display", display == null ? null : display.getEntityId());
    data.put(
        "dynamicDisplays", dynamicDisplays.values().stream().map(Entity::getEntityId).toList());
    data.put("layerStates", frame.layerStates());
    data.put("glyphs", frame.visibleLayers().size());
    data.put("hudSurfaces", frame.hud().surfaces().size());
    return data;
  }

  @Override
  public void close() {
    runtime.owner.mainThread();
    finish(true);
  }

  void finish(boolean restoreLocation) {
    if (!active) return;
    active = false;
    runtime.remove(this);
    tasks.close();
    viewTasks.close();
    dispose(() -> player.hideBossBar(hud));
    if (display != null) dispose(display::remove);
    for (var entity : dynamicDisplays.values()) dispose(entity::remove);
    dynamicDisplays.clear();
    if (input != null) dispose(input::remove);
    if (seat != null) {
      dispose(seat::eject);
      dispose(seat::remove);
    }
    if (started && player.isOnline()) {
      dispose(
          () -> {
            if (originalWeather == null) player.resetPlayerWeather();
            else player.setPlayerWeather(originalWeather);
          });
      dispose(() -> player.getInventory().setHeldItemSlot(originalSlot));
      if (restoreLocation && !player.isDead())
        dispose(
            () -> {
              ownTeleport = true;
              try {
                player.teleport(original);
              } finally {
                ownTeleport = false;
              }
            });
    }
    if (appearance != null) dispose(appearance::close);
    dispose(closed);
  }

  private void dispose(Runnable action) {
    try {
      action.run();
    } catch (RuntimeException error) {
      runtime
          .plugin
          .getLogger()
          .log(java.util.logging.Level.WARNING, "World-map cleanup callback failed", error);
    }
  }
}
