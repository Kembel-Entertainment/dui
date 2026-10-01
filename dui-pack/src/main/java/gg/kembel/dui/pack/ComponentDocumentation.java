package gg.kembel.dui.pack;

import com.google.gson.*;
import gg.kembel.dui.core.*;
import java.nio.file.*;
import java.util.*;

public final class ComponentDocumentation {
  private ComponentDocumentation() {}

  public static void main(String[] args) throws Exception {
    Path out = Path.of(args[0]);
    Files.createDirectories(out);
    var schemas = new TreeMap<>(ComponentSchemas.all());
    var documented = new TreeMap<String, Object>();
    for (var schema : schemas.values()) {
      var fields = new TreeMap<String, Object>();
      fields.put("name", schema.name());
      fields.put("attributes", new TreeSet<>(schema.attributes()));
      fields.put("defaults", new TreeMap<>(schema.defaults()));
      fields.put("constraints", schema.constraints());
      fields.put("cost", schema.cost());
      fields.put("backend", schema.backend());
      documented.put(schema.name(), fields);
    }
    Files.writeString(
        out.resolve("components.json"),
        new GsonBuilder().setPrettyPrinting().create().toJson(documented) + "\n");
    var text =
        new StringBuilder(
            "# Generated component contract\n\n"
                + "Generated from ComponentSchemas, which also validates templates. Placement and"
                + " style properties appear on each schema. Bindings resolve at render time. Costs"
                + " are counts, not network/FPS measurements.\n\n");
    for (var schema : schemas.values())
      text.append("## dui-")
          .append(schema.name())
          .append("\n\n")
          .append(schema.constraints())
          .append("\n\nBackend: ")
          .append(schema.backend())
          .append(". Cost: ")
          .append(schema.cost())
          .append(".\n\nSupported properties: `")
          .append(String.join("`, `", new TreeSet<>(schema.attributes())))
          .append("`.\n\nDefaults: ")
          .append(new TreeMap<>(schema.defaults()))
          .append(".\n\n");
    Files.writeString(out.resolve("component-contract.md"), text.toString());
  }
}
