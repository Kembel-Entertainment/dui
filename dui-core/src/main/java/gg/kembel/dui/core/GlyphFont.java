package gg.kembel.dui.core;

import java.util.*;

public final class GlyphFont {
  private final Map<Integer, Integer> widths = new HashMap<>();

  public GlyphFont(Map<String, Integer> metrics) {
    this(metrics, 32);
  }

  public static GlyphFont from(BitmapFont font) {
    var metrics = new HashMap<String, Integer>();
    font.glyphs().forEach((key, value) -> metrics.put(key, value.advance()));
    return new GlyphFont(metrics, 128);
  }

  private GlyphFont(Map<String, Integer> metrics, int maximum) {
    metrics.forEach(
        (character, advance) -> {
          if (character.codePointCount(0, character.length()) != 1
              || advance < 0
              || advance > maximum) throw new IllegalArgumentException("Invalid font metric");
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
    String ellipsis = "...";
    while (!ellipsis.isEmpty() && width(ellipsis) > available) ellipsis = ellipsis.substring(1);
    int budget = Math.max(0, available - width(ellipsis));
    StringBuilder clipped = new StringBuilder();
    int used = 0;
    for (int cp : result.codePoints().toArray()) {
      int next = widths.get(cp);
      if (used + next > budget) break;
      clipped.appendCodePoint(cp);
      used += next;
    }
    return clipped + ellipsis;
  }

  /** Word wrapping with code-point-safe long-word splits and truncation on the last line. */
  public List<String> wrap(String text, int available, int maxLines) {
    if (available < 1 || maxLines < 1)
      throw new IllegalArgumentException("Text wrap needs positive width and lines");
    String remaining = text.replaceAll("\\s+", " ").strip();
    var lines = new ArrayList<String>();
    while (!remaining.isEmpty() && lines.size() < maxLines) {
      if (lines.size() == maxLines - 1 || width(remaining) <= available) {
        lines.add(fit(remaining, available));
        break;
      }
      int end = 0, used = 0;
      while (end < remaining.length()) {
        int cp = remaining.codePointAt(end), next = widths.getOrDefault(cp, widths.get((int) '?'));
        if (used + next > available) break;
        used += next;
        end += Character.charCount(cp);
      }
      if (end == 0) {
        lines.add(fit(remaining, available));
        break;
      }
      int space = remaining.lastIndexOf(' ', end);
      if (space > 0) end = space;
      lines.add(remaining.substring(0, end));
      remaining = remaining.substring(end).stripLeading();
    }
    return List.copyOf(lines);
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
