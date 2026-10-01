package gg.kembel.dui.paper;

import gg.kembel.dui.core.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.*;
import net.kyori.adventure.text.object.ObjectContents;

/**
 * Native profile glyphs carry the complete live skin texture. Never stores skin pixels in a pack.
 */
public final class NativePlayerModels {
  private NativePlayerModels() {}

  public static Component band(
      Canvas.PlayerModel model, PlayerAppearance appearance, int band, boolean motion) {
    var builder = ObjectContents.playerHead().hat(false);
    if (appearance.profile() == null || appearance.fallback())
      builder.texture(appearance.texture());
    else {
      var profile = appearance.profile();
      if (profile
          instanceof net.kyori.adventure.text.object.PlayerHeadObjectContents.SkinSource skin)
        builder.skin(skin);
      else {
        var adapted =
            org.bukkit.Bukkit.createProfileExact(profile.getUniqueId(), profile.getName());
        adapted.setTextures(profile.getTextures());
        builder.skin(adapted);
      }
    }
    return Component.object(builder.build())
        .color(
            TextColor.color(
                PlayerModelCodec.color(
                    PlayerModelCodec.flags(model, appearance.slim(), motion), band)))
        .shadowColor(ShadowColor.shadowColor(0));
  }
}
