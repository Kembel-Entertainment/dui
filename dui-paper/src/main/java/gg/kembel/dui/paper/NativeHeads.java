package gg.kembel.dui.paper;

import gg.kembel.dui.core.Canvas;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.*;
import net.kyori.adventure.text.object.ObjectContents;
import org.bukkit.entity.Player;

/** Native player objects keep live skin/profile data out of the generated resource pack. */
public final class NativeHeads {
  private NativeHeads() {}

  public static Component render(Canvas.Head head, Player viewer) {
    var builder = ObjectContents.playerHead().hat(head.hat());
    String source = head.source();
    if (source.equals("self") && viewer != null) builder.skin(viewer.getPlayerProfile());
    else if (source.startsWith("texture:")) builder.texture(Key.key(source.substring(8)));
    else if (source.startsWith("uuid:")) builder.id(UUID.fromString(source.substring(5)));
    else if (!source.equals("self") && source.matches("[A-Za-z0-9_]{1,16}")) builder.name(source);
    else builder.texture(Key.key("minecraft:entity/player/wide/steve"));
    return Component.object(builder.build())
        .color(NamedTextColor.WHITE)
        .shadowColor(ShadowColor.shadowColor(0));
  }
}
