package consumer;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import gg.kembel.dui.testing.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class HarnessTest {
  @Test
  void lateProviderAndReplacedViewCannotRepaint() {
    var scheduler = new FakeScheduler();
    int[] calls = {0};
    var session = new TaskScope(scheduler);
    var view = new TaskScope(scheduler);
    view.later("animation", 5, () -> calls[0]++);
    view.close();
    var request = new CompletableFuture<String>();
    session.latest("load", request, (value, error) -> calls[0]++);
    session.close();
    request.complete("late");
    scheduler.advance(10);
    assertEquals(0, calls[0]);
    assertEquals(0, scheduler.pending());
  }

  @Test
  void actionSequenceUsesTheActualHitPayload() {
    int[] state = {0};
    var router = new ActionRouter<int[]>((s, a) -> fail(a));
    router.on("increment", Integer::parseInt, (s, n) -> s[0] += n);
    var harness =
        new MenuHarness<>(
            state,
            s -> {
              var c = new Canvas(180, 90);
              c.hit(new Canvas.Hit("add", "increment", "2", "", 0, 0, 18, 18));
              return c;
            },
            router);
    harness.click("add");
    harness.click("add");
    assertEquals(4, state[0]);
  }
}
