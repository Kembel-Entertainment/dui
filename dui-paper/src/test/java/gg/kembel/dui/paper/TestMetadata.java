package gg.kembel.dui.paper;

import gg.kembel.dui.core.*;
import java.util.*;

final class TestMetadata {
  static PackMetadata create(
      String mc, String lib, String sha, Set<String> models, Map<String, Integer> metrics) {
    return new PackMetadata(
        mc,
        lib,
        sha,
        models,
        metrics,
        RendererProtocol.VERSION,
        Set.of("native", "shader-components-v1", "clips", "motion-tracks", "effects-32"),
        RendererProtocol.SCHEMA_SHA256);
  }

  static PackMetadata create(
      String mc,
      String lib,
      String sha,
      Set<String> models,
      Map<String, Integer> metrics,
      int protocol,
      Set<String> caps,
      String hash) {
    return new PackMetadata(mc, lib, sha, models, metrics, protocol, caps, hash);
  }
}
