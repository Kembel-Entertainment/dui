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
    return bodies(canvas, stacks, models, true);
  }

  public static List<DialogBody> bodies(
      Canvas canvas, Map<String, ItemStack> stacks, Set<String> models, boolean motionTracks) {
    canvas = canvas.renderPlan();
    var batches = EffectBatches.of(canvas);
    var renderItems = new ArrayList<Canvas.Item>(canvas.items);
    if (canvas.animation != null) {
      String id = canvas.animation.itemId();
      int at = -1;
      for (int j = 0; j < renderItems.size(); j++)
        if (renderItems.get(j).id().equals(id)) {
          at = j;
          break;
        }
      if (at >= 0) {
        renderItems.remove(at);
        for (int j = 0; j < batches.size(); j++)
          renderItems.add(at + j, new Canvas.Item(id + "/batch/" + j, 0, 0, 1));
      }
    }
    var result = new ArrayList<DialogBody>();
    if (renderItems.isEmpty()) return result;
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
    for (int i = 0; i < renderItems.size(); i++) {
      var item = renderItems.get(i);
      boolean animation =
          canvas.animation != null && item.id().startsWith(canvas.animation.itemId() + "/batch/");
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
      var motion = motionTracks ? canvas.motions.get(item.id()) : null;
      var transition = motion == null ? canvas.transitions.get(item.id()) : null;
      boolean clipped = canvas.clips.containsKey(item.id());
      if (clipped && transition == null && motion == null)
        transition = new ItemTransition(ItemTransition.Kind.SLIDE, 0, 1, 0, false);
      if (transition != null && (confetti || animation))
        throw new IllegalArgumentException(
            "Item transition must have its own carrier: " + item.id());
      var cmd = meta.getCustomModelDataComponent();
      var colors = new ArrayList<>(cmd.getColors());
      while (colors.size()
          < ItemTransport.DATA_INDEX
              + ItemTransport.NATIVE_CELLS
              + (animation ? ShaderEffect.LIMIT * 24 : 0)) colors.add(Color.BLACK);
      // FocusableTextWidget: 4px padding. Text width = canvas.width+2.
      // Each following item body occupies 1px plus the vanilla 10px gap.
      int flags =
          animation
              ? 1
                  | (canvas.animation.motion() && canvas.motionEnabled ? 4 : 0)
                  | (canvas.animation.startedAt() >= 0 ? 8 : 0)
              : item.size();
      var payload =
          new ArrayList<>(
              ItemTransport.payload(
                  item.x() - canvas.width / 2,
                  item.y() - canvas.height - 14 - (i + 1) * 11,
                  flags,
                  confetti || animation || transition != null || motion != null));
      if (confetti)
        payload.addAll(
            ItemTransport.confettiPayload(
                canvas.confetti.startedAt(), canvas.width, canvas.height, item.x(), item.y()));
      if (transition != null)
        payload.addAll(ItemTransport.transitionPayload(canvas, item, transition));
      if (motion != null) payload.addAll(ItemTransport.motionPayload(canvas, item, motion));
      if (animation) {
        int batch = Integer.parseInt(item.id().substring(item.id().lastIndexOf('/') + 1));
        payload.addAll(ItemTransport.animationPayload(canvas, batches.get(batch)));
      }
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
