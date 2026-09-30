package gg.kembel.dui.paper;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.*;
import org.junit.jupiter.api.Test;

class PackMetadataTest {
  @Test
  void metadataAndDescriptorValidateTheVersionAndStablePackIdentity() {
    var m =
        new PackMetadata(
            "26.2", "0.1.0-SNAPSHOT", "a".repeat(40), Set.of("minecraft:paper"), Map.of("?", 6));
    var a = PackDescriptor.of(URI.create("https://example.com/dui.zip"), m);
    assertEquals(a.id(), PackDescriptor.of(URI.create("https://example.com/other.zip"), m).id());
    assertThrows(
        IllegalArgumentException.class,
        () -> new PackMetadata("1.21", "0.1.0-SNAPSHOT", "a".repeat(40), Set.of(), Map.of("?", 6)));
    assertThrows(
        IllegalArgumentException.class,
        () -> PackDescriptor.of(URI.create("file:///tmp/dui.zip"), m));
  }

  @Test
  void externallyOpenedUiLinksAreExplicitHttpUrls() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ViewModel(
                Map.of(), Map.of(), Map.of(), Map.of("button", URI.create("javascript:alert(1)"))));
    assertDoesNotThrow(
        () ->
            new ViewModel(
                Map.of(), Map.of(), Map.of(), Map.of("button", URI.create("https://kembel.gg"))));
  }
}
