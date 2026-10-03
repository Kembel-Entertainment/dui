package gg.kembel.dui.pack;

import gg.kembel.dui.core.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import javax.imageio.ImageIO;

/** A tagged zero-net-advance glyph masks only the padded native border of our canvases. */
public final class FocusGuard {
  private FocusGuard() {}

  static void write(ZipOutputStream zip) throws IOException {
    write(zip, false);
  }

  static void write(ZipOutputStream zip, boolean worldMaps) throws IOException {
    int count = (480 - 120 + 1) * FocusMarker.ROWS,
        columns = 128,
        rows = (count + columns - 1) / columns;
    var bitmap = new BufferedImage(columns * 4, rows * 4, BufferedImage.TYPE_INT_ARGB);
    var chars = new com.google.gson.JsonArray();
    for (int row = 0; row < rows; row++) {
      var text = new StringBuilder();
      for (int column = 0; column < columns; column++) {
        int index = row * columns + column;
        if (index >= count) {
          text.append('\0');
          continue;
        }
        text.appendCodePoint(FocusMarker.BASE + index);
        int width = 120 + index / FocusMarker.ROWS, heightRows = index % FocusMarker.ROWS + 1;
        int green = (width >> 8) | (heightRows << 1);
        for (int y = 0; y < 4; y++)
          for (int x = 0; x < 4; x++) {
            int corner = (x >= 2 ? 1 : 0) + (y >= 2 ? 2 : 0);
            bitmap.setRGB(
                column * 4 + x,
                row * 4 + y,
                (248 << 24) | ((width & 255) << 16) | (green << 8) | 224 | corner);
          }
      }
      chars.add(text.toString());
    }
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(bitmap, "png", bytes);
    put(zip, "assets/dui/textures/gui/canvas/focus_guard.png", bytes.toByteArray());
    var provider = new com.google.gson.JsonObject();
    provider.addProperty("type", "bitmap");
    provider.addProperty("file", "dui:gui/canvas/focus_guard.png");
    provider.addProperty("height", 8);
    provider.addProperty("ascent", 7);
    provider.add("chars", chars);
    put(
        zip,
        "assets/dui/font/focus_guard.json",
        ("{\"providers\":[" + provider + "]}").getBytes(StandardCharsets.UTF_8));
    for (String name : new String[] {"symbols", "vertex", "fragment"})
      try (var in = FocusGuard.class.getResourceAsStream("/ui/shader/video-" + name + ".glsl")) {
        put(zip, "assets/dui/shaders/include/video-" + name + ".glsl",
            java.util.Objects.requireNonNull(in).readAllBytes());
      }
    for (String ext : new String[] {"vsh", "fsh"})
      try (var in = FocusGuard.class.getResourceAsStream("/ui/shader/text." + ext)) {
        put(
            zip,
            "assets/minecraft/shaders/core/text." + ext,
            shader(ext, java.util.Objects.requireNonNull(in).readAllBytes()));
      }
  }

  private static byte[] shader(String ext, byte[] bytes) throws IOException {
    String source = new String(baseShader(ext, bytes), StandardCharsets.UTF_8);
    if (ext.equals("vsh")) {
      source = source.replace("out vec2 texCoord0;", "out vec2 texCoord0;\n"
          + "flat out int duiVideoFormat;flat out int duiVideoBackground;"
          + "flat out ivec2 duiVideoSize;out vec2 duiVideoPoint;\n");
      String helpers = "#ifndef IS_GUI\n"
          + "#moj_import <dui:video-vertex.glsl>\n#endif\n";
      source = source.replace("void main() {", helpers + "void main() {\n"
          + "duiVideoFormat=-1;duiVideoBackground=0;duiVideoSize=ivec2(0);duiVideoPoint=vec2(0);\n"
          + "#ifndef IS_GUI\nif(duiVideoVertex())return;\n#endif\n");
    } else {
      source = source.replace("out vec4 fragColor;", "out vec4 fragColor;\n"
          + "flat in int duiVideoFormat;flat in int duiVideoBackground;"
          + "flat in ivec2 duiVideoSize;in vec2 duiVideoPoint;\n"
          + "#moj_import <dui:video-fragment.glsl>\n");
      source = source.replace("void main() {", "void main() {\n"
          + "if(duiVideoFormat>=0){fragColor=duiVideoPixel();return;}\n");
    }
    return source.getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] baseShader(String ext, byte[] bytes) throws IOException {
    if (ext.equals("vsh")) {
      String source = new String(bytes, StandardCharsets.UTF_8);
      String imports =
          "#moj_import <minecraft:globals.glsl>\n"
              + "#moj_import <dui:world-map-protocol.glsl>\n"
              + "#if defined(IS_GUI)\n"
              + "#moj_import <dui:world-map-hud.glsl>\n"
              + "#else\n"
              + "uniform sampler2D Sampler0;\n"
              + "#moj_import <dui:world-map-geometry.glsl>\n"
              + "#moj_import <dui:world-map-scene.glsl>\n"
              + "#endif\n";
      String entry =
          "void main() {\n"
              + "#ifdef IS_GUI\n"
              + "focusGuard=0;guardSize=vec2(0);guardPoint=vec2(0);playerFlags=-1;playerPoint=vec2(0);playerSize=vec2(0);\n"
              + "if(duiWorldHud()) return;\n"
              + "#else\n"
              + "if(duiWorldScene()) return;\n"
              + "#endif";
      if (!source.contains("void main() {"))
        throw new IOException("Text shader entrypoint changed");
      return source
          .replace("void main() {", imports + entry)
          .replace("// PLAYER_RENDERERS", "#moj_import <dui:player-renderers.glsl>")
          .getBytes(StandardCharsets.UTF_8);
    }
    if (!ext.equals("fsh")) return bytes;
    try (var in = FocusGuard.class.getResourceAsStream("/ui/shader/player-model.glsl")) {
      return new String(bytes, StandardCharsets.UTF_8)
          .replace(
              "// PLAYER_FUNCTIONS",
              new String(
                  java.util.Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8))
          .replace("// PLAYER_RENDERERS", "#moj_import <dui:player-renderers.glsl>")
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
