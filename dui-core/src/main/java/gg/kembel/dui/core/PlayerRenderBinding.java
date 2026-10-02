package gg.kembel.dui.core;

import java.util.*;

public record PlayerRenderBinding(PlayerRenderSpec specification, int code) {
  public PlayerRenderBinding {
    if (specification == null || code < 0 || code > 15)
      throw new IllegalArgumentException("Player renderer address");
  }

  public static Map<String, PlayerRenderBinding> bind(Collection<PlayerRenderSpec> consumers) {
    var specs = new TreeMap<String, PlayerRenderSpec>();
    specs.put("dui:player", PlayerRenderSpec.standard());
    for (var spec : consumers)
      if (specs.putIfAbsent(spec.id(), spec) != null)
        throw new IllegalArgumentException("Duplicate player renderer: " + spec.id());
    if (specs.size() > 16) throw new IllegalArgumentException("Player renderer capacity");
    var result = new TreeMap<String, PlayerRenderBinding>();
    result.put("dui:player", new PlayerRenderBinding(specs.remove("dui:player"), 0));
    int index = 1;
    for (var spec : specs.values()) result.put(spec.id(), new PlayerRenderBinding(spec, index++));
    return Collections.unmodifiableMap(result);
  }
}
