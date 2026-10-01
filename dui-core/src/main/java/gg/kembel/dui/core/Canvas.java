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
      String id,
      int x,
      int y,
      int width,
      int height,
      int pixelSize,
      RasterImage raster,
      boolean background) {
    public Image(
        String id, int x, int y, int width, int height, int pixelSize, RasterImage raster) {
      this(id, x, y, width, height, pixelSize, raster, false);
    }
  }

  public record Confetti(String itemId, long startedAt) {}

  public record Animation(String itemId, long startedAt, boolean motion, int durationTicks) {}

  public record Hit(
      String id, String action, String value, String tooltip, int x, int y, int width, int height) {
    public boolean contains(int px, int py) {
      return px >= x && px < x + width && py >= y && py < y + height;
    }
  }

  public record Placement(String id, int x, int y) {}

  final Map<Object, Placement> placements = new IdentityHashMap<>();
  private Placement placement = new Placement("canvas", 0, 0);
  private int nodeSequence;

  public Placement enter(String id, int x, int y) {
    var previous = placement;
    placement =
        new Placement(previous.id() + "/" + (id.isBlank() ? "node_" + (nodeSequence++) : id), x, y);
    return previous;
  }

  public void leave(Placement previous) {
    placement = Objects.requireNonNull(previous);
  }

  private <T> T record(T primitive) {
    placements.put(primitive, placement);
    return primitive;
  }

  public final int width, height;
  public final List<Paint> paints = new ArrayList<>();
  public final List<Head> heads = new ArrayList<>();
  public final List<Item> items = new ArrayList<>();
  public final List<Image> images = new ArrayList<>();
  public final List<Hit> hits = new ArrayList<>();
  public final Map<String, ItemTransition> transitions = new LinkedHashMap<>();
  public final Map<String, ItemClip> clips = new LinkedHashMap<>();
  public boolean motionEnabled = true;
  public long animationStart = 0;
  public Confetti confetti;
  public Animation animation;
  public boolean hideFocusOutline = true;
  public final List<ShaderEffect> effects = new ArrayList<>();

  private record FitKey(RasterImage source, int width, int height) {}

  private static final BoundedCache<FitKey, RasterImage> FITTED =
      new BoundedCache<>(128, 2_000_000L, i -> (long) i.width * i.height);
  private final GlyphFont metrics;
  private final UiTheme theme;
  private ThemeTokens tokens;
  private Map<String, String> style = Map.of();
  public int effectLimit = ShaderEffect.LIMIT;
  public final List<Scene.Coverage> coverage = new ArrayList<>();
  public final Map<String, Motion> effectMotions = new LinkedHashMap<>();

  public void effectMotion(String id, Motion motion) {
    if (effects.stream().noneMatch(e -> e.id().equals(id))
        || effectMotions.putIfAbsent(id, Objects.requireNonNull(motion)) != null)
      throw new IllegalArgumentException("Effect motion needs a unique existing effect");
  }

  public final Map<String, Motion> motions = new LinkedHashMap<>();

  public Canvas(int width, int height, ThemeTokens tokens, GlyphFont metrics) {
    this(width, height, UiTheme.DEFAULT, metrics);
    this.tokens = Objects.requireNonNull(tokens);
  }

  public ThemeTokens tokens() {
    return tokens == null ? ThemeTokens.DARK : tokens;
  }

  public Map<String, String> style(Map<String, String> value) {
    var previous = style;
    style = Map.copyOf(value);
    return previous;
  }

  public void cover(String id, int x, int y, int w, int h) {
    bounds(x, y, w, h);
    coverage.add(
        new Scene.Coverage(
            id,
            new Scene.Rect(x, y, w, h),
            images.size(),
            heads.size(),
            items.size(),
            effects.size()));
  }

  public Canvas renderPlan() {
    return Scene.of(this).plan(this);
  }

  public boolean legacyMotion(String id) {
    var t = transitions.get(id);
    if (t == null) return false;
    Motion expected =
        t.kind() == ItemTransition.Kind.POP
            ? Motion.pop(t.startedAt(), t.durationTicks(), t.distance(), t.motion())
            : t.kind() == ItemTransition.Kind.SLIDE
                ? Motion.slide(t.startedAt(), t.durationTicks(), t.distance(), t.motion())
                : null;
    return expected != null && expected.equals(motions.get(id));
  }

  public void motion(String id, Motion motion) {
    if (items.stream().noneMatch(i -> i.id().equals(id))
        || motions.putIfAbsent(id, Objects.requireNonNull(motion)) != null)
      throw new IllegalArgumentException("Motion needs a unique existing item: " + id);
  }

  private int styled(int color, boolean text) {
    String override = text ? style.get("color") : null;
    if (override != null) return StyleResolver.color(override, tokens());
    return tokens == null ? theme.color(color) : tokens.semantic(color);
  }

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
    paints.add(record(new Paint(x, y, w, h, styled(color, false), null, null)));
  }

  public void panel(int x, int y, int w, int h, int fill, int border) {
    if (style.containsKey("fill")) fill = StyleResolver.color(style.get("fill"), tokens());
    if (style.containsKey("border")) border = StyleResolver.color(style.get("border"), tokens());
    rect(x + 1, y, w - 2, h, border);
    rect(x, y + 1, w, h - 2, border);
    rect(x + 1, y + 1, w - 2, h - 2, fill);
  }

  public void text(int x, int y, int w, String text, int color) {
    String fitted = metrics.fit(text, w);
    if (metrics.width(fitted) > w) return;
    bounds(x, y, metrics.width(fitted), 9);
    paints.add(
        record(new Paint(x, y, metrics.width(fitted), 9, styled(color, true), fitted, null)));
  }

  public void icon(int x, int y, String name, int color) {
    if (name.startsWith("item/") && y % 9 != 0)
      throw new IllegalArgumentException("Item sprites must use 9 px rows");
    int size = name.startsWith("item/") ? 18 : 9;
    bounds(x, y, size, size);
    paints.add(
        record(
            new Paint(
                x,
                y,
                size,
                size,
                name.startsWith("item/") ? color : styled(color, true),
                null,
                name)));
  }

  public void hit(Hit hit) {
    bounds(hit.x, hit.y, hit.width, hit.height);
    if (hit.y % 9 != 0 || hit.height % 9 != 0)
      throw new IllegalArgumentException("Hit must use full 9 px rows: " + hit.id);
    if (hits.stream().anyMatch(h -> h.id.equals(hit.id)))
      throw new IllegalArgumentException("Duplicate hit id: " + hit.id);
    hits.add(record(hit));
  }

  public void head(int x, int y, String source, boolean hat) {
    if (y % 9 != 0) throw new IllegalArgumentException("Head must use 9 px rows");
    if (source == null || source.isBlank())
      throw new IllegalArgumentException("Head needs a player or skin resource");
    bounds(x, y, 8, 8);
    heads.add(record(new Head(x, y, source, hat)));
  }

  public void effect(ShaderEffect effect) {
    bounds(effect.x(), effect.y(), effect.width(), effect.height());
    if (effects.size() >= effectLimit)
      throw new IllegalArgumentException(
          "At most " + effectLimit + " shader components per canvas");
    if (effects.stream().anyMatch(e -> e.id().equals(effect.id())))
      throw new IllegalArgumentException("Duplicate shader component: " + effect.id());
    effects.add(record(effect));
  }

  public void item(String id, int x, int y, int size) {
    item(id, x, y, size, null);
  }

  /** Clipped carriers may sit outside the canvas, to enter or leave a carousel viewport. */
  public void item(String id, int x, int y, int size, ItemClip clip) {
    if (id == null
        || id.isBlank()
        || size < 1
        || size > 127
        || items.stream().anyMatch(i -> i.id().equals(id)))
      throw new IllegalArgumentException("Native item needs a unique id and size 1..127");
    if (clip == null) bounds(x, y, size, size);
    else {
      bounds(clip.x(), clip.y(), clip.width(), clip.height());
      if (clip.x() - x < -512 || clip.x() - x > 511 || clip.y() - y < -512 || clip.y() - y > 511)
        throw new IllegalArgumentException("Item clip offset must fit -512..511");
      clips.put(id, clip);
    }
    items.add(record(new Item(id, x, y, size)));
  }

  public void transition(String id, ItemTransition transition) {
    if (items.stream().noneMatch(i -> i.id().equals(id)) || transitions.containsKey(id))
      throw new IllegalArgumentException("Transition needs a unique existing item: " + id);
    if (confetti != null && confetti.itemId().equals(id))
      throw new IllegalArgumentException("Use a separate particles carrier with item transitions");
    transitions.put(id, Objects.requireNonNull(transition));
    if (transition.kind() == ItemTransition.Kind.POP)
      motions.put(
          id,
          Motion.pop(
              transition.startedAt(),
              transition.durationTicks(),
              transition.distance(),
              transition.motion()));
    if (transition.kind() == ItemTransition.Kind.SLIDE)
      motions.put(
          id,
          Motion.slide(
              transition.startedAt(),
              transition.durationTicks(),
              transition.distance(),
              transition.motion()));
  }

  public void image(String id, int x, int y, int w, int h, int pixelSize, RasterImage raster) {
    image(id, x, y, w, h, pixelSize, raster, false);
  }

  public void image(
      String id,
      int x,
      int y,
      int w,
      int h,
      int pixelSize,
      RasterImage raster,
      boolean background) {
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
        > RenderReport.IMAGE_PIXEL_LIMIT)
      throw new IllegalArgumentException("Canvas image pixel budget exceeded");
    images.add(
        record(
            new Image(
                id,
                x,
                y,
                w,
                h,
                pixelSize,
                FITTED.computeIfAbsent(
                    new FitKey(Objects.requireNonNull(raster), columns, rows),
                    key -> key.source().cover(key.width(), key.height())),
                background)));
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
