package gg.kembel.dui.pack;

import java.util.*;

/** Trusted build-time package contributions. Runtime templates cannot inject shader code. */
public record PackContribution(String owner, Map<String, byte[]> resources, List<Effect> effects) {
  public record Effect(String id, int code, String function, String glsl) {
    public Effect {
      if (id == null
          || !id.matches("[a-z][a-z0-9_-]*:[a-z][a-z0-9_/-]*")
          || code < 8
          || code > 15
          || function == null
          || !function.matches("[a-zA-Z_][a-zA-Z0-9_]*")
          || glsl == null
          || glsl.length() > 32768)
        throw new IllegalArgumentException("Invalid effect contribution");
    }
  }

  public PackContribution {
    if (owner == null || !owner.matches("[a-z][a-z0-9_-]*"))
      throw new IllegalArgumentException("Contribution owner");
    var copy = new TreeMap<String, byte[]>();
    resources.forEach(
        (k, v) -> {
          if (!k.startsWith("assets/" + owner + "/") || k.contains(".."))
            throw new IllegalArgumentException("Resources must belong to " + owner);
          copy.put(k, v.clone());
        });
    resources = Collections.unmodifiableMap(copy);
    effects = List.copyOf(effects);
    for (var e : effects)
      if (!e.id().startsWith(owner + ":"))
        throw new IllegalArgumentException("Effect owner mismatch");
  }

  @Override
  public Map<String, byte[]> resources() {
    var result = new TreeMap<String, byte[]>();
    resources.forEach((k, v) -> result.put(k, v.clone()));
    return Collections.unmodifiableMap(result);
  }

  public static List<Effect> merge(
      List<PackContribution> contributions, Map<String, byte[]> assets) {
    var ids = new HashSet<String>();
    var codes = new HashSet<Integer>();
    var functions = new HashSet<String>();
    var result = new ArrayList<Effect>();
    for (var c : contributions) {
      c.resources()
          .forEach(
              (k, v) -> {
                if (assets.putIfAbsent(k, v.clone()) != null)
                  throw new IllegalArgumentException("Pack resource collision: " + k);
              });
      for (var effect : c.effects()) {
        if (!ids.add(effect.id()) || !codes.add(effect.code()) || !functions.add(effect.function()))
          throw new IllegalArgumentException("Effect contribution collision: " + effect.id());
        result.add(effect);
      }
    }
    return List.copyOf(result);
  }
}
