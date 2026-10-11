package gg.kembel.dui.minestom;

import gg.kembel.dui.runtime.*;
import java.util.*;
import java.util.function.Consumer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.*;
import net.minestom.server.entity.*;
import net.minestom.server.entity.metadata.display.*;
import net.minestom.server.entity.metadata.other.InteractionMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ChangeGameStatePacket;

/** Private native Minestom entities. Every mutation holds ownership of the viewer. */
final class MinestomSpatialView implements SpatialView {
  private static final ThreadLocal<Set<Player>> MUTATING = ThreadLocal.withInitial(HashSet::new);
  static boolean mutating(Player player) { return MUTATING.get().contains(player); }
  private void mutate(Runnable action) {
    var players = MUTATING.get(); boolean added = players.add(player);
    try { action.run(); } finally { if (added) players.remove(player); if (players.isEmpty()) MUTATING.remove(); }
  }
  private final Player player;
  private final Map<String, Entity> text = new LinkedHashMap<>();
  private final BossBar hud = BossBar.bossBar(Component.empty(), 1, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
  private Pos original, anchor;
  private Instance instance;
  private byte slot;
  private Entity seat, input;
  private MinestomAppearanceMask appearance;
  MinestomSpatialView(Player player) { this.player = player; }
  public boolean begin(boolean resetLook, Integer anchorSlot, Runnable abort) {
    if (player.getInstance() == null || player.getVehicle() != null || player.isDead() || player.isFlying()
        || player.isFlyingWithElytra() || player.getEntityMeta().isSwimming() || player.getPose() == EntityPose.SLEEPING) {
      player.sendMessage(Component.text("Stand on the ground outside a vehicle to open the display.")); return false;
    }
    original = player.getPosition(); instance = player.getInstance(); slot = player.getHeldSlot();
    seat = entity(EntityType.ITEM_DISPLAY, original.add(0, .6, 0), e -> {});
    if (resetLook) mutate(() -> player.setView(original.yaw(), 0));
    seat.addPassenger(player);
    appearance = new MinestomAppearanceMask(player);
    MinestomAppearanceMask.sendRaw(player, new ChangeGameStatePacket(ChangeGameStatePacket.Reason.END_RAINING, 0));
    MinestomAppearanceMask.sendRaw(player, new ChangeGameStatePacket(ChangeGameStatePacket.Reason.RAIN_LEVEL_CHANGE, 0));
    MinestomAppearanceMask.sendRaw(player, new ChangeGameStatePacket(ChangeGameStatePacket.Reason.THUNDER_LEVEL_CHANGE, 0));
    if (anchorSlot != null) player.setHeldItemSlot((byte)(int)anchorSlot);
    double radians = Math.toRadians(original.yaw());
    anchor = player.getPosition().add(-Math.sin(radians), player.getEyeHeight(), Math.cos(radians)).withView(original.yaw(), 0);
    player.showBossBar(hud); return true;
  }
  private Entity entity(EntityType type, Pos position, Consumer<Entity> configure) {
    var entity = new Entity(type); entity.setAutoViewable(false); entity.setNoGravity(true);
    configure.accept(entity);
    // All presentation entities stay in the already loaded viewer chunk.
    var placed = entity.setInstance(instance, position);
    if (!placed.isDone()) { entity.remove(); throw new IllegalStateException("Display chunk must be loaded before opening a spatial UI"); }
    placed.join(); entity.addViewer(player); return entity;
  }
  public boolean valid() { return !player.isDead() && seat != null && !seat.isRemoved() && player.getVehicle() == seat && player.getInstance() == instance; }
  public boolean dead() { return player.isDead(); }
  public float yaw() { return player.getPosition().yaw(); }
  public float pitch() { return player.getPosition().pitch(); }
  public long gameTime() { return instance == null ? 0 : instance.getWorldAge(); }
  public void text(String id, Component contents, float translationY, int opacity, boolean overlay) {
    var display = text.get(id);
    if (display == null) {
      display = entity(EntityType.TEXT_DISPLAY, anchor, e -> {
        var meta = (TextDisplayMeta)e.getEntityMeta();
        meta.setWidth(8); meta.setHeight(8); meta.setViewRange(1); meta.setBrightness(15, 15);
        meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.FIXED);
        meta.setShadow(false); meta.setSeeThrough(overlay); meta.setUseDefaultBackground(false); meta.setBackgroundColor(0);
        meta.setLineWidth(1_000_000); meta.setScale(new Vec(1e-8f, 1e-8f, 1e-8f));
        meta.setTranslation(new Vec(0, translationY, 0)); meta.setTextOpacity((byte)opacity); meta.setText(contents);
      });
      text.put(id, display);
    } else {
      var meta = (TextDisplayMeta)display.getEntityMeta();
      var translation = new Vec(0, translationY, 0);
      if (!meta.getTranslation().equals(translation)) meta.setTranslation(translation);
      if (meta.getTextOpacity() != (byte)opacity) meta.setTextOpacity((byte)opacity);
      if (!meta.getText().equals(contents)) meta.setText(contents);
    }
  }
  public void removeText(String id) { var entity = text.remove(id); if (entity != null) entity.remove(); }
  public UUID replaceInput() {
    if (input != null) input.remove();
    input = entity(EntityType.INTERACTION, player.getPosition().add(0, player.getEyeHeight() - 2, 0), e -> {
      var meta = (InteractionMeta)e.getEntityMeta(); meta.setWidth(4); meta.setHeight(4); meta.setResponse(true);
    });
    return input.getUuid();
  }
  public void hud(Component contents) { if (!hud.name().equals(contents)) hud.name(contents); }
  public MapVideoConnection video(int[] maps, Consumer<Throwable> failure) { return new MinestomMapVideoBridge(player, maps, failure); }
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
    if (seat != null) { dispose(() -> seat.removePassenger(player)); dispose(seat::remove); seat = null; }
    if (appearance != null) { dispose(appearance::close); appearance = null; }
    if (original != null && player.isOnline()) {
      dispose(() -> player.setHeldItemSlot(slot));
      if (player.getInstance() != null) {
        var weather = player.getInstance().getWeather();
        dispose(() -> player.sendPacket(weather.createIsRainingPacket()));
        dispose(() -> player.sendPacket(weather.createRainLevelPacket()));
        dispose(() -> player.sendPacket(weather.createThunderLevelPacket()));
      }
      if (restore && !player.isDead() && player.getInstance() == instance) dispose(() -> mutate(() -> player.teleport(original)));
    }
  }
  private void dispose(Runnable cleanup) {
    try { cleanup.run(); } catch (RuntimeException error) { System.getLogger(getClass().getName()).log(System.Logger.Level.WARNING, "dui spatial cleanup failed", error); }
  }
}
