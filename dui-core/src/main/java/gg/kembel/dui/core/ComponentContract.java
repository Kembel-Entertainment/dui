package gg.kembel.dui.core;

import java.util.*;

/** Scalar template properties plus typed values preserved across exact bindings. */
public record ComponentContract(PropertySchema scalars, Map<String, Value> values) {
  public record Value(ValueCodec<?> codec, boolean required, Object defaultValue) {
    public Value {
      Objects.requireNonNull(codec);
      if (defaultValue != null) defaultValue = codec.decode(defaultValue);
    }
  }

  public ComponentContract {
    Objects.requireNonNull(scalars);
    values = Map.copyOf(values);
    for (String name : values.keySet())
      if (!name.matches("[a-z][a-z0-9-]*") || scalars.properties().containsKey(name))
        throw new IllegalArgumentException("Duplicate/invalid structured property: " + name);
  }

  public static ComponentContract scalar(PropertySchema schema) {
    return new ComponentContract(schema, Map.of());
  }

  public Set<String> attributes() {
    var result = new HashSet<>(scalars.properties().keySet());
    result.addAll(values.keySet());
    return Set.copyOf(result);
  }

  public Map<String, Object> resolve(Map<String, Object> supplied) {
    var result = new LinkedHashMap<String, Object>();
    values.forEach(
        (name, definition) -> {
          Object value = supplied.getOrDefault(name, definition.defaultValue());
          if (value == null) {
            if (definition.required())
              throw new IllegalArgumentException("Missing property " + name);
          } else result.put(name, definition.codec().decode(value));
        });
    return Map.copyOf(result);
  }
}
