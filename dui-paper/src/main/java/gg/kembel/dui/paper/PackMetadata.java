package gg.kembel.dui.paper;

import com.google.gson.*;
import gg.kembel.dui.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public record PackMetadata(
    String minecraftVersion,
    String libraryVersion,
    String sha1,
    Set<String> models,
    Map<String, Integer> fontMetrics,
    int protocolVersion,
    Set<String> capabilities,
    String codecHash) {
  public PackMetadata(
      String mc, String lib, String sha, Set<String> models, Map<String, Integer> metrics) {
    this(mc, lib, sha, models, metrics, 1, Set.of("native", "effects-8", "clips"), "");
  }

  public boolean supports(String capability) {
    return capabilities.contains(capability);
  }

  public void validate(Canvas canvas) {
    if (!canvas.playerModels.isEmpty() && !supports(PlayerModelCodec.CAPABILITY))
      throw new IllegalArgumentException("Pack lacks player-model-v1 capability");
    for (var effect : canvas.effects)
      if (effect.kind() instanceof ShaderEffect.Extension e
          && !supports("effect:" + e.id() + "@" + e.code()))
        throw new IllegalArgumentException("Pack lacks effect extension: " + e.id());
    if (canvas.effects.size() > 8 && !supports("effects-32"))
      throw new IllegalArgumentException("Pack lacks effects-32 capability");
    if ((canvas.motions.keySet().stream().anyMatch(id -> !canvas.legacyMotion(id))
            || !canvas.effectMotions.isEmpty())
        && !supports("motion-tracks"))
      throw new IllegalArgumentException("Pack lacks motion-tracks capability");
  }

  public PackMetadata {
    if (!"26.2".equals(minecraftVersion)
        || !"0.1.0-SNAPSHOT".equals(libraryVersion)
        || sha1 == null
        || !sha1.matches("[a-f0-9]{40}"))
      throw new IllegalArgumentException("Unsupported or invalid dui pack metadata");
    if (protocolVersion == 0) protocolVersion = 1; // Missing metadata is explicit legacy v1.
    if (protocolVersion != 1 && protocolVersion != RendererProtocol.VERSION)
      throw new IllegalArgumentException("Unsupported renderer protocol: " + protocolVersion);
    capabilities =
        capabilities == null ? Set.of("native", "effects-8", "clips") : Set.copyOf(capabilities);
    if (protocolVersion == RendererProtocol.VERSION
        && (!RendererProtocol.SCHEMA_SHA256.equals(codecHash)
            || !capabilities.containsAll(
                Set.of("native", "effects-8", "clips", "motion-tracks", "effects-32"))))
      throw new IllegalArgumentException("Renderer codec/capabilities mismatch");
    if (protocolVersion == 1
        && (capabilities.contains("motion-tracks") || capabilities.contains("effects-32")))
      throw new IllegalArgumentException("Legacy pack claims v2 capabilities");
    var extensionIds = new HashSet<String>();
    var extensionCodes = new HashSet<Integer>();
    for (String capability : capabilities) {
      if (Set.of(
              "native",
              "effects-8",
              "clips",
              "motion-tracks",
              "effects-32",
              PlayerModelCodec.CAPABILITY)
          .contains(capability)) continue;
      if (!capability.matches("effect:[a-z][a-z0-9_-]*:[a-z][a-z0-9_/-]*@(?:[89]|1[0-5])"))
        throw new IllegalArgumentException("Unknown pack capability: " + capability);
      String id = capability.substring(7, capability.lastIndexOf('@'));
      int code = Integer.parseInt(capability.substring(capability.lastIndexOf('@') + 1));
      if (!extensionIds.add(id) || !extensionCodes.add(code))
        throw new IllegalArgumentException("Pack extension collision: " + capability);
    }
    models = Set.copyOf(models);
    fontMetrics = Map.copyOf(fontMetrics);
    new GlyphFont(fontMetrics);
  }

  public static PackMetadata read(Path path) throws IOException {
    return new Gson().fromJson(Files.readString(path), PackMetadata.class);
  }

  public GlyphFont font() {
    return new GlyphFont(fontMetrics);
  }
}
