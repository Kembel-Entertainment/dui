package gg.kembel.dui.minestom;

import gg.kembel.dui.runtime.NativeItem;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.event.HoverEvent;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.CustomModelData;

/** Minestom items are immutable; modifying carrier fields retains every other component. */
public record MinestomItem(ItemStack stack) implements NativeItem {
  public MinestomItem { java.util.Objects.requireNonNull(stack); }
  public static MinestomItem of(ItemStack stack) { return new MinestomItem(stack); }
  public Key model() { return Key.key(stack.get(DataComponents.ITEM_MODEL, stack.material().key().asString())); }
  public boolean empty() { return stack.isAir() || stack.amount() == 0; }
  public Key armorAsset() { var data = stack.get(DataComponents.EQUIPPABLE); return data == null || data.assetId() == null ? null : Key.key(data.assetId()); }
  public List<Integer> colors() {
    var data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
    return data == null ? List.of() : data.colors().stream().map(c -> (c.red() << 16) | (c.green() << 8) | c.blue()).toList();
  }
  public MinestomItem withModel(Key model) { return of(stack.with(DataComponents.ITEM_MODEL, model.asString())); }
  public MinestomItem withColors(List<Integer> colors) {
    var data = stack.get(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), List.of()));
    return of(stack.with(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(data.floats(), data.flags(), data.strings(),
        colors.stream().map(c -> (net.kyori.adventure.util.RGBLike)new net.minestom.server.color.Color(c)).toList())));
  }
  public MinestomItem withoutGlint() { return of(stack.with(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false)); }
  public HoverEvent<?> hover() { return stack.asHoverEvent(); }
}
