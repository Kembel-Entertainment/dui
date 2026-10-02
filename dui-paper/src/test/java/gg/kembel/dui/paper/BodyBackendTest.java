package gg.kembel.dui.paper;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class BodyBackendTest {
  @Test
  void consumerMustDeclareTransformedGeometrySupport() {
    BodyBackend backend =
        new BodyBackend() {
          public Set<String> requiredPackCapabilities() {
            return Set.of("own:panel");
          }

          public Set<String> geometryCapabilities() {
            return Set.of();
          }

          public List<io.papermc.paper.registry.data.dialog.body.DialogBody> render(
              RenderPrimitive p, ViewModel m) {
            return List.of();
          }
        };
    var bounds = new Scene.Rect(0, 0, 18, 18);
    assertDoesNotThrow(
        () -> backend.validate(new RenderPrimitive("p", "own:panel", bounds, Map.of())));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            backend.validate(
                new RenderPrimitive(
                    "p", "own:panel", bounds, Map.of(), Transform2D.translation(1, 2), null, 1)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            backend.validate(
                new RenderPrimitive(
                    "p", "own:panel", bounds, Map.of(), Transform2D.IDENTITY, bounds, .5)));
  }
}
