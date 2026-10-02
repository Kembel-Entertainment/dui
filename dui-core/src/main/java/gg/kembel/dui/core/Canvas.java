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

  /** Contained orthographic full-body viewport; source refers to ViewModel.appearances. */
  public record PlayerModel(
      String id,
      String source,
      int x,
      int y,
      int width,
      int height,
      int facing,
      boolean outerLayer,
      boolean idle,
      String renderer,
      int rendererCode,
      int viewportIndex) {
    public PlayerModel(
        String id,
        String source,
        int x,
        int y,
        int width,
        int height,
        int facing,
        boolean outerLayer,
        boolean idle) {
      this(
          id,
          source,
          x,
          y,
          width,
          height,
          facing,
          outerLayer,
          idle,
          "dui:player",
          0,
          PlayerRenderSpec.standard().fit(width, height));
    }
  }

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
  private final List<Object> drawOrder = new ArrayList<>();

  public List<Object> drawOrder() {
    return List.copyOf(drawOrder);
  }

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
    drawOrder.add(primitive);
    return primitive;
  }

  public final int width, height;
  public final List<Paint> paints = new ArrayList<>();
  public final List<Head> heads = new ArrayList<>();
  public final List<PlayerModel> playerModels = new ArrayList<>();
  public final List<Item> items = new ArrayList<>();
  public final List<Image> images = new ArrayList<>();
  public final List<Hit> hits = new ArrayList<>();
  public final Map<String, ItemClip> clips = new LinkedHashMap<>();
  public boolean motionEnabled = true;
  public long animationStart = 0;
  public Animation animation;
  public boolean hideFocusOutline = false;
  public int focusOutlineColor;
  public final List<ShaderInvocation> effects = new ArrayList<>();
  public final List<RenderPrimitive> primitives = new ArrayList<>();

  public void primitive(RenderPrimitive primitive) {
    var rect = primitive.bounds();
    bounds(rect.x(), rect.y(), rect.width(), rect.height());
    if (primitives.size() >= 32 || primitives.stream().anyMatch(p -> p.id().equals(primitive.id())))
      throw new IllegalArgumentException("Extension primitive capacity/id collision");
    primitives.add(record(primitive));
  }

  private record FitKey(RasterImage source, int width, int height) {}

  private static final BoundedCache<FitKey, RasterImage> FITTED =
      new BoundedCache<>(128, 2_000_000L, i -> (long) i.width * i.height);
  private final GlyphFont metrics;
  private final RenderEnvironment environment;
  private Map<String, String> style = Map.of();
  public int effectLimit = ShaderInvocation.LIMIT;
  public final List<Scene.Coverage> coverage = new ArrayList<>();
  public final Map<String, Motion> effectMotions = new LinkedHashMap<>();

  public void effectMotion(String id, Motion motion) {
    if (effects.stream().noneMatch(e -> e.id().equals(id))
        || effectMotions.putIfAbsent(id, Objects.requireNonNull(motion)) != null)
      throw new IllegalArgumentException("Effect motion needs a unique existing effect");
  }

  public final Map<String, Motion> motions = new LinkedHashMap<>();

  public Canvas(int width, int height, ThemeTokens tokens, GlyphFont metrics) {
    this(width, height, RenderEnvironment.plain(metrics).withTokens(tokens));
  }

  public ThemeTokens tokens() {
    return environment.tokens();
  }

  public RenderEnvironment environment() {
    return environment;
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
            effects.size(),
            playerModels.size()));
  }

  public Canvas renderPlan() {
    return Scene.of(this).plan(this);
  }

  public void motion(String id, Motion motion) {
    if (items.stream().noneMatch(i -> i.id().equals(id))
        || motions.putIfAbsent(id, Objects.requireNonNull(motion)) != null)
      throw new IllegalArgumentException("Motion needs a unique existing item: " + id);
  }

  private int styled(int color, boolean text) {
    String override = text ? style.get("color") : null;
    if (override != null) return StyleResolver.color(override, tokens());
    return environment.colorTransform().apply(Map.of(), tokens()).applyAsInt(color);
  }

  public GlyphFont metrics() {
    if (style.containsKey("font"))
      return GlyphFont.from(RichText.require(environment.fonts(), style.get("font")));
    return metrics;
  }

  public int textLineHeight() {
    return style.containsKey("font")
        ? RichText.require(environment.fonts(), style.get("font")).lineHeight()
        : 9;
  }

  public Canvas(int width, int height, RenderEnvironment environment) {
    this.environment = Objects.requireNonNull(environment);
    this.metrics = environment.font();
    if (width < 120 || width > 480 || height < 9 || height > 360 || height % 9 != 0)
      throw new IllegalArgumentException("Canvas bounds / 9 px grid");
    this.width = width;
    this.height = height;
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
    if (style.containsKey("font")) {
      var face = RichText.require(environment.fonts(), style.get("font"));
      String fitted = text;
      while (face.width(fitted) > w && !fitted.isEmpty())
        fitted = fitted.substring(0, fitted.offsetByCodePoints(fitted.length(), -1));
      if (!fitted.isEmpty())
        text(
            x,
            y,
            w,
            new RichText(List.of(new RichText.Span(fitted, face.id(), styled(color, true), 1))));
      return;
    }
    String fitted = metrics.fit(text, w);
    if (metrics.width(fitted) > w) return;
    bounds(x, y, metrics.width(fitted), 9);
    paints.add(
        record(new Paint(x, y, metrics.width(fitted), 9, styled(color, true), fitted, null)));
  }

  public void icon(int x, int y, String name, int color) {
    var binding = GlyphRegistry.require(environment.glyphs(), name);
    int size = binding.specification().width(), height = binding.specification().height();
    bounds(x, y, size, height);
    paints.add(
        record(
            new Paint(
                x,
                y,
                size,
                height,
                binding.specification().tintable() ? styled(color, true) : 0xFFFFFF,
                null,
                name)));
  }

  public void text(int x, int y, int width, RichText text) {
    int cursor = x;
    var size = text.measure(environment.fonts());
    bounds(x, y, Math.min(width, size.width()), size.height());
    for (var span : text.spans()) {
      var face = RichText.require(environment.fonts(), span.font());
      if (span.text().isEmpty()) continue;
      var raster = face.raster(span.text(), span.color());
      int visible = Math.min(raster.width, x + width - cursor);
      if (visible <= 0) break;
      int[] pixels = new int[visible * raster.height];
      for (int yy = 0; yy < raster.height; yy++)
        for (int xx = 0; xx < visible; xx++) {
          int pixel = raster.argb(xx, yy);
          pixels[yy * visible + xx] =
              ((int) Math.round((pixel >>> 24) * span.opacity()) << 24) | (pixel & 0xffffff);
        }
      image(
          "text_" + (nodeSequence++),
          cursor,
          y,
          visible,
          raster.height,
          1,
          RasterImage.argb(visible, raster.height, pixels));
      cursor += raster.width;
    }
  }

  /** Atomic composition: failure never leaves a half-painted parent canvas. */
  public void group(String id, SceneGroup options, java.util.function.Consumer<Canvas> children) {
    if (id == null || id.isBlank()) throw new IllegalArgumentException("Group id required");
    var child = new Canvas(width, height, environment);
    child.motionEnabled = motionEnabled;
    child.animationStart = animationStart;
    child.effectLimit = effectLimit;
    children.accept(child);
    var result = GroupComposer.compose(id, child, options);
    var ids = new HashSet<String>();
    for (var i : items) ids.add(i.id());
    for (var i : result.items)
      if (!ids.add(i.id())) throw new IllegalArgumentException("Group item collision: " + i.id());
    ids.clear();
    for (var i : hits) ids.add(i.id());
    for (var i : result.hits)
      if (!ids.add(i.id())) throw new IllegalArgumentException("Group hit collision: " + i.id());
    ids.clear();
    for (var i : effects) ids.add(i.id());
    for (var i : result.effects)
      if (!ids.add(i.id())) throw new IllegalArgumentException("Group effect collision: " + i.id());
    if (effects.size() + result.effects.size() > effectLimit
        || images.stream().mapToInt(i -> i.raster().width * i.raster().height).sum()
                + result.images.stream().mapToInt(i -> i.raster().width * i.raster().height).sum()
            > RenderReport.IMAGE_PIXEL_LIMIT)
      throw new IllegalArgumentException("Group exceeds canvas budget");
    ids.clear();
    for (var i : primitives) ids.add(i.id());
    for (var i : result.primitives)
      if (!ids.add(i.id())) throw new IllegalArgumentException("Group primitive collision");
    ids.clear();
    for (var i : playerModels) ids.add(i.id());
    for (var i : result.playerModels)
      if (!ids.add(i.id())) throw new IllegalArgumentException("Group player model collision");
    if (primitives.size() + result.primitives.size() > 32)
      throw new IllegalArgumentException("Group extension budget");
    var previous = enter(id, 0, 0);
    try {
      for (var image : result.images) images.add(record(image));
      for (var item : result.items) items.add(record(item));
      for (var effect : result.effects) effects.add(record(effect));
      for (var head : result.heads) heads.add(record(head));
      for (var model : result.playerModels) playerModels.add(record(model));
      for (var hit : result.hits) hits.add(record(hit));
      for (var primitive : result.primitives) primitives.add(record(primitive));
      clips.putAll(result.clips);
      motions.putAll(result.motions);
      effectMotions.putAll(result.effectMotions);
    } finally {
      leave(previous);
    }
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

  public void playerModel(
      String id,
      String source,
      int x,
      int y,
      int w,
      int h,
      int facing,
      boolean outerLayer,
      boolean idle) {
    playerModel(id, source, x, y, w, h, "dui:player", facing, outerLayer, idle);
  }

  public void playerModel(
      String id,
      String source,
      int x,
      int y,
      int w,
      int h,
      String renderer,
      int facing,
      boolean outerLayer,
      boolean idle) {
    var binding =
        Objects.requireNonNull(
            environment.playerRenderers().get(renderer),
            "Unregistered player renderer: " + renderer);
    if (id == null
        || id.isBlank()
        || source == null
        || source.isBlank()
        || facing < 0
        || facing >= binding.specification().poses().size()
        || y % 9 != 0
        || h % 9 != 0)
      throw new IllegalArgumentException(
          "Player model needs an id, appearance, facing 0..7 and nine-pixel rows");
    bounds(x, y, w, h);
    if (playerModels.stream().anyMatch(p -> p.id().equals(id)))
      throw new IllegalArgumentException("Duplicate player model id: " + id);
    int viewport = binding.specification().fit(w, h);
    var dimensions = binding.specification().viewports().get(viewport);
    int height = dimensions.height(), width = dimensions.width();
    int top = y + ((h - height) / 18) * 9;
    playerModels.add(
        record(
            new PlayerModel(
                id,
                source,
                x + (w - width) / 2,
                top,
                width,
                height,
                facing,
                outerLayer,
                idle,
                renderer,
                binding.code(),
                viewport)));
  }

  public void effect(ShaderInvocation effect) {
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
