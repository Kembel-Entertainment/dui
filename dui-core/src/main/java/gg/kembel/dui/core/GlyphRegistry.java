package gg.kembel.dui.core;

import java.util.*;

/** Deterministic, pack-owned glyph addresses; all pictures are supplied by consumers. */
public final class GlyphRegistry {
  private GlyphRegistry() {}

  public static Map<String, GlyphBinding> bind(Collection<GlyphSpec> specs) {
    if (specs.size() > 256) throw new IllegalArgumentException("Glyph registry capacity 256");
    var result = new TreeMap<String, GlyphBinding>();
    int code = 0xEB00;
    for (var spec : specs.stream().sorted(Comparator.comparing(GlyphSpec::id)).toList())
      if (result.putIfAbsent(spec.id(), new GlyphBinding(spec, code++)) != null)
        throw new IllegalArgumentException("Duplicate glyph " + spec.id());
    return Collections.unmodifiableMap(result);
  }

  public static void validate(Map<String, GlyphBinding> bindings) {
    var codes = new HashSet<Integer>();
    for (var entry : bindings.entrySet())
      if (!entry.getKey().equals(entry.getValue().specification().id())
          || !codes.add(entry.getValue().codePoint()))
        throw new IllegalArgumentException("Glyph registry collision/mismatch: " + entry.getKey());
  }

  public static GlyphBinding require(Map<String, GlyphBinding> bindings, String id) {
    var result = bindings.get(id);
    if (result == null) throw new IllegalArgumentException("Unregistered glyph: " + id);
    return result;
  }
}
