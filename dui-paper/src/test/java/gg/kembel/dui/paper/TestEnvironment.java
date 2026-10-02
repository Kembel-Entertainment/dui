package gg.kembel.dui.paper;

import gg.kembel.dui.core.*;
import java.util.*;

public final class TestEnvironment {
  public static GlyphFont font() {
    var p = new HashMap<String, Integer>();
    for (int c = 32; c < 127; c++) p.put(Character.toString(c), 6);
    return new GlyphFont(p);
  }

  public static RenderEnvironment environment() {
    var skins = new HashMap<String, WidgetSkinRegistry.Skin>();
    for (String name :
        List.of(
            "text",
            "heading",
            "button",
            "checkbox",
            "toggle",
            "dropdown",
            "icon",
            "head",
            "progress",
            "divider"))
      skins.put(
          name,
          new WidgetSkinRegistry.Skin(
              18,
              0,
              0,
              18,
              (ctx, part) -> {
                if (part.equals("popup")) {
                  ctx.canvas().rect(ctx.x(), ctx.y(), ctx.width(), ctx.height(), 0x111111);
                  return;
                }
                if (ctx.node().type().equals("icon"))
                  ctx.canvas()
                      .icon(ctx.x(), ctx.y(), ctx.node().s("name", "fixture:shape"), 0xffffff);
                else if (ctx.node().type().equals("head"))
                  ctx.canvas()
                      .head(
                          ctx.x(),
                          ctx.y(),
                          ctx.node().s("player", "self"),
                          !ctx.node().s("hat", "true").equals("false"));
                else
                  TextLayout.draw(
                      ctx.canvas(),
                      ctx.node().s("label", ""),
                      ctx.x(),
                      ctx.y(),
                      ctx.width(),
                      ctx.height(),
                      ctx.node().s("align", "left"),
                      ctx.node().b("wrap"),
                      ctx.node().n("max-lines", Math.max(1, ctx.height() / 9)),
                      ctx.node().props().containsKey("color")
                          ? StyleResolver.color(ctx.node().s("color", ""), ctx.canvas().tokens())
                          : 0xffffff);
              },
              (n, w, h) -> Map.of()));
    return new RenderEnvironment(
        font(),
        ThemeTokens.EMPTY,
        new WidgetSkinRegistry(skins),
        GlyphRegistry.bind(List.of(new GlyphSpec("fixture:shape", 9, 9))),
        (data, tokens) -> v -> v);
  }

  public static Canvas canvas(int width, int height) {
    return new Canvas(width, height, environment());
  }

  public static MenuTemplate parse(String source) throws Exception {
    return MenuTemplate.parse(source, environment(), ComponentRegistry.EMPTY);
  }

  public static MenuTemplate parse(String source, GlyphFont font, ComponentRegistry registry)
      throws Exception {
    return MenuTemplate.parse(source, environment().withFont(font), registry);
  }

  public static MenuTemplate parse(
      String source, GlyphFont font, ComponentRegistry registry, String name) throws Exception {
    return MenuTemplate.parse(source, environment().withFont(font), registry, name);
  }
}
