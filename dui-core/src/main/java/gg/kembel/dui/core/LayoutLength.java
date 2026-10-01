package gg.kembel.dui.core;

import java.util.regex.*;

/** A bounded dimension: integer pixels, fill, a percentage, or either minus a pixel inset. */
public final class LayoutLength {
  private static final Pattern RELATIVE = Pattern.compile("(fill|[0-9]{1,3}%)(?:-([0-9]+))?");

  private LayoutLength() {}

  public static int resolve(String expression, int available) {
    var match = RELATIVE.matcher(expression);
    if (!match.matches()) return Integer.parseInt(expression);
    int percent =
        match.group(1).equals("fill") ? 100 : Integer.parseInt(match.group(1).replace("%", ""));
    if (percent > 100 || available < 0)
      throw new IllegalArgumentException("Relative dimension must fit 0..100% of its parent");
    long result = (long) available * percent / 100;
    if (match.group(2) != null) result -= Integer.parseInt(match.group(2));
    if (result < 0) throw new IllegalArgumentException("Dimension inset exceeds available space");
    return Math.toIntExact(result);
  }
}
