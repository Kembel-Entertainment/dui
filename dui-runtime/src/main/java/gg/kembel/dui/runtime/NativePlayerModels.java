package gg.kembel.dui.runtime;

import gg.kembel.dui.core.Canvas;
import gg.kembel.dui.core.PlayerModelCodec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;

public final class NativePlayerModels {
  private NativePlayerModels() {}
  public static Component band(Canvas.PlayerModel model, PlayerAppearance appearance, int band, boolean motion) {
    return Component.object(appearance.head().toBuilder().hat(false).build())
        .color(TextColor.color(PlayerModelCodec.color(PlayerModelCodec.flags(model, appearance.slim(), motion), band, model.rendererCode())))
        .shadowColor(ShadowColor.none());
  }
}
