package gg.kembel.dui.core;

import java.util.*;

/**
 * Validates spanning cells before returning any geometry. All vertical units are nine-pixel rows.
 */
public final class GridLayout {
  public record Cell(String key, int column, int row, int columns, int rows) {}

  public record Box(String key, int x, int y, int width, int height) {}

  private GridLayout() {}

  public static List<Box> place(
      List<Cell> cells,
      int columns,
      int rows,
      int x,
      int y,
      int columnStride,
      int rowStride,
      int gapX,
      int gapY) {
    if (columns < 1
        || rows < 1
        || columnStride < 1
        || rowStride < 9
        || rowStride % 9 != 0
        || y % 9 != 0
        || gapX < 0
        || gapY < 0
        || gapY % 9 != 0) throw new IllegalArgumentException("Grid geometry");
    var keys = new HashSet<String>();
    var used = new HashSet<Long>();
    var boxes = new ArrayList<Box>();
    for (var c : cells) {
      if (c.key() == null
          || !keys.add(c.key())
          || c.column() < 0
          || c.row() < 0
          || c.columns() < 1
          || c.rows() < 1
          || c.column() + c.columns() > columns
          || c.row() + c.rows() > rows)
        throw new IllegalArgumentException("Grid overflow/duplicate: " + c.key());
      for (int yy = c.row(); yy < c.row() + c.rows(); yy++)
        for (int xx = c.column(); xx < c.column() + c.columns(); xx++)
          if (!used.add(((long) yy << 32) | xx))
            throw new IllegalArgumentException("Grid overlap: " + c.key());
      int w = c.columns() * columnStride - gapX, h = c.rows() * rowStride - gapY;
      if (w < 1 || h < 9) throw new IllegalArgumentException("Grid cell too small: " + c.key());
      boxes.add(new Box(c.key(), x + c.column() * columnStride, y + c.row() * rowStride, w, h));
    }
    return List.copyOf(boxes);
  }
}
