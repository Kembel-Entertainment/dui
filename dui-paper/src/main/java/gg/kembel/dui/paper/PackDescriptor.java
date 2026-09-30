package gg.kembel.dui.paper;

import java.net.URI;
import java.util.*;

public record PackDescriptor(UUID id, URI uri, String sha1, boolean required) {
  public PackDescriptor {
    if (id == null
        || uri == null
        || !Set.of("http", "https").contains(uri.getScheme())
        || uri.getHost() == null
        || sha1 == null
        || !sha1.matches("[a-f0-9]{40}"))
      throw new IllegalArgumentException("Invalid resource pack descriptor");
  }

  public static PackDescriptor of(URI uri, PackMetadata metadata) {
    return new PackDescriptor(
        UUID.nameUUIDFromBytes(
            ("dui:" + metadata.sha1()).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
        uri,
        metadata.sha1(),
        false);
  }
}
