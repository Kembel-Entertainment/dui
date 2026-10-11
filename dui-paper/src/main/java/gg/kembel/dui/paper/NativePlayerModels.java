package gg.kembel.dui.paper;
import gg.kembel.dui.core.Canvas;
import net.kyori.adventure.text.Component;

public final class NativePlayerModels {
  private NativePlayerModels() {}
  public static Component band(Canvas.PlayerModel model, PlayerAppearance appearance, int band, boolean motion) {
    return gg.kembel.dui.runtime.NativePlayerModels.band(model, appearance.portable(), band, motion);
  }
}
