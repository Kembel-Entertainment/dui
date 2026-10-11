package gg.kembel.dui.runtime;

import gg.kembel.dui.core.Canvas;
import gg.kembel.dui.core.RasterImage;
import java.net.URI;
import java.util.Map;
import java.util.Set;

/** Shared immutable projection resources; native values are provided through adapter snapshots. */
public record ViewModel(Map<String, Object> data, Map<String, RasterImage> images,
                        Map<String, NativeItem> items, Map<String, URI> links,
                        Map<String, PlayerAppearance> appearances) {
  public ViewModel {
    data = Map.copyOf(data); images = Map.copyOf(images); items = Map.copyOf(items);
    links = Map.copyOf(links); appearances = Map.copyOf(appearances);
    for (var uri : links.values())
      if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null)
        throw new IllegalArgumentException("Only HTTP(S) UI links are supported");
  }
  public ViewModel(Map<String, Object> data, Map<String, RasterImage> images,
                   Map<String, NativeItem> items, Map<String, URI> links) {
    this(data, images, items, links, Map.of());
  }
  public static ViewModel data(Map<String, Object> data) {
    return new ViewModel(data, Map.of(), Map.of(), Map.of(), Map.of());
  }
  public void validate(Canvas canvas) {
    for (var model : canvas.renderPlan().playerModels)
      if (!appearances.containsKey(model.source()))
        throw new IllegalArgumentException("Missing player appearance: " + model.source());
  }
}
