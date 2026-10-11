package gg.kembel.dui.paper;

import gg.kembel.dui.runtime.NativeItem;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

/** Immutable Paper item snapshot. Carrier changes preserve unrelated item components. */
public final class PaperItem implements NativeItem {
  private final ItemStack stack;
  private PaperItem(ItemStack stack) { this.stack = stack.clone(); }
  public static PaperItem of(ItemStack stack) { return new PaperItem(java.util.Objects.requireNonNull(stack)); }
  public ItemStack stack() { return stack.clone(); }
  public Key model() {
    var meta = stack.getItemMeta();
    return meta != null && meta.hasItemModel() ? meta.getItemModel() : stack.getType().getKey();
  }
  public boolean empty() { return stack.isEmpty(); }
  public Key armorAsset() {
    // ItemMeta.hasEquippable only sees an explicit override, not vanilla defaults.
    var equipment = stack.getData(io.papermc.paper.datacomponent.DataComponentTypes.EQUIPPABLE);
    return equipment == null ? null : equipment.assetId();
  }
  public List<Integer> colors() {
    var meta = stack.getItemMeta();
    return meta == null ? List.of() : meta.getCustomModelDataComponent().getColors().stream().map(Color::asRGB).toList();
  }
  public PaperItem withModel(Key model) {
    var copy = stack(); var meta = copy.getItemMeta();
    meta.setItemModel(new NamespacedKey(model.namespace(), model.value())); copy.setItemMeta(meta); return of(copy);
  }
  public PaperItem withColors(List<Integer> colors) {
    var copy = stack(); var meta = copy.getItemMeta(); var data = meta.getCustomModelDataComponent();
    data.setColors(colors.stream().map(Color::fromRGB).toList()); meta.setCustomModelDataComponent(data); copy.setItemMeta(meta); return of(copy);
  }
  public PaperItem withoutGlint() {
    var copy = stack(); var meta = copy.getItemMeta(); meta.setEnchantmentGlintOverride(false); copy.setItemMeta(meta); return of(copy);
  }
  public HoverEvent<?> hover() { return stack.asHoverEvent(); }
}
