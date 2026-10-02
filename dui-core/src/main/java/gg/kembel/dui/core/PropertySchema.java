package gg.kembel.dui.core;

import java.util.*;

/** Consumer property contract, resolved after bindings and before rendering. */
public record PropertySchema(Map<String, Property> properties) {
  public enum Type {
    STRING,
    INTEGER,
    DECIMAL,
    BOOLEAN,
    ENUM,
    COLOR,
    RESOURCE
  }

  public record Property(
      Type type,
      boolean required,
      String defaultValue,
      double min,
      double max,
      Set<String> choices) {
    public Property {
      Objects.requireNonNull(type);
      choices = Set.copyOf(choices);
      if (min > max || !Double.isFinite(min) || !Double.isFinite(max))
        throw new IllegalArgumentException("Property range");
      if (type == Type.ENUM && choices.isEmpty())
        throw new IllegalArgumentException("Enum choices");
      if (defaultValue != null) validate(defaultValue, type, min, max, choices);
    }

    public static Property string(boolean required, String value) {
      return new Property(Type.STRING, required, value, 0, 0, Set.of());
    }

    public static Property integer(int min, int max, String value) {
      return new Property(Type.INTEGER, value == null, value, min, max, Set.of());
    }

    public static Property decimal(double min, double max, String value) {
      return new Property(Type.DECIMAL, value == null, value, min, max, Set.of());
    }

    public static Property bool(String value) {
      return new Property(Type.BOOLEAN, value == null, value, 0, 1, Set.of());
    }

    public static Property enumeration(Set<String> choices, String value) {
      return new Property(Type.ENUM, value == null, value, 0, 0, choices);
    }

    public static Property color(String value) {
      return new Property(Type.COLOR, value == null, value, 0, 0, Set.of());
    }

    public static Property resource(String value) {
      return new Property(Type.RESOURCE, value == null, value, 0, 0, Set.of());
    }

    String accept(String value) {
      validate(value, type, min, max, choices);
      return value;
    }
  }

  public PropertySchema {
    properties = Map.copyOf(properties);
    for (String name : properties.keySet())
      if (!name.matches("[a-z][a-z0-9-]*"))
        throw new IllegalArgumentException("Property name: " + name);
  }

  public static PropertySchema strings(Set<String> names, boolean required) {
    var p = new HashMap<String, Property>();
    for (var name : names) p.put(name, Property.string(required, null));
    return new PropertySchema(p);
  }

  public Map<String, String> resolve(Map<String, String> values) {
    var out = new HashMap<>(values);
    properties.forEach(
        (name, property) -> {
          String value = values.getOrDefault(name, property.defaultValue());
          if (value == null) {
            if (property.required()) throw new IllegalArgumentException("Missing property " + name);
          } else out.put(name, property.accept(value));
        });
    return Map.copyOf(out);
  }

  private static void validate(
      String value, Type type, double min, double max, Set<String> choices) {
    boolean ok =
        switch (type) {
          case STRING -> true;
          case BOOLEAN -> value.equals("true") || value.equals("false");
          case ENUM -> choices.contains(value);
          case COLOR -> value.matches("#[0-9a-fA-F]{6}") || value.matches("\\$[a-z][a-z0-9_-]*");
          case RESOURCE -> value.matches("[a-z][a-z0-9_-]*:[a-z0-9_/-]+");
          case INTEGER, DECIMAL -> {
            try {
              double n = Double.parseDouble(value);
              yield Double.isFinite(n)
                  && n >= min
                  && n <= max
                  && (type != Type.INTEGER || n == Math.rint(n) && value.matches("[+-]?[0-9]+"));
            } catch (NumberFormatException e) {
              yield false;
            }
          }
        };
    if (!ok) throw new IllegalArgumentException("Invalid " + type + " property value: " + value);
  }
}
