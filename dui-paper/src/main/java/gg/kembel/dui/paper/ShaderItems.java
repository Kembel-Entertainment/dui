package gg.kembel.dui.paper;

import gg.kembel.dui.core.*;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import java.util.*;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

/**
 * Native render carriers follow the text canvas. Visuals are relocated; hit tests stay in the text.
 */
public final class ShaderItems {
  private ShaderItems() {}

  public static List<DialogBody> bodies(
      Canvas canvas, Map<String, ItemStack> stacks, Set<String> models) {
    var result = new ArrayList<DialogBody>();
    if (canvas.items.isEmpty()) return result;
    var fence = new ItemStack(org.bukkit.Material.PAPER);
    var fenceMeta = fence.getItemMeta();
    fenceMeta.setItemModel(new NamespacedKey("dui", "layer_fence"));
    fence.setItemMeta(fenceMeta);
    result.add(
        DialogBody.item(fence)
            .width(1)
            .height(1)
            .showDecorations(false)
            .showTooltip(false)
            .build());
    for (int i = 0; i < canvas.items.size(); i++) {
      var item = canvas.items.get(i);
      boolean animation = canvas.animation != null && canvas.animation.itemId().equals(item.id());
      var stack =
          animation
              ? new ItemStack(org.bukkit.Material.PAPER)
              : Objects.requireNonNull(stacks.get(item.id()), "Missing native stack: " + item.id())
                  .clone();
      if (animation) {
        var effectMeta = stack.getItemMeta();
        effectMeta.setItemModel(new NamespacedKey("dui", "effect/panel"));
        stack.setItemMeta(effectMeta);
      }
      var meta = stack.getItemMeta();
      var original = meta.hasItemModel() ? meta.getItemModel() : stack.getType().getKey();
      if (!models.contains(original.toString()))
        throw new IllegalArgumentException("No native model wrapper registered for " + original);
      meta.setItemModel(
          new NamespacedKey("dui", "live/" + original.getNamespace() + "/" + original.getKey()));
      boolean confetti = canvas.confetti != null && canvas.confetti.itemId().equals(item.id());
      var cmd = meta.getCustomModelDataComponent();
      var colors = new ArrayList<>(cmd.getColors());
      while (colors.size()
          < ItemTransport.DATA_INDEX
              + 28
              + (animation ? ShaderEffect.LIMIT * ShaderEffect.CELLS : 0)) colors.add(Color.BLACK);
      // FocusableTextWidget: 4px padding. Text width = canvas.width+2.
      // Each following item body occupies 1px plus the vanilla 10px gap.
      int flags =
          animation
              ? 1
                  | (canvas.animation.motion() ? 4 : 0)
                  | (canvas.animation.startedAt() >= 0 ? 8 : 0)
              : item.size();
      var payload =
          new ArrayList<>(
              ItemTransport.payload(
                  item.x() - canvas.width / 2,
                  item.y() - canvas.height - 14 - (i + 1) * 11,
                  flags,
                  confetti || animation));
      if (confetti)
        payload.addAll(
            ItemTransport.confettiPayload(
                canvas.confetti.startedAt(), canvas.width, canvas.height, item.x(), item.y()));
      if (animation) payload.addAll(ItemTransport.animationPayload(canvas));
      for (int k = 0; k < payload.size(); k++)
        colors.set(ItemTransport.DATA_INDEX + k, Color.fromRGB(payload.get(k)));
      cmd.setColors(colors);
      meta.setCustomModelDataComponent(cmd);
      stack.setItemMeta(meta);
      result.add(
          DialogBody.item(stack)
              .width(1)
              .height(1)
              .showDecorations(false)
              .showTooltip(false)
              .build());
    }
    return result;
  }
}
