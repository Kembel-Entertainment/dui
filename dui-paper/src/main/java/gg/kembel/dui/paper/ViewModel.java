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
    Map<String, URI> links) {
  public ViewModel {
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

  public static ViewModel data(Map<String, Object> data) {
    return new ViewModel(data, Map.of(), Map.of(), Map.of());
  }
}
