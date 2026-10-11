package gg.kembel.dui.paper;

import gg.kembel.dui.core.*;
import java.net.URI;
import java.util.*;
import org.bukkit.inventory.ItemStack;

/** Values are supplied by the application; image keys never fetch URLs on the client. */
public record ViewModel(
    Map<String, Object> data,
    Map<String, RasterImage> images,
    Map<String, ItemStack> items,
    Map<String, URI> links,
    Map<String, PlayerAppearance> appearances) {
  public ViewModel(
      Map<String, Object> data,
      Map<String, RasterImage> images,
      Map<String, ItemStack> items,
      Map<String, URI> links) {
    this(data, images, items, links, Map.of());
  }

  public ViewModel {
    appearances = Map.copyOf(appearances);
    data = Map.copyOf(data);
    images = Map.copyOf(images);
    var cloned = new HashMap<String, ItemStack>();
    items.forEach((key, value) -> cloned.put(key, value.clone()));
    items = Collections.unmodifiableMap(cloned);
    links = Map.copyOf(links);
    for (var uri : links.values())
      if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null)
        throw new IllegalArgumentException("Only HTTP(S) UI links are supported");
  }

  /** Returns defensive item snapshots; mutating a result never changes this view. */
  @Override
  public Map<String, ItemStack> items() {
    var copy = new HashMap<String, ItemStack>();
    items.forEach((key, item) -> copy.put(key, item.clone()));
    return Collections.unmodifiableMap(copy);
  }

  /** Resource checks happen before pack prompts; projection never resolves a skin. */
  public void validate(Canvas canvas) {
    for (var model : canvas.renderPlan().playerModels)
      if (!appearances.containsKey(model.source()))
        throw new IllegalArgumentException("Missing player appearance: " + model.source());
  }

  public static ViewModel data(Map<String, Object> data) {
    return new ViewModel(data, Map.of(), Map.of(), Map.of());
  }

  public gg.kembel.dui.runtime.ViewModel portable() {
    var nativeItems = new HashMap<String, gg.kembel.dui.runtime.NativeItem>();
    items.forEach((key, item) -> nativeItems.put(key, PaperItem.of(item)));
    var players = new HashMap<String, gg.kembel.dui.runtime.PlayerAppearance>();
    appearances.forEach((key, appearance) -> players.put(key, appearance.portable()));
    return new gg.kembel.dui.runtime.ViewModel(data, images, nativeItems, links, players);
  }

  static ViewModel fromPortable(gg.kembel.dui.runtime.ViewModel model) {
    var items = new HashMap<String, ItemStack>();
    model.items().forEach((key, item) -> items.put(key, ((PaperItem)item).stack()));
    var appearances = new HashMap<String, PlayerAppearance>();
    model.appearances().forEach((key, appearance) -> appearances.put(key, PlayerAppearance.fromPortable(appearance)));
    return new ViewModel(model.data(), model.images(), items, model.links(), appearances);
  }
}
