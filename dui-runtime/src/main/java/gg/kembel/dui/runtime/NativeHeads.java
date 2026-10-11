package gg.kembel.dui.runtime;

import gg.kembel.dui.core.Canvas;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.object.ObjectContents;

public final class NativeHeads {
  private NativeHeads() {}
  public static Component render(Canvas.Head head, UiViewer viewer) {
    var builder = ObjectContents.playerHead().hat(head.hat());
    String source = head.source();
    if (source.equals("self") && viewer != null) builder.skin(target -> {
      var snapshot = viewer.head();
      target.id(snapshot.id()).name(snapshot.name()).profileProperties(snapshot.profileProperties());
      if (snapshot.texture() != null) target.texture(snapshot.texture());
    });
    else if (source.startsWith("texture:")) builder.texture(Key.key(source.substring(8)));
    else if (source.startsWith("uuid:")) builder.id(UUID.fromString(source.substring(5)));
    else if (!source.equals("self") && source.matches("[A-Za-z0-9_]{1,16}")) builder.name(source);
    else builder.texture(Key.key("minecraft:entity/player/wide/steve"));
    return Component.object(builder.build()).color(NamedTextColor.WHITE).shadowColor(ShadowColor.none());
  }
}
