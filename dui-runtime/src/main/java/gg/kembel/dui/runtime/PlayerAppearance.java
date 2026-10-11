package gg.kembel.dui.runtime;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;

/** Resolved appearance; rendering performs no profile lookup or inventory access. */
public record PlayerAppearance(PlayerHeadObjectContents head, boolean slim, List<NativeItem> armor) {
  public PlayerAppearance {
    Objects.requireNonNull(head);
    if (armor.size() != 4) throw new IllegalArgumentException("Armor order: head, chest, legs, feet");
    armor = Collections.unmodifiableList(new ArrayList<>(armor));
  }

  public static PlayerAppearance skinResource(String resource, boolean slim, List<NativeItem> armor) {
    return new PlayerAppearance(ObjectContents.playerHead().texture(Key.key(resource)).hat(false).build(), slim, armor);
  }
}
