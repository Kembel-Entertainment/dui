package gg.kembel.dui.paper;

import gg.kembel.dui.core.*;
import java.util.*;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

/** Immutable, cloned armor transports share the native body ordering with item/effect carriers. */
final class PlayerArmor {
  record Carrier(Canvas.PlayerModel model, int slot, int flags, ItemStack stack) {}

  static Map<String, Carrier> carriers(Canvas canvas, Map<String, PlayerAppearance> appearances) {
    var result = new LinkedHashMap<String, Carrier>();
    for (var model : canvas.playerModels) {
      var appearance =
          Objects.requireNonNull(
              appearances.get(model.source()), "Missing player appearance: " + model.source());
      var armor = appearance.armor();
      for (int slot = 0; slot < 4; slot++) {
        var stack = armor.get(slot);
        if (stack == null || stack.getType().isAir()) continue;
        var equippable =
            stack.getData(io.papermc.paper.datacomponent.DataComponentTypes.EQUIPPABLE);
        if (equippable == null
            || equippable.assetId() == null
            || !equippable.assetId().namespace().equals("minecraft")) continue;
        String material = equippable.assetId().value();
        if (!Set.of(
                "leather",
                "chainmail",
                "copper",
                "iron",
                "gold",
                "diamond",
                "netherite",
                "turtle_scute")
            .contains(material)) continue;
        var clone = stack.clone();
        var meta = clone.getItemMeta();
        meta.setItemModel(
            NamespacedKey.fromString(PlayerModelCodec.armorModel(material, slot == 2)));
        meta.setEnchantmentGlintOverride(false);
        clone.setItemMeta(meta);
        String id = "__player_armor/" + model.id() + "/" + slot;
        result.put(
            id,
            new Carrier(
                model,
                slot,
                PlayerModelCodec.flags(model, appearance.slim(), canvas.motionEnabled)
                    | (slot << 8),
                clone));
      }
    }
    return result;
  }
}
