package gg.kembel.dui.runtime;

import gg.kembel.dui.core.*;
import java.util.*;
import java.util.function.Function;
import net.kyori.adventure.key.Key;

/** Shared native item/effect/armor carrier generation; adapters only snapshot and lower items. */
public final class ShaderItems {
  private ShaderItems() {}
  private record Armor(Canvas.PlayerModel model, int flags, NativeItem item) {}

  public static List<DialogBody> bodies(Canvas source, ViewModel resources, PackMetadata metadata,
                                        Function<Key, NativeItem> factory) {
    return bodies(source, resources, metadata.models(), metadata.shaders(), factory);
  }
  public static List<DialogBody> bodies(Canvas source, ViewModel resources, Set<String> models,
      Map<String, ShaderBinding> shaders, Function<Key, NativeItem> factory) {
    var canvas = source.renderPlan();
    var batches = EffectBatches.of(canvas);
    var items = new ArrayList<>(canvas.items);
    if (canvas.animation != null) {
      String id = canvas.animation.itemId();
      for (int i = 0; i < items.size(); i++) if (items.get(i).id().equals(id)) {
        items.remove(i);
        for (int j = 0; j < batches.size(); j++) items.add(i + j, new Canvas.Item(id + "/batch/" + j, 0, 0, 1));
        break;
      }
    }
    var armor = new LinkedHashMap<String, Armor>();
    for (var model : canvas.playerModels) {
      var appearance = Objects.requireNonNull(resources.appearances().get(model.source()), "Missing player appearance: " + model.source());
      for (int slot = 0; slot < 4; slot++) {
        var stack = appearance.armor().get(slot);
        if (stack == null || stack.empty() || stack.armorAsset() == null || !stack.armorAsset().namespace().equals("minecraft")) continue;
        String material = stack.armorAsset().value();
        if (!Set.of("leather", "chainmail", "copper", "iron", "gold", "diamond", "netherite", "turtle_scute").contains(material)) continue;
        String id = "__player_armor/" + model.id() + "/" + slot;
        if (items.stream().anyMatch(item -> item.id().equals(id))) throw new IllegalArgumentException("Reserved armor carrier id");
        var item = stack.withModel(Key.key(PlayerModelCodec.armorModel(material, slot == 2))).withoutGlint();
        armor.put(id, new Armor(model, PlayerModelCodec.flags(model, appearance.slim(), canvas.motionEnabled) | (slot << 8), item));
        items.add(new Canvas.Item(id, model.x(), model.y(), 1));
      }
    }
    if (items.isEmpty()) return List.of();
    var result = new ArrayList<DialogBody>();
    result.add(new DialogBody.Item(factory.apply(Key.key("minecraft:paper")).withModel(Key.key("dui:layer_fence"))));
    for (int i = 0; i < items.size(); i++) {
      var item = items.get(i);
      boolean animation = canvas.animation != null && item.id().startsWith(canvas.animation.itemId() + "/batch/");
      var carrier = armor.get(item.id());
      NativeItem stack = carrier != null ? carrier.item()
          : animation ? factory.apply(Key.key("minecraft:paper")).withModel(Key.key("dui:effect/panel"))
          : Objects.requireNonNull(resources.items().get(item.id()), "Missing native stack: " + item.id());
      var original = stack.model();
      if (!models.contains(original.asString())) throw new IllegalArgumentException("No native model wrapper registered for " + original);
      stack = stack.withModel(Key.key("dui", "live/" + original.namespace() + "/" + original.value()));
      var motion = canvas.motions.get(item.id());
      if (canvas.clips.containsKey(item.id()) && motion == null)
        motion = new Motion(0, 1, 0, Motion.Easing.LINEAR, false, 0, 0, 1, 1, 0, 0, 1, 1, .5, .5);
      var colors = new ArrayList<>(stack.colors());
      while (colors.size() < ItemTransport.DATA_INDEX + ItemTransport.NATIVE_CELLS + (animation ? ShaderInvocation.LIMIT * 24 : 0)) colors.add(0);
      int flags = animation ? 1 | (canvas.animation.motion() && canvas.motionEnabled ? 4 : 0) | (canvas.animation.startedAt() >= 0 ? 8 : 0) : item.size();
      var payload = new ArrayList<>(ItemTransport.payload(item.x() - canvas.width / 2,
          item.y() - canvas.height - 14 - (i + 1) * 11, flags, animation || motion != null || carrier != null));
      if (motion != null) payload.addAll(ItemTransport.motionPayload(canvas, item, motion));
      if (animation) {
        int batch = Integer.parseInt(item.id().substring(item.id().lastIndexOf('/') + 1));
        payload.addAll(ItemTransport.animationPayload(canvas, batches.get(batch), shaders));
      }
      if (carrier != null) {
        var model = carrier.model();
        payload.addAll(ItemTransport.headerPayload(0, model.width(), model.height(), 0, 0));
        int code = carrier.flags() | (model.rendererCode() << 10);
        for (int j = 0; j < 6; j++) {
          int bits = (code >> (j * 3)) & 7;
          payload.add(((bits & 1) != 0 ? 0xFF0000 : 0) | ((bits & 2) != 0 ? 0x00FF00 : 0) | ((bits & 4) != 0 ? 0x0000FF : 0));
        }
      }
      for (int k = 0; k < payload.size(); k++) colors.set(ItemTransport.DATA_INDEX + k, payload.get(k));
      result.add(new DialogBody.Item(stack.withColors(colors)));
    }
    return List.copyOf(result);
  }
}
