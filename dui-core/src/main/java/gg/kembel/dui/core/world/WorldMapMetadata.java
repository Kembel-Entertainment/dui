package gg.kembel.dui.core.world;

import java.util.*;

/** A pack's geometry, bitmap advances and fonts, not a consumer's mutable map state. */
public record WorldMapMetadata(
    WorldMapDefinition definition,
    String font,
    Map<String, Glyph> glyphs,
    String geometryHash,
    String protocolHash) {
  public record Glyph(int character, int advance) {
    public Glyph {
      if (character < 0xE000 || character > 0xE7FF || advance < 1 || advance > 257)
        throw new IllegalArgumentException("Map glyph outside transport");
    }
  }

  public WorldMapMetadata {
    Objects.requireNonNull(definition);
    Objects.requireNonNull(font);
    glyphs = Map.copyOf(glyphs);
    if (!definition.geometryHash().equals(geometryHash)
        || !WorldMapProtocol.SHA256.equals(protocolHash))
      throw new IllegalArgumentException("World-map geometry/protocol mismatch");
    for (var layer : definition.layers())
      if (!glyphs.containsKey(layer.image()))
        throw new IllegalArgumentException("Missing map glyph: " + layer.image());
  }
}
