package gg.kembel.dui.core;

import java.util.*;

public final class GlyphFont {
  private final Map<Integer, Integer> widths = new HashMap<>();

  /** Small ASCII fallback for standalone layouts. Production uses the generated pack metrics. */
  public GlyphFont() {
    for (int cp = 32; cp < 127; cp++) widths.put(cp, 6);
    " \"()*I[]t{}".codePoints().forEach(cp -> widths.put(cp, 4));
    "!',.:;i|".codePoints().forEach(cp -> widths.put(cp, 2));
    "<>fk".codePoints().forEach(cp -> widths.put(cp, 5));
    "@~".codePoints().forEach(cp -> widths.put(cp, 7));
    "`l".codePoints().forEach(cp -> widths.put(cp, 3));
  }

  public GlyphFont(Map<String, Integer> metrics) {
    metrics.forEach(
        (character, advance) -> {
          if (character.codePointCount(0, character.length()) != 1 || advance < 0 || advance > 32)
            throw new IllegalArgumentException("Invalid font metric");
          widths.put(character.codePointAt(0), advance);
        });
    if (!widths.containsKey((int) '?'))
      throw new IllegalArgumentException("Missing fallback glyph");
  }

  public Map<String, Integer> metrics() {
    var result = new TreeMap<String, Integer>();
    widths.forEach((cp, w) -> result.put(new String(Character.toChars(cp)), w));
    return Collections.unmodifiableMap(result);
  }

  public int width(String text) {
    return text.codePoints().map(cp -> widths.getOrDefault(cp, widths.get((int) '?'))).sum();
  }

  public String fit(String text, int available) {
    StringBuilder supported = new StringBuilder();
    text.codePoints()
        .forEach(
            cp ->
                supported.appendCodePoint(
                    widths.containsKey(cp) && !Character.isISOControl(cp) ? cp : '?'));
    String result = supported.toString();
    if (width(result) <= available) return result;
    int budget = Math.max(0, available - width("..."));
    StringBuilder clipped = new StringBuilder();
    int used = 0;
    for (int cp : result.codePoints().toArray()) {
      int next = widths.get(cp);
      if (used + next > budget) break;
      clipped.appendCodePoint(cp);
      used += next;
    }
    return clipped + "...";
  }

  public static String shift(int pixels) {
    if (Math.abs(pixels) > 2047) throw new IllegalArgumentException("Shift too large");
    StringBuilder text = new StringBuilder();
    int value = Math.abs(pixels);
    for (int bit = 10; bit >= 0; bit--)
      if ((value & (1 << bit)) != 0) text.append((char) ((pixels < 0 ? 0xE900 : 0xE800) + bit));
    return text.toString();
  }
}
