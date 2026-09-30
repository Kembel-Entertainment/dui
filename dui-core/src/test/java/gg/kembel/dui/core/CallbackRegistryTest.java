package gg.kembel.dui.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CallbackRegistryTest {
  @Test
  void foreignPlayersReplayAndSiblingTokensCannotAct() {
    var registry = new CallbackRegistry<String>();
    var a = UUID.randomUUID();
    var b = UUID.randomUUID();
    var token = registry.register(a, "press");
    var sibling = registry.register(a, "close");
    assertNull(registry.consume(b, token));
    assertEquals("press", registry.consume(a, token));
    assertNull(registry.consume(a, token));
    assertNull(registry.consume(a, sibling));
  }

  @Test
  void updatesQuitAndShutdownInvalidateTokens() {
    var r = new CallbackRegistry<String>();
    var p = UUID.randomUUID();
    var t = r.register(p, "old");
    r.invalidate(p);
    assertNull(r.consume(p, t));
    var n = r.register(p, "new");
    r.clear();
    assertNull(r.consume(p, n));
  }
}
