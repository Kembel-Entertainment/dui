package gg.kembel.dui.core;

import java.util.*;
import java.util.function.Function;

/** Consumer-defined, typed binding conversion. No expression or script evaluation. */
public interface ValueCodec<T> {
  T decode(Object value);

  String description();

  static <T> ValueCodec<T> of(String description, Function<Object, T> decode) {
    Objects.requireNonNull(decode);
    if (description == null || description.isBlank())
      throw new IllegalArgumentException("Codec description");
    return new ValueCodec<>() {
      public T decode(Object value) {
        return Objects.requireNonNull(decode.apply(value));
      }

      public String description() {
        return description;
      }
    };
  }

  static ValueCodec<String> string() {
    return of(
        "string",
        v -> {
          if (!(v instanceof String s)) throw new IllegalArgumentException("Expected string");
          return s;
        });
  }

  static <T> ValueCodec<List<T>> list(ValueCodec<T> element, int maximum) {
    if (maximum < 0 || maximum > 512) throw new IllegalArgumentException("List capacity");
    return of(
        "list<" + element.description() + "> <= " + maximum,
        value -> {
          if (!(value instanceof List<?> values) || values.size() > maximum)
            throw new IllegalArgumentException("Expected bounded list");
          return values.stream().map(element::decode).toList();
        });
  }

  static ValueCodec<Map<String, Object>> object() {
    return of(
        "immutable string-keyed object",
        value -> {
          if (!(value instanceof Map<?, ?> map))
            throw new IllegalArgumentException("Expected object");
          return freezeMap(map, 0);
        });
  }

  private static Map<String, Object> freezeMap(Map<?, ?> source, int depth) {
    if (depth > 32 || source.size() > 512)
      throw new IllegalArgumentException("Binding object budget");
    var out = new LinkedHashMap<String, Object>();
    source.forEach(
        (key, value) -> {
          if (!(key instanceof String s))
            throw new IllegalArgumentException("Object keys must be strings");
          out.put(s, freeze(value, depth + 1));
        });
    return Collections.unmodifiableMap(out);
  }

  private static Object freeze(Object value, int depth) {
    if (depth > 32) throw new IllegalArgumentException("Binding depth");
    if (value instanceof Map<?, ?> map) return freezeMap(map, depth);
    if (value instanceof List<?> list) {
      if (list.size() > 512) throw new IllegalArgumentException("Binding list budget");
      return list.stream().map(v -> freeze(v, depth + 1)).toList();
    }
    // Number is extensible: AtomicInteger/AtomicLong and arbitrary subclasses
    // must not retain mutable state in an otherwise immutable binding snapshot.
    if (value instanceof String
        || value instanceof Byte
        || value instanceof Short
        || value instanceof Integer
        || value instanceof Long
        || value instanceof Float
        || value instanceof Double
        || (value instanceof java.math.BigInteger && value.getClass() == java.math.BigInteger.class)
        || (value instanceof java.math.BigDecimal && value.getClass() == java.math.BigDecimal.class)
        || value instanceof Boolean) return value;
    throw new IllegalArgumentException("Unsupported structured binding value");
  }
}
