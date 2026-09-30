package gg.kembel.dui.pack;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Produces a deterministic pack plus its matching runtime metadata. */
public final class PackGenerator {
  private PackGenerator() {}

  public static void generate(Path clientJar, Path additionsDirectory, Path output)
      throws Exception {
    var additions = new TreeMap<String, byte[]>();
    if (additionsDirectory != null && Files.isDirectory(additionsDirectory))
      try (var paths = Files.walk(additionsDirectory)) {
        for (var p : paths.filter(Files::isRegularFile).sorted().toList()) {
          String name =
              additionsDirectory.relativize(p).toString().replace(File.separatorChar, '/');
          if (!name.startsWith("assets/") || name.contains(".."))
            throw new IOException("Invalid pack extension path: " + name);
          additions.put(name, Files.readAllBytes(p));
        }
      }
    Files.createDirectories(output);
    try (var assets = new VanillaAssets(clientJar)) {
      byte[] pack = CanvasPack.build(assets, additions);
      var sorted = new TreeMap<String, byte[]>();
      try (var zip = new ZipInputStream(new ByteArrayInputStream(pack))) {
        for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry())
          if (sorted.put(entry.getName(), zip.readAllBytes()) != null)
            throw new IOException("Duplicate pack entry: " + entry.getName());
      }
      var bytes = new ByteArrayOutputStream();
      try (var zip = new ZipOutputStream(bytes)) {
        for (var entry : sorted.entrySet()) {
          var e = new ZipEntry(entry.getKey());
          e.setTime(0);
          zip.putNextEntry(e);
          zip.write(entry.getValue());
          zip.closeEntry();
        }
      }
      pack = bytes.toByteArray();
      var models = new TreeSet<>(assets.itemDefinitions().keySet());
      models.add("dui:effect/panel");
      for (String name : additions.keySet())
        if (name.matches("assets/[^/]+/items/.+\\.json")) {
          var parts = name.split("/", 4);
          models.add(parts[1] + ":" + parts[3].substring(0, parts[3].length() - 5));
        }
      String sha1 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(pack));
      Files.write(output.resolve("dui.zip"), pack);
      Files.writeString(
          output.resolve("dui.json"),
          new GsonBuilder()
                  .setPrettyPrinting()
                  .create()
                  .toJson(
                      Map.of(
                          "minecraftVersion",
                          "26.2",
                          "libraryVersion",
                          "0.1.0-SNAPSHOT",
                          "sha1",
                          sha1,
                          "models",
                          models,
                          "fontMetrics",
                          assets.metrics()))
              + "\n");
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 2 || args.length > 3)
      throw new IllegalArgumentException(
          "Usage: PackGenerator <verified-client.jar> <output-directory> [own-assets-directory]");
    generate(Path.of(args[0]), args.length == 3 ? Path.of(args[2]) : null, Path.of(args[1]));
  }
}
