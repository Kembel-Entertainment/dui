package gg.kembel.dui.core;

import java.util.*;

/** Little-endian bit stream shared by CPU tests, pack ABI and native carrier transport. */
public final class ShaderParameters {
  private ShaderParameters() {}

  public static final class Bits {
    private final List<Integer> bits = new ArrayList<>();

    public void put(int value, int count) {
      if (count < 1 || count > 30 || value < 0 || ((long) value >> count) != 0)
        throw new IllegalArgumentException("Bit field overflow");
      for (int i = 0; i < count; i++) bits.add((value >>> i) & 1);
    }

    public void cells(List<Integer> cells, int count) {
      for (int i = 0; i < count; i++) {
        int rgb = cells.get(i / 3);
        int cell = (rgb >> 16 & 255) > 127 ? 1 : 0;
        if ((rgb >> 8 & 255) > 127) cell |= 2;
        if ((rgb & 255) > 127) cell |= 4;
        bits.add(cell >> (i % 3) & 1);
      }
    }

    public int size() {
      return bits.size();
    }

    public List<Integer> colors() {
      var out = new ArrayList<Integer>();
      for (int i = 0; i < bits.size(); i += 3) {
        int v = 0;
        for (int j = 0; j < 3 && i + j < bits.size(); j++) v |= bits.get(i + j) << j;
        out.add(
            ((v & 1) != 0 ? 0xff0000 : 0)
                | ((v & 2) != 0 ? 0xff00 : 0)
                | ((v & 4) != 0 ? 0xff : 0));
      }
      return List.copyOf(out);
    }

    public int read(int at, int count) {
      int v = 0;
      for (int i = 0; i < count; i++) v |= bits.get(at + i) << i;
      return v;
    }
  }

  public static Bits encode(ShaderSpec spec, Map<String, Object> values) {
    values = spec.validate(values);
    var b = new Bits();
    for (var p : spec.parameters()) b.put(p.encode(values.get(p.name())), p.bits());
    return b;
  }

  public static Map<String, Object> decode(ShaderSpec spec, Bits data) {
    var out = new LinkedHashMap<String, Object>();
    int at = 0;
    for (var p : spec.parameters()) {
      out.put(p.name(), p.decode(data.read(at, p.bits())));
      at += p.bits();
    }
    return Map.copyOf(out);
  }
}
