package gg.kembel.dui.core;

import java.util.*;

/**
 * Consumer-owned immutable registrations: aliases, assets, factory and validation travel together.
 */
public final class MenuCatalogue<T> {
  public record Definition<T>(
      String id, Set<String> aliases, List<String> templates, T factory, Runnable validate) {
    public Definition {
      if (id == null || id.isBlank()) throw new IllegalArgumentException("Menu id");
      aliases = Set.copyOf(aliases);
      templates = List.copyOf(templates);
      Objects.requireNonNull(factory);
      Objects.requireNonNull(validate);
    }
  }

  private final Map<String, Definition<T>> names;
  private final List<Definition<T>> definitions;

  public MenuCatalogue(List<Definition<T>> definitions) {
    this.definitions = List.copyOf(definitions);
    var map = new LinkedHashMap<String, Definition<T>>();
    for (var d : definitions) {
      for (var name : union(d))
        if (map.putIfAbsent(name, d) != null)
          throw new IllegalArgumentException("Duplicate menu name: " + name);
    }
    names = Map.copyOf(map);
  }

  private static <T> Set<String> union(Definition<T> d) {
    var names = new HashSet<>(d.aliases());
    names.add(d.id());
    return names;
  }

  public Optional<Definition<T>> find(String name) {
    return Optional.ofNullable(names.get(name));
  }

  public List<Definition<T>> definitions() {
    return definitions;
  }

  public List<String> templates() {
    return definitions.stream().flatMap(d -> d.templates().stream()).distinct().toList();
  }

  public void validate() {
    definitions.forEach(d -> d.validate().run());
  }
}
