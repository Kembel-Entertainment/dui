package gg.kembel.dui.pack;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import gg.kembel.dui.core.FocusMarker;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class FocusGuardTest {
  @Test
  void everySupportedSizeHasAUniqueStampWithCallerOwnedColor() throws Exception {
    var bytes = new ByteArrayOutputStream();
    try (var zip = new ZipOutputStream(bytes)) {
      FocusGuard.write(zip);
    }
    var files = new HashMap<String, byte[]>();
    try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      for (var e = zip.getNextEntry(); e != null; e = zip.getNextEntry())
        files.put(e.getName(), zip.readAllBytes());
    }
    var sheet =
        ImageIO.read(
            new ByteArrayInputStream(files.get("assets/dui/textures/gui/canvas/focus_guard.png")));
    var provider =
        JsonParser.parseString(
                new String(files.get("assets/dui/font/focus_guard.json"), StandardCharsets.UTF_8))
            .getAsJsonObject()
            .getAsJsonArray("providers")
            .get(0)
            .getAsJsonObject();
    assertEquals(8, provider.get("height").getAsInt());
    var rows = provider.getAsJsonArray("chars");
    int count = 0;
    for (int r = 0; r < rows.size(); r++) {
      int[] cps = rows.get(r).getAsString().codePoints().toArray();
      assertEquals(128, cps.length);
      for (int c = 0; c < cps.length; c++) {
        if (cps[c] == 0) continue;
        int index = cps[c] - FocusMarker.BASE;
        assertEquals(count++, index);
        for (int y = 0; y < 2; y++)
          for (int x = 0; x < 2; x++) {
            int pixel = sheet.getRGB(c * 4 + x * 3, r * 4 + y * 3);
            assertEquals(248, pixel >>> 24);
            int red = pixel >> 16 & 255, green = pixel >> 8 & 255;
            assertEquals(120 + index / FocusMarker.ROWS, red + ((green & 1) << 8));
            assertEquals(index % FocusMarker.ROWS + 1, green >> 1);
            assertEquals(224 + x + 2 * y, pixel & 255);
          }
      }
    }
    assertEquals(361 * 40, count);
    assertEquals(9, FocusMarker.ADVANCE);
    String fragment =
        new String(files.get("assets/minecraft/shaders/core/text.fsh"), StandardCharsets.UTF_8);
    assertTrue(fragment.contains("vec4(vertexColor.rgb,1.0)"));
  }
}
