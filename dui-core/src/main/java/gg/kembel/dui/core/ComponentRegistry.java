package gg.kembel.dui.core;

import java.util.*;

/** Immutable consumer-owned components. Each compiled template keeps its registry snapshot. */
public final class ComponentRegistry {
  public static final ComponentRegistry EMPTY = new ComponentRegistry(Map.of());

  @FunctionalInterface
  public interface Renderer {
    void draw(ComponentContext context);
  }

  @FunctionalInterface
  public interface Measurer {
    Measure.Size measure(Measure.Context context);
  }

  record Definition(
      Set<String> attributes,
      int height,
      Renderer renderer,
      String template,
      PropertySchema schema,
      Measurer measurer,
      ComponentContract contract) {
    Definition(
        Set<String> attributes,
        int height,
        Renderer renderer,
        String template,
        PropertySchema schema) {
      this(attributes, height, renderer, template, schema, null, ComponentContract.scalar(schema));
    }

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

  /** Exportable public contracts for consumer documentation and editor/LLM tooling. */
  public Map<String, ComponentContract> contracts() {
    var result = new TreeMap<String, ComponentContract>();
    definitions.forEach((id, definition) -> result.put(id, definition.contract()));
    return Collections.unmodifiableMap(result);
  }

  public Map<String, Object> describe() {
    var result = new TreeMap<String, Object>();
    contracts()
        .forEach(
            (id, contract) -> {
              var values = new TreeMap<String, Object>();
              contract
                  .values()
                  .forEach(
                      (name, value) ->
                          values.put(
                              name,
                              Map.of(
                                  "type",
                                  value.codec().description(),
                                  "required",
                                  value.required(),
                                  "default",
                                  value.defaultValue() == null
                                      ? "<absent>"
                                      : value.defaultValue())));
              result.put(id, Map.of("scalars", contract.scalars().properties(), "values", values));
            });
    return Collections.unmodifiableMap(result);
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
      return add(
          name,
          new Definition(
              properties,
              18,
              null,
              Objects.requireNonNull(source),
              PropertySchema.strings(properties, true)));
    }

    /** The renderer composes existing Canvas primitives; it does not register a GPU opcode. */
    public Builder renderer(
        String name, Set<String> attributes, int naturalHeight, Renderer renderer) {
      if (naturalHeight < 1 || naturalHeight > 360)
        throw new IllegalArgumentException("Component height must be 1..360");
      return add(
          name,
          new Definition(
              attributes,
              naturalHeight,
              Objects.requireNonNull(renderer),
              null,
              PropertySchema.strings(attributes, false)));
    }

    public Builder renderer(String name, PropertySchema schema, int height, Renderer renderer) {
      if (height < 1 || height > 360) throw new IllegalArgumentException("Component height");
      return add(
          name,
          new Definition(
              schema.properties().keySet(),
              height,
              Objects.requireNonNull(renderer),
              null,
              schema));
    }

    public Builder template(String name, PropertySchema schema, String source) {
      return add(
          name,
          new Definition(
              schema.properties().keySet(), 18, null, Objects.requireNonNull(source), schema));
    }

    /** Intrinsic size and allocation are separate; renderer receives final bounds. */
    public Builder renderer(
        String name, ComponentContract contract, Measurer measurer, Renderer renderer) {
      return add(
          name,
          new Definition(
              contract.attributes(),
              18,
              Objects.requireNonNull(renderer),
              null,
              contract.scalars(),
              Objects.requireNonNull(measurer),
              contract));
    }

    private Builder add(String name, Definition definition) {
      if (!name.matches("[a-z][a-z0-9]*(?:-[a-z0-9]+)+"))
        throw new IllegalArgumentException(
            "Use an owner-prefixed component name, e.g. acme-widget");
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
