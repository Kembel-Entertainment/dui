package gg.kembel.dui.runtime;

import static org.junit.jupiter.api.Assertions.*;
import gg.kembel.dui.core.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;

class RuntimeLifecycleTest {
  @Test void packGatingRejectsForeignAcknowledgementsAndAllowsRetry() {
    var f = new RuntimeFixture();
    try (var dui = f.dui) {
      var s = f.open(f.viewer, action -> {});
      assertEquals(1, f.transport.offers); assertNull(s.component());
      dui.resourcePack(f.viewer, UUID.randomUUID(), "SUCCESSFULLY_LOADED");
      assertNull(s.component());
      dui.resourcePack(f.viewer, f.pack.id(), "DECLINED");
      assertFalse(s.isActive()); assertEquals(1, f.viewer.messages);
      var retry = f.open(f.viewer, action -> {}); assertEquals(2, f.transport.offers);
      f.loaded(f.viewer); assertNotNull(retry.component()); assertEquals(1, retry.bodyCount());
    }
  }
  @Test void callbacksAreViewerScopedSingleUseAndInvalidatedByUpdatesAndReplacement() {
    var f = new RuntimeFixture(); var calls = new AtomicInteger();
    try (var dui = f.dui) {
      f.loaded(f.viewer); var other = new RuntimeFixture.Viewer(); f.loaded(other);
      var s = f.open(f.viewer, c -> calls.incrementAndGet()); f.open(other, c -> calls.addAndGet(100));
      var key = f.hitKey(f.viewer);
      assertFalse(dui.click(other, key, DialogResponse.EMPTY));
      assertTrue(dui.click(f.viewer, key, DialogResponse.EMPTY)); assertEquals(1, calls.get());
      assertFalse(dui.click(f.viewer, key, DialogResponse.EMPTY));
      s.update(f.canvas(), ViewModel.data(Map.of()), DialogOptions.notice("New", "Close", "close"), c -> calls.incrementAndGet());
      var old = f.hitKey(f.viewer);
      s.update(f.canvas(), ViewModel.data(Map.of()), DialogOptions.notice("Next", "Close", "close"), c -> calls.incrementAndGet());
      assertFalse(dui.click(f.viewer, old, DialogResponse.EMPTY));
      var current = f.hitKey(f.viewer); f.open(f.viewer, c -> calls.incrementAndGet());
      assertFalse(s.isActive()); assertFalse(dui.click(f.viewer, current, DialogResponse.EMPTY));
    }
  }
  @Test void invalidOptionsLeaveExistingCallbacksAndJobsAlive() {
    var f = new RuntimeFixture(); var calls = new AtomicInteger();
    try (var dui = f.dui) {
      f.loaded(f.viewer); var s = f.open(f.viewer, c -> calls.incrementAndGet()); var key = f.hitKey(f.viewer);
      s.viewTasks().later("pending", 1, () -> calls.addAndGet(10));
      assertThrows(NullPointerException.class, () -> s.update(f.canvas(), ViewModel.data(Map.of()), null, c -> {}));
      f.viewer.clock.advance(1); assertEquals(10, calls.get());
      assertTrue(dui.click(f.viewer, key, DialogResponse.EMPTY)); assertEquals(11, calls.get());
    }
  }
  @Test void replacingViewAndQuittingCancelUiWorkWithoutCancellingProviders() {
    var f = new RuntimeFixture(); var delivered = new AtomicInteger(); var provider = new CompletableFuture<String>();
    try (var dui = f.dui) {
      f.loaded(f.viewer); var s = f.open(f.viewer, c -> {});
      s.viewTasks().latest("old", provider, (result, error) -> delivered.incrementAndGet());
      s.tasks().later("session", 3, delivered::incrementAndGet);
      s.update(f.canvas(), ViewModel.data(Map.of()), DialogOptions.notice("Next", "Close", "close"), c -> {});
      provider.complete("ready"); f.viewer.clock.drain(); assertEquals(0, delivered.get()); assertFalse(provider.isCancelled());
      dui.quit(f.viewer); f.viewer.clock.advance(5); assertFalse(s.isActive()); assertEquals(0, delivered.get());
    }
  }
  @Test void renderingFailureClosesSessionAndInvalidatesCallbacks() {
    var f = new RuntimeFixture();
    try (var dui = f.dui) {
      f.loaded(f.viewer); var s = f.open(f.viewer, c -> {}); var key = f.hitKey(f.viewer); f.transport.failShow = true;
      assertThrows(IllegalStateException.class, () -> s.update(f.canvas(), ViewModel.data(Map.of()), DialogOptions.notice("Next", "Close", "close"), c -> {}));
      assertFalse(s.isActive()); assertFalse(dui.click(f.viewer, key, DialogResponse.EMPTY)); assertEquals(1, f.transport.closes);
    }
  }
  @Test void viewerOwnershipCanMoveAcrossJavaThreads() throws Exception {
    var f = new RuntimeFixture(); var sampled = new AtomicInteger();
    try (var dui = f.dui) {
      f.loaded(f.viewer); var s = f.open(f.viewer, c -> {}); s.tasks().later("later", 1, sampled::incrementAndGet);
      var error = new AtomicReference<Throwable>();
      var next = new Thread(() -> {
        f.viewer.owner = Thread.currentThread();
        try { f.viewer.clock.advance(1); s.close(); } catch (Throwable failure) { error.set(failure); }
      });
      next.start(); next.join(); f.viewer.owner = Thread.currentThread();
      assertNull(error.get()); assertEquals(1, sampled.get()); assertFalse(s.isActive());
    }
  }
  @Test void wrongOwnerFailsBeforeAStateMutation() throws Exception {
    var f = new RuntimeFixture();
    try (var dui = f.dui) {
      var error = new AtomicReference<Throwable>();
      var wrong = new Thread(() -> { try { f.open(f.viewer, c -> {}); } catch (Throwable failure) { error.set(failure); } });
      wrong.start(); wrong.join();
      assertInstanceOf(IllegalStateException.class, error.get()); assertEquals(0, f.transport.offers);
    }
  }
  @Test void inputDefinitionsAreImmutableAndValidateAmbiguousKeys() {
    var title = net.kyori.adventure.text.Component.text("Fixture");
    var input = new DialogInput.Text("name", 200, title, true, "Initial", 64, new DialogInput.Text.Multiline(2, 24));
    assertThrows(IllegalArgumentException.class, () -> new DialogOptions(title, List.of(input, input), List.of(), new DialogOptions.Button("Close", "close"), 1, false));
    assertThrows(IllegalArgumentException.class, () -> new DialogInput.NumberRange("range", 200, title, "%s", 0, 10, 12f, 1f));
    assertThrows(IllegalArgumentException.class, () -> new DialogInput.SingleOption("option", 200, List.of(new DialogInput.SingleOption.Option("a", title, true), new DialogInput.SingleOption.Option("b", title, true)), title, true));
  }
  @Test void throwingCloseHandlerDoesNotLeakOtherViewerSessions() {
    var f = new RuntimeFixture(); var other = new RuntimeFixture.Viewer();
    var first = f.open(f.viewer, c -> {}).onClose(() -> { throw new IllegalStateException("Consumer close failure"); });
    var second = f.open(other, c -> {});
    f.dui.close();
    assertFalse(first.isActive()); assertFalse(second.isActive());
    assertThrows(IllegalStateException.class, () -> f.open(other, c -> {}));
  }
}
