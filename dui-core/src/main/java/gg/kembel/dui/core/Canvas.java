package gg.kembel.dui.core;

import java.util.*;

/**
 * Pixel layout independent of Paper. Hit regions use the vanilla 9 px line grid; glyphs can be
 * centered within a row.
 */
public final class Canvas {
  public record Paint(int x, int y, int width, int height, int color, String text, String icon) {}

  /**
   * Native 8x8 player object, resolved from a profile or skin resource by the platform renderer.
   */
  public record Head(int x, int y, String source, boolean hat) {}

  public record Item(String id, int x, int y, int size) {}

  public record Image(
      String id, int x, int y, int width, int height, int pixelSize, RasterImage raster) {}

  public record Confetti(String itemId, long startedAt) {}

  public record Animation(String itemId, long startedAt, boolean motion, int durationTicks) {}

  public record Hit(
      String id, String action, String value, String tooltip, int x, int y, int width, int height) {
    public boolean contains(int px, int py) {
      return px >= x && px < x + width && py >= y && py < y + height;
    }
  }

  public final int width, height;
  public final List<Paint> paints = new ArrayList<>();
  public final List<Head> heads = new ArrayList<>();
  public final List<Item> items = new ArrayList<>();
  public final List<Image> images = new ArrayList<>();
  public final List<Hit> hits = new ArrayList<>();
  public final Map<String, ItemTransition> transitions = new LinkedHashMap<>();
  public boolean motionEnabled = true;
  public Confetti confetti;
  public Animation animation;
  public boolean hideFocusOutline = true;
  public final List<ShaderEffect> effects = new ArrayList<>();
  private final GlyphFont metrics;
  private final UiTheme theme;

  public Canvas(int width, int height) {
    this(width, height, UiTheme.DEFAULT);
  }

  public Canvas(int width, int height, UiTheme theme) {
    this(width, height, theme, new GlyphFont());
  }

  public GlyphFont metrics() {
    return metrics;
  }

  public Canvas(int width, int height, UiTheme theme, GlyphFont metrics) {
    this.metrics = Objects.requireNonNull(metrics);
    if (width < 120 || width > 480 || height < 9 || height > 360 || height % 9 != 0)
      throw new IllegalArgumentException("Canvas bounds / 9 px grid");
    this.width = width;
    this.height = height;
    this.theme = Objects.requireNonNull(theme);
  }

  public void rect(int x, int y, int w, int h, int color) {
    if (w <= 0 || h <= 0) return;
    bounds(x, y, w, h);
    paints.add(new Paint(x, y, w, h, theme.color(color), null, null));
  }

  public void panel(int x, int y, int w, int h, int fill, int border) {
    rect(x + 1, y, w - 2, h, border);
    rect(x, y + 1, w, h - 2, border);
    rect(x + 1, y + 1, w - 2, h - 2, fill);
  }

  public void text(int x, int y, int w, String text, int color) {
    String fitted = metrics.fit(text, w);
    if (metrics.width(fitted) > w) return;
    bounds(x, y, metrics.width(fitted), 9);
    paints.add(new Paint(x, y, metrics.width(fitted), 9, theme.color(color), fitted, null));
  }

  public void icon(int x, int y, String name, int color) {
    if (name.startsWith("item/") && y % 9 != 0)
      throw new IllegalArgumentException("Item sprites must use 9 px rows");
    int size = name.startsWith("item/") ? 18 : 9;
    bounds(x, y, size, size);
    paints.add(
        new Paint(
            x, y, size, size, name.startsWith("item/") ? color : theme.color(color), null, name));
  }

  public void hit(Hit hit) {
    bounds(hit.x, hit.y, hit.width, hit.height);
    if (hit.y % 9 != 0 || hit.height % 9 != 0)
      throw new IllegalArgumentException("Hit must use full 9 px rows: " + hit.id);
    if (hits.stream().anyMatch(h -> h.id.equals(hit.id)))
      throw new IllegalArgumentException("Duplicate hit id: " + hit.id);
    hits.add(hit);
  }

  public void head(int x, int y, String source, boolean hat) {
    if (y % 9 != 0) throw new IllegalArgumentException("Head must use 9 px rows");
    if (source == null || source.isBlank())
      throw new IllegalArgumentException("Head needs a player or skin resource");
    bounds(x, y, 8, 8);
    heads.add(new Head(x, y, source, hat));
  }

  public void effect(ShaderEffect effect) {
    bounds(effect.x(), effect.y(), effect.width(), effect.height());
    if (effects.size() >= ShaderEffect.LIMIT)
      throw new IllegalArgumentException(
          "At most " + ShaderEffect.LIMIT + " shader components per canvas");
    if (effects.stream().anyMatch(e -> e.id().equals(effect.id())))
      throw new IllegalArgumentException("Duplicate shader component: " + effect.id());
    effects.add(effect);
  }

  public void item(String id, int x, int y, int size) {
    bounds(x, y, size, size);
    items.add(new Item(id, x, y, size));
  }

  public void transition(String id, ItemTransition transition) {
    if (items.stream().noneMatch(i -> i.id().equals(id)) || transitions.containsKey(id))
      throw new IllegalArgumentException("Transition needs a unique existing item: " + id);
    if (confetti != null && confetti.itemId().equals(id))
      throw new IllegalArgumentException("Use a separate particles carrier with item transitions");
    transitions.put(id, Objects.requireNonNull(transition));
  }

  public void image(String id, int x, int y, int w, int h, int pixelSize, RasterImage raster) {
    bounds(x, y, w, h);
    if (id == null
        || id.isBlank()
        || w < 1
        || h < 1
        || pixelSize < 1
        || pixelSize > 8
        || images.stream().anyMatch(i -> i.id.equals(id)))
      throw new IllegalArgumentException("Invalid image placement");
    int columns = (w + pixelSize - 1) / pixelSize, rows = (h + pixelSize - 1) / pixelSize;
    if (images.stream().mapToInt(i -> i.raster.width * i.raster.height).sum() + columns * rows
        > 16_384) throw new IllegalArgumentException("Canvas image pixel budget exceeded");
    images.add(
        new Image(id, x, y, w, h, pixelSize, Objects.requireNonNull(raster).cover(columns, rows)));
  }

  public Hit at(int x, int y) {
    for (int i = hits.size() - 1; i >= 0; i--) if (hits.get(i).contains(x, y)) return hits.get(i);
    return null;
  }

  private void bounds(int x, int y, int w, int h) {
    if (x < 0 || y < 0 || x + w > width || y + h > height)
      throw new IllegalArgumentException(
          "Canvas overflow: " + x + "," + y + " " + w + "x" + h + " in " + width + "x" + height);
  }
}
