package gg.kembel.dui.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class LayoutUtilitiesTest {
  @Test
  void paginationClampsAndHandlesEmptyAndHugeSizesWithoutOverflow() {
    assertEquals(1, Page.of(List.of(), 99, 2).pages());
    assertEquals(List.of(3), Page.of(List.of(1, 2, 3), 99, 2).items());
    assertEquals(0, Page.of(List.of(1), -99, 1).index());
    assertEquals(1, Page.count(Integer.MAX_VALUE, Integer.MAX_VALUE));
    assertThrows(IllegalArgumentException.class, () -> Page.of(List.of(), 0, 0));
  }

  @Test
  void allocationReservesMinimumsThenHonoursCallerPriority() {
    var groups =
        RenderBudget.allocate(
            8, new RenderBudget.Request("dealer", 10, 0), new RenderBudget.Request("player", 2, 2));
    assertEquals(Map.of("dealer", 6, "player", 2), groups);
    assertEquals(
        Map.of("dealer", 2, "player", 6),
        RenderBudget.allocate(
            8,
            new RenderBudget.Request("dealer", 2, 0),
            new RenderBudget.Request("player", 10, 4)));
    assertThrows(
        IllegalArgumentException.class,
        () -> RenderBudget.allocate(3, new RenderBudget.Request("required", 4, 4)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            RenderBudget.allocate(
                8, new RenderBudget.Request("same", 1, 0), new RenderBudget.Request("same", 1, 0)));
  }

  @Test
  void textWrapUsesInjectedMetricsAndRightAlignmentUsesMeasuredWidth() throws Exception {
    var font = new GlyphFont(Map.of("?", 6, "A", 10, "B", 8, ".", 2, " ", 3));
    assertEquals(List.of("AA", "BB"), font.wrap("AA BB", 20, 2));
    assertTrue(font.width(font.fit("AAAA", 1)) <= 1);
    assertTrue(font.wrap("AAAAAAAA", 20, 2).stream().allMatch(line -> font.width(line) <= 20));
    var c =
        MenuTemplate.parse(
                "<dui-menu width='120' height='36'><dui-column>"
                    + "<dui-text height='18' label='AA' align='right'/>"
                    + "<dui-text height='18' width='fill' label='AA BB' wrap='true' max-lines='2'/>"
                    + "</dui-column></dui-menu>",
                font)
            .render(Map.of());
    assertEquals(100, c.paints.getFirst().x());
    assertTrue(c.paints.stream().allMatch(p -> p.x() + p.width() <= 120));
  }

  @Test
  void anchoredRelativeLengthsAdaptToTheirAllocatedParent() throws Exception {
    var c =
        MenuTemplate.parse(
                "<dui-menu width='240' height='54'><dui-layer height='fill'><dui-button id='right'"
                    + " action='go' width='25%' height='18' anchor-x='right' x='6'"
                    + " anchor-y='bottom' y='9'/><dui-rect x='6' y='0' width='fill-12' height='1'/>"
                    + "</dui-layer></dui-menu>")
            .render(Map.of());
    assertEquals(58, c.hits.getFirst().width());
    assertEquals(176, c.hits.getFirst().x());
    assertEquals(27, c.hits.getFirst().y());
    assertEquals(222, c.paints.getLast().width());
    assertThrows(IllegalArgumentException.class, () -> LayoutLength.resolve("101%", 240));
    assertThrows(IllegalArgumentException.class, () -> LayoutLength.resolve("fill-300", 240));
  }

  @Test
  void timelineCompositionPreservesOffsetsAndRemainingClock() {
    var plan =
        AnimationTimeline.sequence(
            AnimationTimeline.of("open", 24),
            AnimationTimeline.parallel(
                AnimationTimeline.of("confetti", 72), AnimationTimeline.of("reward", 48)));
    assertEquals(96, plan.duration());
    assertEquals(24, plan.segment("confetti").start());
    assertEquals(72, plan.remaining(100, 124));
    assertEquals(0, plan.remaining(100, 250));
    assertEquals(96, plan.remaining(100, 90));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            AnimationTimeline.parallel(
                AnimationTimeline.of("same", 2), AnimationTimeline.of("same", 3)));
    assertEquals(29, plan.delayed(5).segment("reward").start());
  }
}
