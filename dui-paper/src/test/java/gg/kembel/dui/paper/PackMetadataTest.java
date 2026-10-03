package gg.kembel.dui.paper;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.net.URI;
import java.util.*;
import org.junit.jupiter.api.Test;

class PackMetadataTest {
  @Test
  void videoOnlyMetadataNeedsSharedHudMetricsButNoMapDefinitions() {
    var base = TestMetadata.create("26.2", "0.2.0-SNAPSHOT", "a".repeat(40), Set.of(), Map.of("?", 6));
    var gson = new com.google.gson.Gson();
    var json = gson.toJsonTree(base).getAsJsonObject();
    json.getAsJsonArray("capabilities").add(gg.kembel.dui.core.video.MapVideoCodec.CAPABILITY);
    assertThrows(RuntimeException.class, () -> gson.fromJson(json, PackMetadata.class));
    json.add("worldMapLegendMetrics", gson.toJsonTree(Map.of("?", 6)));
    var video = gson.fromJson(json, PackMetadata.class);
    assertTrue(video.worldMaps().isEmpty());
    assertDoesNotThrow(() -> new GlyphFont(video.worldMapLegendMetrics()));
  }

  @Test
  void serializedShaderSchemaFingerprintsAndRuntimeBindingsMustMatch() {
    var spec = new ShaderSpec("external:decoration", List.of(ShaderSpec.Parameter.rgb("tint")));
    var base =
        TestMetadata.create(
            "26.2", "0.2.0-SNAPSHOT", "b".repeat(40), Set.of("minecraft:paper"), Map.of("?", 6));
    var metadata =
        new PackMetadata(
            base.minecraftVersion(),
            base.libraryVersion(),
            base.sha1(),
            base.models(),
            base.fontMetrics(),
            base.protocolVersion(),
            base.capabilities(),
            base.codecHash(),
            Map.of(),
            Map.of(),
            ShaderRegistry.bind(List.of(spec)),
            Map.of());
    var gson = new com.google.gson.Gson();
    String json = gson.toJson(metadata);
    assertEquals(metadata, gson.fromJson(json, PackMetadata.class));
    assertThrows(
        RuntimeException.class,
        () -> gson.fromJson(json.replace(spec.hash(), "0".repeat(64)), PackMetadata.class));
    var canvas = TestEnvironment.canvas(180, 90);
    canvas.effect(
        new ShaderInvocation("decorative", spec, 9, 9, 27, 27, Map.of("tint", 0x123456), 0));
    assertDoesNotThrow(() -> metadata.validate(canvas));
    var changed = new ShaderSpec(spec.id(), List.of(ShaderSpec.Parameter.integer("tint", 0, 255)));
    var mismatch = TestEnvironment.canvas(180, 90);
    mismatch.effect(
        new ShaderInvocation("decorative", changed, 9, 9, 27, 27, Map.of("tint", 123), 0));
    assertThrows(IllegalArgumentException.class, () -> metadata.validate(mismatch));
  }

  @Test
  void metadataAndDescriptorValidateTheVersionAndStablePackIdentity() {
    var m =
        TestMetadata.create(
            "26.2", "0.2.0-SNAPSHOT", "a".repeat(40), Set.of("minecraft:paper"), Map.of("?", 6));
    var a = PackDescriptor.of(URI.create("https://example.com/dui.zip"), m);
    assertEquals(a.id(), PackDescriptor.of(URI.create("https://example.com/other.zip"), m).id());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            TestMetadata.create(
                "1.21", "0.2.0-SNAPSHOT", "a".repeat(40), Set.of(), Map.of("?", 6)));
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
