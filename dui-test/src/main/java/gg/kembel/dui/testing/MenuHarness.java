package gg.kembel.dui.testing;

import gg.kembel.dui.core.*;
import java.util.function.*;

/** Exercise pure consumer projections and typed actions without starting Paper. */
public final class MenuHarness<S> {
  private final S state;
  private final Function<S, Canvas> project;
  private final ActionRouter<S> actions;

  public MenuHarness(S state, Function<S, Canvas> project, ActionRouter<S> actions) {
    this.state = state;
    this.project = project;
    this.actions = actions;
  }

  public Canvas render() {
    Canvas c = project.apply(state);
    RenderAssertions.visibleHits(c);
    RenderAssertions.budget(c);
    return c;
  }

  public void click(String id) {
    var hit = RenderAssertions.hit(render(), id);
    if (hit.action().isBlank()) throw new AssertionError("Disabled control: " + id);
    actions.dispatch(state, hit.action(), hit.value());
  }
}
