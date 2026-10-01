package gg.kembel.dui.consumer;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ComponentRegistryTest {
  @Test
  void consumerRendererUsesItsSchemaAndAllocatedBounds() throws Exception {
    var registry =
        ComponentRegistry.builder()
            .renderer(
                "acme-status",
                Set.of("message"),
                18,
                c -> c.canvas().text(c.x(), c.y(), c.width(), c.node().s("message", ""), 0x123456))
            .build();
    var template =
        MenuTemplate.parse(
            "<dui-menu width='150' height='36'><dui-column>"
                + "<dui-acme-status message='{{text}}'/></dui-column></dui-menu>",
            new GlyphFont(),
            registry);
    assertEquals("consumer", template.render(Map.of("text", "consumer")).paints.getFirst().text());
    assertThrows(
        Exception.class,
        () ->
            MenuTemplate.parse(
                "<dui-menu><dui-acme-status typo='oops'/></dui-menu>", new GlyphFont(), registry));
  }

  @Test
  void fragmentsPreserveTypedPropsLiteralTextAndCallerSlotBindings() throws Exception {
    var registry =
        ComponentRegistry.builder()
            .template(
                "acme-card",
                Set.of("record"),
                """
                <dui-fragment><dui-column>
                  <dui-button id='{{props.record.id}}' label='{{props.record.label}}' action='select'/>
                  <dui-outlet name='footer'/>
                </dui-column></dui-fragment>
                """)
            .build();
    var template =
        MenuTemplate.parse(
            """
            <dui-menu width='150' height='36'><dui-acme-card record='{{record}}'>
              <dui-content name='footer'><dui-text label='{{caption}}'/></dui-content>
            </dui-acme-card></dui-menu>
            """,
            new GlyphFont(),
            registry);
    var canvas =
        template.render(
            Map.of("record", Map.of("id", "a", "label", "<x>& $\\ test"), "caption", "footer"));
    assertEquals("a", canvas.hits.getFirst().id());
    assertTrue(canvas.paints.stream().anyMatch(p -> "<x>& $\\ test".equals(p.text())));
    assertTrue(canvas.paints.stream().anyMatch(p -> "footer".equals(p.text())));
  }

  @Test
  void localFragmentsAndStylesRemainScoped() throws Exception {
    var template =
        MenuTemplate.parse(
            """
            <dui-menu width='150' height='36'>
              <dui-style id='ink' color='#FFFFFF'/>
              <dui-component name='acme-label' props='text'>
                <dui-style id='ink' color='#112233'/>
                <dui-text class='ink' label='{{props.text}}'/>
              </dui-component>
              <dui-column><dui-acme-label text='first'/><dui-text class='ink' label='second'/></dui-column>
            </dui-menu>
            """);
    var paints = template.render(Map.of()).paints;
    assertEquals(0x112233, paints.getFirst().color());
    assertEquals(0xFFFFFF, paints.getLast().color());
  }

  @Test
  void registrySnapshotsRecursionAndDiagnosticsAreBounded() throws Exception {
    var builder =
        ComponentRegistry.builder()
            .template(
                "acme-box",
                Set.of("label"),
                "<dui-fragment><dui-text label='{{props.label}}'/></dui-fragment>");
    var snapshot = builder.build();
    builder.renderer("acme-later", Set.of(), 18, c -> {});
    assertThrows(
        Exception.class,
        () ->
            MenuTemplate.parse(
                "<dui-menu><dui-acme-later/></dui-menu>", new GlyphFont(), snapshot));
    var recursive =
        MenuTemplate.parse(
            """
            <dui-menu><dui-component name='acme-loop'><dui-acme-loop/></dui-component><dui-acme-loop/></dui-menu>
            """);
    assertThrows(IllegalArgumentException.class, () -> recursive.render(Map.of()));
    var missing =
        MenuTemplate.parse(
            "<dui-menu><dui-acme-box/></dui-menu>", new GlyphFont(), snapshot, "ui/example.html");
    assertTrue(
        assertThrows(IllegalArgumentException.class, () -> missing.render(Map.of()))
            .getMessage()
            .contains("ui/example.html: Missing property label"));
    assertThrows(
        Exception.class,
        () ->
            MenuTemplate.parse(
                "<dui-menu><dui-acme-box label='hi' bad='x'/></dui-menu>",
                new GlyphFont(),
                snapshot));
  }

  @Test
  void defaultOutletAndNestedCompositionUseDistinctExplicitIds() throws Exception {
    var registry =
        ComponentRegistry.builder()
            .template(
                "acme-wrapper",
                Set.of(),
                "<dui-fragment><dui-column><dui-outlet"
                    + " name='default'/></dui-column></dui-fragment>")
            .build();
    var template =
        MenuTemplate.parse(
            "<dui-menu width='150' height='36'><dui-acme-wrapper><dui-button id='one' label='A'"
                + " action='a'/><dui-button id='two' label='B' action='b'/>"
                + "</dui-acme-wrapper></dui-menu>",
            new GlyphFont(),
            registry);
    assertEquals(
        List.of("one", "two"),
        template.render(Map.of()).hits.stream().map(Canvas.Hit::id).toList());
  }

  @Test
  void repeatedOutletsCannotBypassTheExpandedNodeBudget() throws Exception {
    var registry =
        ComponentRegistry.builder()
            .template(
                "acme-repeat",
                Set.of("rows"),
                "<dui-fragment><dui-layer><dui-repeat items='props.rows' as='row'>"
                    + "<dui-outlet name='default'/></dui-repeat></dui-layer></dui-fragment>")
            .build();
    var template =
        MenuTemplate.parse(
            "<dui-menu><dui-acme-repeat rows='{{rows}}'><dui-column><dui-repeat items='rows'"
                + " as='row'><dui-text label='x'/></dui-repeat></dui-column>"
                + "</dui-acme-repeat></dui-menu>",
            new GlyphFont(),
            registry);
    assertTrue(
        assertThrows(
                IllegalArgumentException.class,
                () -> template.render(Map.of("rows", Collections.nCopies(30, Map.of()))))
            .getMessage()
            .contains("Expanded node limit"));
  }

  @Test
  void callSiteClassesOverrideTheComposedRootStyle() throws Exception {
    var template =
        MenuTemplate.parse(
            """
            <dui-menu width='150' height='18'>
              <dui-style id='alert' color='#BB3344'/>
              <dui-component name='acme-label' props='label'><dui-text label='{{props.label}}' color='#FFFFFF'/></dui-component>
              <dui-acme-label label='alert' class='alert'/>
            </dui-menu>
            """);
    assertEquals(0xBB3344, template.render(Map.of()).paints.getFirst().color());
  }
}
