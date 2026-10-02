package gg.kembel.dui.pack;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Explicit, verified build input. No game binaries or textures are bundled with dui. */
public final class VanillaAssets implements AutoCloseable {
  private final ZipFile jar;

  public static JsonObject manifest() {
    try (var in = VanillaAssets.class.getResourceAsStream("/dui/minecraft.json")) {
      return JsonParser.parseString(
              new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8))
          .getAsJsonObject();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public VanillaAssets(Path path) throws IOException {
    this(path, manifest().getAsJsonObject("client").get("sha1").getAsString());
  }

  VanillaAssets(Path path, String expectedSha1) throws IOException {
    try (var in = Files.newInputStream(path)) {
      var digest = MessageDigest.getInstance("SHA-1");
      byte[] buffer = new byte[65536];
      for (int n; (n = in.read(buffer)) >= 0; ) digest.update(buffer, 0, n);
      if (!HexFormat.of().formatHex(digest.digest()).equals(expectedSha1))
        throw new IOException("Client JAR is not the verified Minecraft 26.2 build");
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
    jar = new ZipFile(path.toFile());
  }

  public byte[] read(String path) throws IOException {
    var entry = jar.getEntry(path);
    if (entry == null) throw new IOException("Missing vanilla asset: " + path);
    try (var in = jar.getInputStream(entry)) {
      return in.readAllBytes();
    }
  }

  private JsonArray providers(String id) throws IOException {
    var result = new JsonArray();
    var json =
        JsonParser.parseString(
                new String(read("assets/minecraft/font/" + id + ".json"), StandardCharsets.UTF_8))
            .getAsJsonObject();
    for (var entry : json.getAsJsonArray("providers")) {
      var p = entry.getAsJsonObject();
      String type = p.get("type").getAsString();
      if (type.equals("reference"))
        result.addAll(providers(p.get("id").getAsString().replace("minecraft:", "")));
      else if (type.equals("space")
          || type.equals("bitmap")
              && Set.of(
                      "minecraft:font/ascii.png",
                      "minecraft:font/accented.png",
                      "minecraft:font/nonlatin_european.png")
                  .contains(p.get("file").getAsString())) result.add(p.deepCopy());
    }
    return result;
  }

  public byte[] fontDefinition() throws IOException {
    var root = new JsonObject();
    root.add("providers", providers("default"));
    return root.toString().getBytes(StandardCharsets.UTF_8);
  }

  public Map<String, Integer> metrics() throws IOException {
    var result = new TreeMap<String, Integer>();
    for (var entry : providers("default")) {
      var p = entry.getAsJsonObject();
      if (p.get("type").getAsString().equals("space")) {
        for (var advance : p.getAsJsonObject("advances").entrySet())
          result.putIfAbsent(advance.getKey(), advance.getValue().getAsInt());
        continue;
      }
      String file = p.get("file").getAsString().replace("minecraft:", "assets/minecraft/textures/");
      java.awt.image.BufferedImage image;
      try (var in = new ByteArrayInputStream(read(file))) {
        image = javax.imageio.ImageIO.read(in);
      }
      var rows = p.getAsJsonArray("chars");
      int columns = rows.get(0).getAsString().codePointCount(0, rows.get(0).getAsString().length()),
          cellWidth = image.getWidth() / columns,
          cellHeight = image.getHeight() / rows.size();
      float scale = (p.has("height") ? p.get("height").getAsInt() : 8) / (float) cellHeight;
      for (int row = 0; row < rows.size(); row++) {
        int[] chars = rows.get(row).getAsString().codePoints().toArray();
        for (int col = 0; col < chars.length; col++) {
          if (chars[col] == 0) continue;
          String cp = new String(Character.toChars(chars[col]));
          if (result.containsKey(cp)) continue;
          int ink = 0;
          for (int x = 0; x < cellWidth; x++)
            for (int y = 0; y < cellHeight; y++)
              if ((image.getRGB(col * cellWidth + x, row * cellHeight + y) >>> 24) != 0)
                ink = Math.max(ink, x + 1);
          result.put(cp, Math.round(ink * scale) + 1);
        }
      }
    }
    return Collections.unmodifiableMap(result);
  }

  public JsonObject itemDefinitions() throws IOException {
    var result = new JsonObject();
    var entries =
        jar.stream()
            .filter(
                e ->
                    e.getName().startsWith("assets/minecraft/items/")
                        && e.getName().endsWith(".json"))
            .sorted(Comparator.comparing(ZipEntry::getName))
            .toList();
    for (var entry : entries) {
      String id =
          "minecraft:"
              + entry
                  .getName()
                  .substring("assets/minecraft/items/".length(), entry.getName().length() - 5);
      result.add(
          id, JsonParser.parseString(new String(read(entry.getName()), StandardCharsets.UTF_8)));
    }
    return result;
  }

  /** Same source pixels/advances as the native font, normalized to the canvas baseline. */
  public gg.kembel.dui.core.BitmapFont bitmapFont() throws IOException {
    var result = new TreeMap<String, gg.kembel.dui.core.BitmapFont.Glyph>();
    var metrics = metrics();
    for (var entry : providers("default")) {
      var p = entry.getAsJsonObject();
      if (p.get("type").getAsString().equals("space")) continue;
      var image =
          javax.imageio.ImageIO.read(
              new ByteArrayInputStream(
                  read(
                      p.get("file")
                          .getAsString()
                          .replace("minecraft:", "assets/minecraft/textures/"))));
      var rows = p.getAsJsonArray("chars");
      int columns = rows.get(0).getAsString().codePointCount(0, rows.get(0).getAsString().length());
      int cw = image.getWidth() / columns,
          ch = image.getHeight() / rows.size(),
          ascent = p.get("ascent").getAsInt();
      for (int row = 0; row < rows.size(); row++) {
        int[] chars = rows.get(row).getAsString().codePoints().toArray();
        for (int col = 0; col < chars.length; col++) {
          String key = Character.toString(chars[col]);
          if (chars[col] == 0 || result.containsKey(key) || !metrics.containsKey(key)) continue;
          var pixels = new ArrayList<Integer>();
          for (int yy = 0; yy < 9; yy++)
            for (int xx = 0; xx < cw; xx++) {
              int sy = yy - 7 + ascent;
              pixels.add(sy < 0 || sy >= ch ? 0 : image.getRGB(col * cw + xx, row * ch + sy));
            }
          result.put(key, new gg.kembel.dui.core.BitmapFont.Glyph(cw, 9, metrics.get(key), pixels));
        }
      }
    }
    metrics.forEach(
        (key, advance) ->
            result.putIfAbsent(
                key,
                new gg.kembel.dui.core.BitmapFont.Glyph(1, 9, advance, Collections.nCopies(9, 0))));
    return new gg.kembel.dui.core.BitmapFont("dui:default", 9, result);
  }

  @Override
  public void close() throws IOException {
    jar.close();
  }
}
