package gg.kembel.dui.components;

import gg.kembel.dui.core.*;
import java.util.*;

/** Optional namespaced visual package; business rules and assets are supplied by the consumer. */
public final class VisualComponents {
  private VisualComponents() {}

  public static ComponentRegistry registry() {
    var builder = ComponentRegistry.builder();
    builder.template(
        "chrome-header",
        Set.of("title", "detail"),
        "<dui-fragment><dui-column height=\"36\" gap=\"9\"><dui-heading label=\"{{props.title}}\""
            + " height=\"18\" color=\"$accent\"/><dui-text label=\"{{props.detail}}\" height=\"9\""
            + " color=\"$muted\"/></dui-column></dui-fragment>");
    builder.template(
        "chrome-pagination",
        Set.of("label", "previous", "next", "previous-locked", "next-locked"),
        "<dui-fragment><dui-row height=\"18\" gap=\"9\"><dui-button id=\"previous\""
            + " action=\"{{props.previous}}\" payload=\"-1\" label=\"Previous\""
            + " locked=\"{{props.previous-locked}}\"/><dui-text label=\"{{props.label}}\""
            + " align=\"center\"/><dui-button id=\"next\" action=\"{{props.next}}\" payload=\"1\""
            + " label=\"Next\" locked=\"{{props.next-locked}}\"/></dui-row></dui-fragment>");
    for (String name :
        List.of("playing-card", "chip-stack", "wheel", "reel", "lever", "particles", "lights")) {
      var attributes = new HashSet<>(ComponentSchemas.get(name).attributes());
      attributes.addAll(Motion.ATTRIBUTES);
      attributes.add("animation-start");
      builder.renderer(
          "visual-" + name,
          attributes,
          54,
          ctx -> {
            var node = ctx.node();
            var properties = new HashMap<>(node.props());
            properties.remove("class");
            EffectComponent.draw(
                ctx.canvas(),
                new MenuTemplate.Node(name, properties, node.children()),
                ctx.x(),
                ctx.y(),
                ctx.width(),
                ctx.height());
            Motion.from(
                    node.props(),
                    Long.parseLong(
                        node.s("animation-start", Long.toString(ctx.canvas().animationStart))),
                    ctx.canvas().motionEnabled)
                .ifPresent(m -> ctx.canvas().effectMotion(node.s("id", ""), m));
          });
    }
    return builder.build();
  }
}
