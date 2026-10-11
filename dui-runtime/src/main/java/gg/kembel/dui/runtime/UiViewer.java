package gg.kembel.dui.runtime;

import gg.kembel.dui.core.UiScheduler;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;

/** A connection's UI execution context. Methods other than scheduler.execute run on its owner. */
public interface UiViewer {
  UUID id();
  boolean online();
  boolean isOwnerThread();
  void checkThread();
  UiScheduler scheduler();
  PlayerHeadObjectContents head();
  void message(Component message);
}
