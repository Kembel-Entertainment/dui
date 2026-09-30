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
    Map<String, Integer> fontMetrics) {
  public PackMetadata {
    if (!"26.2".equals(minecraftVersion)
        || !"0.1.0-SNAPSHOT".equals(libraryVersion)
        || sha1 == null
        || !sha1.matches("[a-f0-9]{40}"))
      throw new IllegalArgumentException("Unsupported or invalid dui pack metadata");
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
