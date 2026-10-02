package gg.kembel.dui.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class DynamicCompositionTest {
  @Test
  void structuredSnapshotsCopyNestedContainersAndRejectMutableNumbers() {
    var rows = new ArrayList<>(List.of("first"));
    var source = new HashMap<String, Object>();
    source.put("rows", rows);
    source.put("amount", new java.math.BigDecimal("12.5"));
    var snapshot = ValueCodec.object().decode(source);
    rows.clear();
    source.clear();
    assertEquals(List.of("first"), snapshot.get("rows"));
    assertEquals(new java.math.BigDecimal("12.5"), snapshot.get("amount"));
    assertThrows(UnsupportedOperationException.class, () -> snapshot.put("extra", 1));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ValueCodec.object()
                .decode(
                    Map.of(
                        "nested",
                        Map.of("count", new java.util.concurrent.atomic.AtomicInteger(1)))));
  }

  @Test
  void templateAlignmentAndNaturalHeightUseTheRegisteredFont() throws Exception {
    var face =
        new BitmapFont(
            "own:large",
            18,
            Map.of("?", new BitmapFont.Glyph(3, 18, 4, Collections.nCopies(54, 0xffffffff))));
    var env = environment().withResources(Map.of("own:large", face), Map.of());
    var template =
        MenuTemplate.parse(
            "<dui-menu width='180' height='90'><dui-column><dui-text label='??' font='own:large'"
                + " align='right' color='#FFFFFF'/><dui-rect height='9'"
                + " fill='#FF0000'/></dui-column></dui-menu>",
            env,
            ComponentRegistry.EMPTY);
    var canvas = template.render(Map.of());
    assertEquals(172, canvas.images.getFirst().x());
    assertEquals(18, canvas.paints.getFirst().y());
    assertEquals(18, canvas.images.getFirst().height());
  }

  static RenderEnvironment environment() {
    var face =
        new BitmapFont(
            "dui:default",
            9,
            Map.of("?", new BitmapFont.Glyph(2, 9, 3, Collections.nCopies(18, 0xffffffff))));
    return RenderEnvironment.plain(new GlyphFont(Map.of("?", 3)))
        .withResources(Map.of(face.id(), face), Map.of());
  }

  @Test
  void exactBindingsKeepListsAndIntrinsicHeightFollowsContent() throws Exception {
    var contract =
        new ComponentContract(
            new PropertySchema(Map.of()),
            Map.of(
                "rows",
                new ComponentContract.Value(ValueCodec.list(ValueCodec.string(), 10), true, null)));
    var registry =
        ComponentRegistry.builder()
            .renderer(
                "test-list",
                contract,
                ctx ->
                    new Measure.Size(
                        ctx.constraints().maxWidth(),
                        ctx.node().value("rows", List.class).size() * 9),
                ctx -> {
                  int y = ctx.y();
                  for (Object row : ctx.node().value("rows", List.class)) {
                    ctx.canvas().text(ctx.x(), y, ctx.width(), row.toString(), 0xffffff);
                    y += 9;
                  }
                })
            .build();
    var template =
        MenuTemplate.parse(
            "<dui-menu width=\"180\" height=\"90\"><dui-column><dui-test-list"
                + " rows=\"{{rows}}\"/><dui-rect height=\"9\""
                + " fill=\"#FF0000\"/></dui-column></dui-menu>",
            environment(),
            registry);
    var rows = new ArrayList<>(List.of("first", "second", "third"));
    var canvas = template.render(Map.of("rows", rows));
    rows.clear();
    assertEquals(27, canvas.paints.getLast().y());
    assertThrows(IllegalArgumentException.class, () -> template.render(Map.of("rows", List.of(1))));
    assertTrue(registry.describe().toString().contains("list<string>"));
  }

  @Test
  void customContainersCanMeasureAndArrangeChildren() throws Exception {
    var registry =
        ComponentRegistry.builder()
            .renderer(
                "test-stack",
                ComponentContract.scalar(new PropertySchema(Map.of())),
                ctx ->
                    new Measure.Size(
                        ctx.constraints().maxWidth(),
                        ctx.node().children().stream()
                            .mapToInt(n -> ctx.children().measure(n, ctx.constraints()).height())
                            .sum()),
                ctx -> {
                  int y = ctx.y();
                  for (var child : ctx.node().children()) {
                    var size =
                        ctx.children()
                            .measure(
                                child, Measure.Constraints.available(ctx.width(), ctx.height()));
                    ctx.children().draw(child, ctx.x(), y, size.width(), size.height());
                    y += size.height();
                  }
                })
            .build();
    var c =
        MenuTemplate.parse(
                "<dui-menu width=\"180\" height=\"90\"><dui-column><dui-test-stack><dui-rect"
                    + " height=\"18\" fill=\"#FF0000\"/><dui-rect height=\"27\""
                    + " fill=\"#00FF00\"/></dui-test-stack><dui-rect height=\"9\""
                    + " fill=\"#0000FF\"/></dui-column></dui-menu>",
                environment(),
                registry)
            .render(Map.of());
    assertEquals(45, c.paints.getLast().y());
  }

  @Test
  void nestedGroupsTransformPixelsAndHitsTogether() {
    var c = new Canvas(180, 90, environment());
    c.group(
        "outer",
        SceneGroup.translated(18, 9),
        outer ->
            outer.group(
                "inner",
                SceneGroup.translated(9, 9),
                inner -> {
                  inner.rect(0, 0, 18, 18, 0x22aaff);
                  inner.text(0, 0, 12, "?", 0xffffff);
                  inner.hit(new Canvas.Hit("button", "click", "v", "tip", 0, 0, 18, 18));
                }));
    assertNull(c.at(1, 1));
    assertEquals("click", c.at(28, 19).action());
    assertEquals("outer/inner/button", c.at(28, 19).id());
    var image = c.images.getFirst();
    assertEquals(27, image.x());
    assertEquals(18, image.y());
    assertEquals(0xff22aaff, image.raster().argb(10, 10));
    assertTrue(image.raster().hasAlpha());
  }

  @Test
  void rotatedDecorationIsClippedAndTransparentWithoutFlattening() {
    var c = new Canvas(180, 90, environment());
    c.group(
        "art",
        new SceneGroup(
            Transform2D.translation(30, 27).multiply(Transform2D.rotation(30)),
            new Scene.Rect(27, 18, 27, 27),
            .5),
        g -> g.rect(0, 0, 27, 27, 0xff0000));
    var i = c.images.getFirst();
    assertTrue(i.x() >= 27 && i.x() + i.width() <= 54);
    assertTrue(i.y() >= 18 && i.y() + i.height() <= 45);
    assertTrue(Arrays.stream(i.raster().pixels()).anyMatch(pixel -> (pixel >>> 24) == 128));
  }

  @Test
  void unsupportedInputGeometryFailsAtomically() {
    var c = new Canvas(180, 90, environment());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            c.group(
                "bad",
                SceneGroup.translated(0, 2),
                g -> {
                  g.rect(0, 0, 18, 18, 1);
                  g.hit(new Canvas.Hit("h", "a", "", "", 0, 0, 18, 18));
                }));
    assertTrue(c.images.isEmpty());
    assertTrue(c.hits.isEmpty());
  }

  @Test
  void richTextMeasuresDifferentFontsAndRetainsOpacity() {
    var env = environment();
    var c = new Canvas(180, 90, env);
    var text =
        new RichText(
            List.of(
                new RichText.Span("??", "dui:default", 0xff0000, .5),
                new RichText.Span("?", "dui:default", 0x00ff00, 1)));
    assertEquals(new Measure.Size(9, 9), text.measure(env.fonts()));
    c.text(9, 9, 100, text);
    assertEquals(0x80ff0000, c.images.getFirst().raster().argb(0, 0));
    assertEquals(0xff00ff00, c.images.getLast().raster().argb(0, 0));
  }

  @Test
  void affineInverseAndNativeViewportUseSameGeometry() {
    var t =
        Transform2D.translation(30, 27)
            .multiply(Transform2D.rotation(45))
            .multiply(Transform2D.scale(2, 2));
    var p = t.apply(4, 7);
    var local = t.inverse().apply(p.x(), p.y());
    assertEquals(4, local.x(), 1e-8);
    assertEquals(7, local.y(), 1e-8);
    var c = new Canvas(180, 90, environment());
    c.group(
        "models",
        new SceneGroup(t, new Scene.Rect(0, 0, 180, 90), .5),
        g -> g.item("item", 0, 0, 18));
    assertEquals(36, c.items.getFirst().size());
    assertEquals(45, c.motions.get("models/item").rotateTo());
    assertEquals(.5, c.motions.get("models/item").opacityTo());
  }
}
