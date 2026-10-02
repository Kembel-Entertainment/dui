package gg.kembel.dui.core.world;

import java.util.*;

/** Read-only application projection. Changing viewId invalidates the old input entity. */
public record WorldMapFrame(
    String viewId,
    List<String> visibleLayers,
    List<Region> regions,
    WorldHud hud,
    Map<String, WorldLayerState> layerStates) {
  public WorldMapFrame(
      String viewId, List<String> visibleLayers, List<Region> regions, WorldHud hud) {
    this(viewId, visibleLayers, regions, hud, Map.of());
  }

  public record Region(String id, String layer, String action, String value, boolean enabled) {
    public Region {
      if (id == null
          || id.isBlank()
          || layer == null
          || layer.isBlank()
          || action == null
          || action.isBlank()
          || value == null) throw new IllegalArgumentException("Invalid world-map region");
    }
  }

  public WorldMapFrame {
    if (viewId == null || viewId.isBlank()) throw new IllegalArgumentException("Missing view id");
    visibleLayers = List.copyOf(visibleLayers);
    regions = List.copyOf(regions);
    layerStates = Map.copyOf(layerStates);
    Objects.requireNonNull(hud);
    if (visibleLayers.stream().distinct().count() != visibleLayers.size()
        || regions.stream().map(Region::id).distinct().count() != regions.size())
      throw new IllegalArgumentException("Duplicate frame layer/region");
  }

  public void validate(WorldMapDefinition definition) {
    visibleLayers.forEach(definition::layer);
    layerStates.forEach(
        (id, state) -> {
          if (definition.layer(id).space() != WorldMapDefinition.Space.MAP)
            throw new IllegalArgumentException("Runtime geometry currently uses map-space layers");
        });
    for (var region : regions) {
      var layer = definition.layer(region.layer());
      if (!visibleLayers.contains(layer.id()) || layer.space() != WorldMapDefinition.Space.MAP)
        throw new IllegalArgumentException("Hit region needs a visible map-space layer");
    }
  }
}
