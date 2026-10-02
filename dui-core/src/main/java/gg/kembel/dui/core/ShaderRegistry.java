package gg.kembel.dui.core;

import java.util.*;

/** Deterministic pack ABI assignment; consumers refer to IDs, not numerical addresses. */
public final class ShaderRegistry {
  private ShaderRegistry() {}

  public static Map<String, ShaderBinding> bind(Collection<ShaderSpec> specifications) {
    var specs = new TreeMap<String, ShaderSpec>();
    for (var spec : specifications)
      if (specs.putIfAbsent(spec.id(), spec) != null)
        throw new IllegalArgumentException("Duplicate shader " + spec.id());
    if (specs.size() > 64)
      throw new IllegalArgumentException("Shader profile supports 64 definitions");
    var out = new LinkedHashMap<String, ShaderBinding>();
    int code = 0;
    for (var spec : specs.values()) out.put(spec.id(), new ShaderBinding(spec, code++));
    return Collections.unmodifiableMap(out);
  }

  public static void validate(Map<String, ShaderBinding> bindings) {
    var codes = new HashSet<Integer>();
    for (var e : bindings.entrySet())
      if (!e.getKey().equals(e.getValue().specification().id()) || !codes.add(e.getValue().code()))
        throw new IllegalArgumentException("Invalid shader registry");
  }

  public static ShaderBinding require(Map<String, ShaderBinding> bindings, ShaderSpec spec) {
    var binding = bindings.get(spec.id());
    if (binding == null || !binding.specification().equals(spec))
      throw new IllegalArgumentException("Pack lacks matching shader schema: " + spec.id());
    return binding;
  }
}
