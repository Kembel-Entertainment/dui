package gg.kembel.dui.pack;

import gg.kembel.dui.core.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import javax.imageio.ImageIO;

/** A tagged zero-net-advance glyph masks only the padded native border of our canvases. */
public final class FocusGuard {
  public static final char GLYPH = '\uECF0';
  // Bitmap providers require ascent <= height. Scale the 4px marker to an 8px glyph.
  public static final int ADVANCE = 9;

  private FocusGuard() {}

  public static int payload(Canvas canvas) {
    return canvas.width | (canvas.height << 9);
  }

  static void write(ZipOutputStream zip) throws IOException {
    // The low blue bits identify each corner independently of batched vertex indices.
    var bitmap = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < 4; y++)
      for (int x = 0; x < 4; x++)
        bitmap.setRGB(x, y, 0xFFF804E0 + (x >= 2 ? 1 : 0) + (y >= 2 ? 2 : 0));
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(bitmap, "png", bytes);
    put(zip, "assets/dui/textures/gui/canvas/focus_guard.png", bytes.toByteArray());
    put(
        zip,
        "assets/dui/font/focus_guard.json",
        ("{\"providers\":[{\"type\":\"bitmap\",\"file\":\"dui:gui/canvas/focus_guard.png\",\"height\":8,\"ascent\":7,\"chars\":[\""
                + GLYPH
                + "\"]}]}")
            .getBytes(StandardCharsets.UTF_8));
    for (String ext : new String[] {"vsh", "fsh"})
      try (var in = FocusGuard.class.getResourceAsStream("/ui/shader/text." + ext)) {
        put(
            zip,
            "assets/minecraft/shaders/core/text." + ext,
            shader(ext, java.util.Objects.requireNonNull(in).readAllBytes()));
      }
  }

  private static byte[] shader(String ext, byte[] bytes) throws IOException {
    if (!ext.equals("fsh")) return bytes;
    try (var in = FocusGuard.class.getResourceAsStream("/ui/shader/player-model.glsl")) {
      return new String(bytes, StandardCharsets.UTF_8)
          .replace(
              "// PLAYER_FUNCTIONS",
              new String(
                  java.util.Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8))
          .getBytes(StandardCharsets.UTF_8);
    }
  }

  private static void put(ZipOutputStream zip, String path, byte[] bytes) throws IOException {
    var entry = new ZipEntry(path);
    entry.setTime(0);
    zip.putNextEntry(entry);
    zip.write(bytes);
    zip.closeEntry();
  }
}
