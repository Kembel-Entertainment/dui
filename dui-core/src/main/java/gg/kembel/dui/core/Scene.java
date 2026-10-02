package gg.kembel.dui.core;

import java.util.*;

/**
 * Immutable presentation snapshot. Native backends support fixed clips, not arbitrary painter
 * order.
 */
public record Scene(
    List<Canvas.Paint> paints,
    List<Canvas.Image> images,
    List<Canvas.Head> heads,
    List<Canvas.Item> items,
    List<Canvas.Hit> hits,
    List<ShaderInvocation> effects,
    List<Coverage> coverage,
    List<Node> nodes,
    List<Canvas.PlayerModel> playerModels) {
  public Scene(
      List<Canvas.Paint> paints,
      List<Canvas.Image> images,
      List<Canvas.Head> heads,
      List<Canvas.Item> items,
      List<Canvas.Hit> hits,
      List<ShaderInvocation> effects,
      List<Coverage> coverage,
      List<Node> nodes) {
    this(paints, images, heads, items, hits, effects, coverage, nodes, List.of());
  }

  public enum Backend {
    BACKGROUND_IMAGE,
    FONT,
    IMAGE,
    HEAD,
    PLAYER_MODEL,
    NATIVE,
    EFFECT,
    HIT
  }

  public record Node(
      String id,
      String parent,
      Backend backend,
      Rect local,
      int originX,
      int originY,
      ItemClip clip,
      boolean visible,
      int layer,
      Object primitive) {
    public Rect canvasBounds() {
      return new Rect(originX + local.x(), originY + local.y(), local.width(), local.height());
    }
  }

  public record Rect(int x, int y, int width, int height) {
    public boolean overlaps(int x, int y, int w, int h) {
      return x < this.x + width && x + w > this.x && y < this.y + height && y + h > this.y;
    }
  }

  public record Coverage(
      String id,
      Rect bounds,
      int imageIndex,
      int headIndex,
      int itemIndex,
      int effectIndex,
      int playerIndex) {
    public Coverage(
        String id, Rect bounds, int imageIndex, int headIndex, int itemIndex, int effectIndex) {
      this(id, bounds, imageIndex, headIndex, itemIndex, effectIndex, 0);
    }
  }

  public Scene {
    paints = List.copyOf(paints);
    images = List.copyOf(images);
    heads = List.copyOf(heads);
    items = List.copyOf(items);
    hits = List.copyOf(hits);
    effects = List.copyOf(effects);
    coverage = List.copyOf(coverage);
    nodes = List.copyOf(nodes);
    playerModels = List.copyOf(playerModels);
  }

  public static Scene of(Canvas c) {
    var nodes = new ArrayList<Node>();
    for (var image : c.images)
      node(
          nodes,
          c,
          image,
          image.background() ? Backend.BACKGROUND_IMAGE : Backend.IMAGE,
          image.id(),
          image.x(),
          image.y(),
          image.width(),
          image.height());
    for (var paint : c.paints)
      node(
          nodes,
          c,
          paint,
          Backend.FONT,
          "paint_" + nodes.size(),
          paint.x(),
          paint.y(),
          paint.width(),
          paint.height());
    for (var head : c.heads)
      node(nodes, c, head, Backend.HEAD, "head_" + nodes.size(), head.x(), head.y(), 8, 8);
    for (var item : c.items)
      node(nodes, c, item, Backend.NATIVE, item.id(), item.x(), item.y(), item.size(), item.size());
    for (var effect : c.effects)
      node(
          nodes,
          c,
          effect,
          Backend.EFFECT,
          effect.id(),
          effect.x(),
          effect.y(),
          effect.width(),
          effect.height());
    for (var hit : c.hits)
      node(nodes, c, hit, Backend.HIT, hit.id(), hit.x(), hit.y(), hit.width(), hit.height());
    for (var model : c.playerModels)
      node(
          nodes,
          c,
          model,
          Backend.PLAYER_MODEL,
          model.id(),
          model.x(),
          model.y(),
          model.width(),
          model.height());
    return new Scene(
        c.paints, c.images, c.heads, c.items, c.hits, c.effects, c.coverage, nodes, c.playerModels);
  }

  private static void node(
      List<Node> nodes,
      Canvas c,
      Object primitive,
      Backend backend,
      String id,
      int x,
      int y,
      int w,
      int h) {
    var placement = c.placements.getOrDefault(primitive, new Canvas.Placement("canvas", 0, 0));
    ItemClip clip = primitive instanceof Canvas.Item item ? c.clips.get(item.id()) : null;
    nodes.add(
        new Node(
            placement.id() + "/" + id,
            placement.id(),
            backend,
            new Rect(x - placement.x(), y - placement.y(), w, h),
            placement.x(),
            placement.y(),
            clip,
            true,
            backend.ordinal(),
            primitive));
  }

  public Set<String> capabilities() {
    return Set.of(
        "fixed-backend-layers",
        "canvas-local-geometry",
        "native-viewport-clip",
        "whole-object-popup-coverage");
  }

  /** Requesting a painter layer across native/font boundaries is rejected, not silently ignored. */
  public void require(String capability) {
    if (!capabilities().contains(capability))
      throw new IllegalArgumentException("Scene backend does not support: " + capability);
  }

  /**
   * Popup coverage uses whole-object suppression for late native/effect paths. Base scene is
   * retained.
   */
  public Canvas plan(Canvas source) {
    var result = new Canvas(source.width, source.height, source.environment());
    result.motionEnabled = source.motionEnabled;
    result.hideFocusOutline = source.hideFocusOutline;
    result.focusOutlineColor = source.focusOutlineColor;
    result.animation = source.animation;
    result.animationStart = source.animationStart;
    result.effectLimit = source.effectLimit;
    result.primitives.addAll(source.primitives);
    result.placements.putAll(source.placements);
    result.paints.addAll(paints);
    result.hits.addAll(hits);
    result.motions.putAll(source.motions);
    result.effectMotions.putAll(source.effectMotions);
    result.clips.putAll(source.clips);
    for (int i = 0; i < images.size(); i++) {
      var v = images.get(i);
      final int index = i;
      if (v.background()
          || coverage.stream()
              .noneMatch(
                  o ->
                      index < o.imageIndex()
                          && o.bounds().overlaps(v.x(), v.y(), v.width(), v.height())))
        result.images.add(v);
    }
    for (int i = 0; i < heads.size(); i++) {
      var v = heads.get(i);
      final int index = i;
      if (coverage.stream()
          .noneMatch(o -> index < o.headIndex() && o.bounds().overlaps(v.x(), v.y(), 8, 8)))
        result.heads.add(v);
    }
    for (int i = 0; i < items.size(); i++) {
      var v = items.get(i);
      final int index = i;
      if (coverage.stream()
          .noneMatch(
              o -> index < o.itemIndex() && o.bounds().overlaps(v.x(), v.y(), v.size(), v.size())))
        result.items.add(v);
    }
    for (int i = 0; i < effects.size(); i++) {
      var v = effects.get(i);
      final int index = i;
      if (coverage.stream()
          .noneMatch(
              o ->
                  index < o.effectIndex()
                      && o.bounds().overlaps(v.x(), v.y(), v.width(), v.height())))
        result.effects.add(v);
    }
    for (int i = 0; i < playerModels.size(); i++) {
      var v = playerModels.get(i);
      final int index = i;
      if (coverage.stream()
          .noneMatch(
              o ->
                  index < o.playerIndex()
                      && o.bounds().overlaps(v.x(), v.y(), v.width(), v.height())))
        result.playerModels.add(v);
    }
    return result;
  }
}
