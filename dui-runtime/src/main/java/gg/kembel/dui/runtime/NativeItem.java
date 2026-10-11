package gg.kembel.dui.runtime;

import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.event.HoverEvent;

/** Immutable native snapshot. Implementations retain every platform item data component. */
public interface NativeItem {
  Key model();
  boolean empty();
  Key armorAsset();
  List<Integer> colors();
  NativeItem withModel(Key model);
  NativeItem withColors(List<Integer> colors);
  NativeItem withoutGlint();
  HoverEvent<?> hover();
}
