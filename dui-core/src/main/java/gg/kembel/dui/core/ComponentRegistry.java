package gg.kembel.dui.core;

import java.util.*;

/** Immutable consumer-owned components. Each compiled template keeps its registry snapshot. */
public final class ComponentRegistry {
  public static final ComponentRegistry EMPTY = new ComponentRegistry(Map.of());

  @FunctionalInterface
  public interface Renderer {
    void draw(ComponentContext context);
  }

  record Definition(Set<String> attributes, int height, Renderer renderer, String template) {
    Definition {
      attributes = Set.copyOf(attributes);
    }
  }

  private final Map<String, Definition> definitions;

  private ComponentRegistry(Map<String, Definition> definitions) {
    this.definitions = Map.copyOf(definitions);
  }

  Map<String, Definition> definitions() {
    return definitions;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private final Map<String, Definition> definitions = new LinkedHashMap<>();

    /** Combines immutable packages, rejecting name collisions before compilation. */
    public Builder include(ComponentRegistry registry) {
      registry.definitions.forEach(this::add);
      return this;
    }

    /** Source must contain one dui-fragment root and one visual root inside it. */
    public Builder template(String name, Set<String> properties, String source) {
      return add(name, new Definition(properties, 18, null, Objects.requireNonNull(source)));
    }

    /** The renderer composes existing Canvas primitives; it does not register a GPU opcode. */
    public Builder renderer(
        String name, Set<String> attributes, int naturalHeight, Renderer renderer) {
      if (naturalHeight < 1 || naturalHeight > 360)
        throw new IllegalArgumentException("Component height must be 1..360");
      return add(
          name, new Definition(attributes, naturalHeight, Objects.requireNonNull(renderer), null));
    }

    private Builder add(String name, Definition definition) {
      if (!name.matches("[a-z][a-z0-9]*(?:-[a-z0-9]+)+"))
        throw new IllegalArgumentException("Use an owner-prefixed component name, e.g. acme-card");
      for (String attribute : definition.attributes())
        if (!attribute.matches("[a-z][a-z0-9-]*")
            || attribute.equals("name")
            || attribute.equals("props"))
          throw new IllegalArgumentException("Invalid component property: " + attribute);
      if (definitions.putIfAbsent(name, definition) != null)
        throw new IllegalArgumentException("Duplicate component: " + name);
      return this;
    }

    public ComponentRegistry build() {
      return new ComponentRegistry(definitions);
    }
  }
}
