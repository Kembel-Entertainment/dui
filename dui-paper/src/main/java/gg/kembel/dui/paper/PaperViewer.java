package gg.kembel.dui.paper;

import gg.kembel.dui.core.UiScheduler;
import gg.kembel.dui.runtime.UiViewer;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PaperViewer implements UiViewer {
  private final Player player;
  private final UiScheduler scheduler;
  PaperViewer(Player player, UiScheduler scheduler) { this.player = java.util.Objects.requireNonNull(player); this.scheduler = scheduler; }
  public Player player() { return player; }
  public UUID id() { return player.getUniqueId(); }
  public boolean online() { return player.isOnline(); }
  public boolean isOwnerThread() { return Bukkit.isPrimaryThread(); }
  public void checkThread() { if (!isOwnerThread()) throw new IllegalStateException("dui Paper API must run on the server thread"); }
  public UiScheduler scheduler() { return scheduler; }
  public PlayerHeadObjectContents head() { checkThread(); return ObjectContents.playerHead().skin(player.getPlayerProfile()).build(); }
  public void message(Component message) { checkThread(); player.sendMessage(message); }
}
