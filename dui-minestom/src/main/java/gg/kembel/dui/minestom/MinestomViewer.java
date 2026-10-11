package gg.kembel.dui.minestom;

import gg.kembel.dui.core.UiScheduler;
import gg.kembel.dui.runtime.UiViewer;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.ResolvableProfile;
import net.minestom.server.timer.TaskSchedule;

public final class MinestomViewer implements UiViewer {
  private final Player player;
  public MinestomViewer(Player player) { this.player = java.util.Objects.requireNonNull(player); }
  public Player player() { return player; }
  public UUID id() { return player.getUuid(); }
  public boolean online() { return player.isOnline(); }
  public boolean isOwnerThread() { return player.acquirable().isOwned(); }
  public void checkThread() { if (!isOwnerThread()) throw new IllegalStateException("dui Minestom API requires ownership of the player; use execute() or player.acquirable().sync()"); }
  /** Provider completions enter this player's next tick. */
  public void execute(Runnable task) { scheduler().execute(task); }
  public UiScheduler scheduler() {
    return new UiScheduler() {
      public Cancellation later(long ticks, Runnable task) {
        checkThread();
        if (ticks < 0 || ticks > Integer.MAX_VALUE) throw new IllegalArgumentException("Tick delay");
        var scheduled = player.scheduler().scheduleTask(task, TaskSchedule.tick((int)Math.max(1, ticks)), TaskSchedule.stop());
        return scheduled::cancel;
      }
      public void execute(Runnable task) { player.scheduler().execute(() -> { if (player.isOnline()) task.run(); }); }
    };
  }
  public PlayerHeadObjectContents head() {
    checkThread(); var builder = ObjectContents.playerHead().id(id()).name(player.getUsername());
    var skin = player.getSkin(); if (skin != null) builder.skin(new ResolvableProfile(skin));
    else builder.texture(net.kyori.adventure.key.Key.key("minecraft:entity/player/wide/steve"));
    return builder.build();
  }
  public void message(Component message) { checkThread(); player.sendMessage(message); }
}
