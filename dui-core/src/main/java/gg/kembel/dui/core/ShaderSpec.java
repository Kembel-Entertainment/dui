package gg.kembel.dui.core;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** Consumer-owned, immutable GPU interface. No application opcodes are built into dui. */
public record ShaderSpec(String id, List<Parameter> parameters) {
  public static final int MAX_PARAMETER_BITS = 240;

  public enum Kind {
    INTEGER,
    BOOLEAN,
    ENUM,
    DECIMAL,
    RGB
  }

  public record Parameter(
      String name, Kind kind, double minimum, double maximum, double step, List<String> choices) {
    public Parameter {
      choices = List.copyOf(choices);
      if (name == null
          || !name.matches("[a-zA-Z_][a-zA-Z0-9_]*")
          || kind == null
          || !Double.isFinite(minimum)
          || !Double.isFinite(maximum)
          || maximum < minimum
          || !Double.isFinite(step)
          || step <= 0) throw new IllegalArgumentException("Invalid shader parameter: " + name);
      if (kind == Kind.ENUM
          && (choices.isEmpty() || new HashSet<>(choices).size() != choices.size()))
        throw new IllegalArgumentException("Shader enum requires distinct choices");
      if (kind != Kind.ENUM && !choices.isEmpty())
        throw new IllegalArgumentException("Unexpected enum choices");
      if (kind == Kind.INTEGER
              && (minimum != Math.rint(minimum)
                  || maximum != Math.rint(maximum)
                  || minimum < Integer.MIN_VALUE
                  || maximum > Integer.MAX_VALUE
                  || step != 1)
          || kind == Kind.RGB && (minimum != 0 || maximum != 0xffffff || step != 1)
          || kind == Kind.BOOLEAN && (minimum != 0 || maximum != 1 || step != 1)
          || kind == Kind.DECIMAL
              && (!Float.isFinite((float) minimum)
                  || !Float.isFinite((float) maximum)
                  || !Float.isFinite((float) step)
                  || (float) step == 0)
          || kind == Kind.ENUM && (minimum != 0 || maximum != choices.size() - 1 || step != 1))
        throw new IllegalArgumentException("Invalid range for " + kind);
      double range = (maximum - minimum) / step;
      if (range > 0x3fffffffL || Math.abs(range - Math.rint(range)) > 1e-6)
        throw new IllegalArgumentException("Shader range exceeds transport or is not quantized");
    }

    public static Parameter integer(String name, int min, int max) {
      return new Parameter(name, Kind.INTEGER, min, max, 1, List.of());
    }

    public static Parameter bool(String name) {
      return new Parameter(name, Kind.BOOLEAN, 0, 1, 1, List.of());
    }

    public static Parameter enumeration(String name, String... values) {
      return new Parameter(name, Kind.ENUM, 0, values.length - 1, 1, List.of(values));
    }

    public static Parameter decimal(String name, double min, double max, double step) {
      return new Parameter(name, Kind.DECIMAL, min, max, step, List.of());
    }

    public static Parameter rgb(String name) {
      return new Parameter(name, Kind.RGB, 0, 0xffffff, 1, List.of());
    }

    public int bits() {
      long range = Math.round((maximum - minimum) / step);
      return Math.max(1, 64 - Long.numberOfLeadingZeros(range));
    }

    public int encode(Object value) {
      if (kind == Kind.BOOLEAN) {
        if (!(value instanceof Boolean b))
          throw new IllegalArgumentException(name + " requires a boolean");
        return b ? 1 : 0;
      }
      if (kind == Kind.ENUM) {
        int i = choices.indexOf(value);
        if (i < 0) throw new IllegalArgumentException(name + " has unknown enum value");
        return i;
      }
      if (kind == Kind.RGB && value instanceof String s && s.matches("#[0-9a-fA-F]{6}"))
        value = Integer.parseInt(s.substring(1), 16);
      if (!(value instanceof Number n))
        throw new IllegalArgumentException(name + " requires a number");
      double v = n.doubleValue(), encoded = (v - minimum) / step;
      if (!Double.isFinite(v)
          || v < minimum
          || v > maximum
          || Math.abs(encoded - Math.rint(encoded)) > 1e-6)
        throw new IllegalArgumentException(name + " is outside its range/quantization");
      return (int) Math.round(encoded);
    }

    public Object decode(int encoded) {
      if (encoded < 0 || encoded > Math.round((maximum - minimum) / step))
        throw new IllegalArgumentException("Invalid encoded parameter");
      return switch (kind) {
        case BOOLEAN -> encoded != 0;
        case ENUM -> choices.get(encoded);
        case DECIMAL -> minimum + encoded * step;
        case INTEGER, RGB -> (int) (minimum + encoded);
      };
    }
  }

  public ShaderSpec {
    parameters = List.copyOf(parameters);
    if (id == null
        || !id.matches("[a-z][a-z0-9_-]*:[a-z][a-z0-9_/-]*")
        || parameters.stream().map(Parameter::name).distinct().count() != parameters.size()
        || parameters.stream().mapToInt(Parameter::bits).sum() > MAX_PARAMETER_BITS)
      throw new IllegalArgumentException("Invalid shader specification: " + id);
  }

  public int bits() {
    return parameters.stream().mapToInt(Parameter::bits).sum();
  }

  public Map<String, Object> validate(Map<String, Object> values) {
    if (!values
        .keySet()
        .equals(
            parameters.stream().map(Parameter::name).collect(java.util.stream.Collectors.toSet())))
      throw new IllegalArgumentException("Shader parameter keys do not match " + id);
    var copy = new LinkedHashMap<String, Object>();
    for (var p : parameters) copy.put(p.name(), p.decode(p.encode(values.get(p.name()))));
    return Collections.unmodifiableMap(copy);
  }

  public String hash() {
    try {
      var bytes = new ByteArrayOutputStream();
      try (var out = new DataOutputStream(bytes)) {
        out.writeUTF(id);
        out.writeInt(parameters.size());
        for (var p : parameters) {
          out.writeUTF(p.name());
          out.writeUTF(p.kind().name());
          out.writeDouble(p.minimum());
          out.writeDouble(p.maximum());
          out.writeDouble(p.step());
          out.writeInt(p.choices().size());
          for (var choice : p.choices()) out.writeUTF(choice);
        }
      }
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    } catch (java.security.NoSuchAlgorithmException | IOException e) {
      throw new AssertionError(e);
    }
  }
}
