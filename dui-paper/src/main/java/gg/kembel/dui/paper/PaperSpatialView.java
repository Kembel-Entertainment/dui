package gg.kembel.dui.paper;

import gg.kembel.dui.runtime.*;
import java.util.*;
import java.util.function.Consumer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Paper-only entity, appearance and packet operations for the shared spatial sessions. */
final class PaperSpatialView implements SpatialView {
  private final JavaPlugin plugin;
  private final Player player;
  private final Map<String, TextDisplay> text = new LinkedHashMap<>();
  private final BossBar hud = BossBar.bossBar(Component.empty(), 1, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
  private Location original, anchor;
  private WeatherType weather;
  private int slot;
  private ItemDisplay seat;
  private Interaction input;
  private WorldMapClientAppearance appearance;
  PaperSpatialView(JavaPlugin plugin, Player player) { this.plugin = plugin; this.player = player; }
  public boolean begin(boolean resetLook, Integer anchorSlot, Runnable abort) throws Exception {
    if (player.isInsideVehicle() || player.isDead() || player.isFlying() || player.isGliding() || player.isSleeping() || player.isSwimming()) {
      player.sendMessage("Stand on the ground outside a vehicle to open the display."); return false;
    }
    original = player.getLocation().clone(); slot = player.getInventory().getHeldItemSlot(); weather = player.getPlayerWeather();
    seat = player.getWorld().spawn(original.clone().add(0, .6, 0), ItemDisplay.class, e -> {
      e.setVisibleByDefault(false); e.setPersistent(false); e.setGravity(false); e.setInvulnerable(true); e.setSilent(true);
    });
    player.showEntity(plugin, seat);
    if (resetLook) player.setRotation(original.getYaw(), 0);
    if (!seat.addPassenger(player)) throw new IllegalStateException("Cannot mount private display seat");
    player.setPlayerWeather(WeatherType.CLEAR);
    appearance = new WorldMapClientAppearance(plugin, player, abort);
    if (anchorSlot != null) player.getInventory().setHeldItemSlot(anchorSlot);
    anchor = player.getEyeLocation();
    double radians = Math.toRadians(original.getYaw());
    anchor.add(-Math.sin(radians), 0, Math.cos(radians)); anchor.setYaw(original.getYaw()); anchor.setPitch(0);
    player.showBossBar(hud); return true;
  }
  public boolean valid() { return !player.isDead() && seat != null && seat.isValid() && player.getVehicle() == seat && player.getWorld().equals(original.getWorld()); }
  public boolean dead() { return player.isDead(); }
  public float yaw() { return player.getLocation().getYaw(); }
  public float pitch() { return player.getLocation().getPitch(); }
  public long gameTime() { return player.getWorld().getGameTime(); }
  public void text(String id, Component contents, float translationY, int opacity, boolean overlay) {
    var display = text.get(id);
    if (display == null) {
      display = player.getWorld().spawn(anchor, TextDisplay.class, e -> {
        e.setVisibleByDefault(false); e.setPersistent(false); e.setGravity(false); e.setInvulnerable(true);
        e.setBillboard(Display.Billboard.FIXED); e.setDisplayWidth(8); e.setDisplayHeight(8); e.setViewRange(1);
        e.setBrightness(new Display.Brightness(15, 15)); e.setShadowed(false); e.setSeeThrough(overlay);
        e.setDefaultBackground(false); e.setBackgroundColor(Color.fromARGB(0)); e.setLineWidth(1_000_000);
        e.setTransformation(new Transformation(new Vector3f(0, translationY, 0), new Quaternionf(), new Vector3f(1e-8f), new Quaternionf()));
        e.setTextOpacity((byte)opacity); e.text(contents);
      });
      text.put(id, display); player.showEntity(plugin, display);
    } else {
      var transform = new Transformation(new Vector3f(0, translationY, 0), new Quaternionf(), new Vector3f(1e-8f), new Quaternionf());
      if (!transform.equals(display.getTransformation())) display.setTransformation(transform);
      if (display.getTextOpacity() != (byte)opacity) display.setTextOpacity((byte)opacity);
      if (!display.text().equals(contents)) display.text(contents);
    }
  }
  public void removeText(String id) { var display = text.remove(id); if (display != null) display.remove(); }
  public UUID replaceInput() {
    if (input != null) input.remove();
    input = player.getWorld().spawn(player.getEyeLocation().add(0, -2, 0), Interaction.class, e -> {
      e.setVisibleByDefault(false); e.setPersistent(false); e.setGravity(false); e.setInvulnerable(true);
      e.setInteractionWidth(4); e.setInteractionHeight(4); e.setResponsive(true);
    });
    player.showEntity(plugin, input); return input.getUniqueId();
  }
  public void hud(Component component) { if (!hud.name().equals(component)) hud.name(component); }
  public MapVideoConnection video(int[] mapIds, Consumer<Throwable> failure) throws Exception { return new MapVideoBridge(player, mapIds, failure); }
  public Map<String, Object> diagnostics() {
    var result = new LinkedHashMap<String, Object>();
    result.put("seat", seat == null ? null : seat.getEntityId()); result.put("input", input == null ? null : input.getEntityId());
    result.put("display", text.containsKey("static") ? text.get("static").getEntityId() : null);
    result.put("dynamicDisplays", text.entrySet().stream().filter(e -> !e.getKey().equals("static")).map(e -> e.getValue().getEntityId()).toList());
    return result;
  }
  public void close(boolean restore) {
    dispose(() -> player.hideBossBar(hud));
    for (var entity : text.values()) dispose(entity::remove); text.clear();
    if (input != null) { dispose(input::remove); input = null; }
    if (seat != null) { dispose(seat::eject); dispose(seat::remove); seat = null; }
    if (original != null && player.isOnline()) {
      dispose(() -> { if (weather == null) player.resetPlayerWeather(); else player.setPlayerWeather(weather); });
      dispose(() -> player.getInventory().setHeldItemSlot(slot));
      if (restore && !player.isDead()) dispose(() -> player.teleport(original));
    }
    if (appearance != null) { dispose(appearance::close); appearance = null; }
  }
  private void dispose(Runnable cleanup) {
    try { cleanup.run(); } catch (RuntimeException error) { plugin.getLogger().log(java.util.logging.Level.WARNING, "dui spatial cleanup failed", error); }
  }
}
