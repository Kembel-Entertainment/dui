package gg.kembel.dui.pack;

import static org.lwjgl.util.shaderc.Shaderc.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

/** Compile the complete generated pack against the exact pinned vanilla include files. */
public final class VerifyClientShaders {
  private static final Pattern INCLUDE = Pattern.compile("(?m)^\\s*#include\\s*<([^>]+)>[^\\n]*$");
  private final Map<String, String> assets = new HashMap<>();
  private void read(Path path) throws Exception {
    try (var zip = new ZipFile(path.toFile())) {
      for (var entry : Collections.list(zip.entries())) if (entry.getName().contains("/shaders/") && !entry.isDirectory())
        assets.put(entry.getName(), new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8));
    }
  }
  private String expand(String path, Set<String> stack) {
    if (!stack.add(path)) throw new IllegalArgumentException("Recursive shader include: " + path);
    var source = Objects.requireNonNull(assets.get(path), "Missing shader asset: " + path);
    var matcher = INCLUDE.matcher(source); var result = new StringBuilder();
    while (matcher.find()) {
      var key = matcher.group(1).split(":", 2);
      matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(expand("assets/" + key[0] + "/shaders/include/" + key[1], stack)));
    }
    matcher.appendTail(result); stack.remove(path); return result.toString();
  }
  public static void main(String[] args) throws Exception {
    if (args.length != 2) throw new IllegalArgumentException("client.jar pack.zip");
    try (var verified = new VanillaAssets(Path.of(args[0]))) { }
    var check = new VerifyClientShaders(); check.read(Path.of(args[0])); check.read(Path.of(args[1]));
    long compiler = shaderc_compiler_initialize(); long options = shaderc_compile_options_initialize();
    shaderc_compile_options_set_target_env(options, shaderc_target_env_opengl, shaderc_env_version_opengl_4_5);
    shaderc_compile_options_set_auto_bind_uniforms(options, true); shaderc_compile_options_set_auto_map_locations(options, true);
    int count = 0;
    try {
      var variants = List.of("IS_GUI", "IS_GUI,IS_GRAYSCALE", "IS_SEE_THROUGH", "IS_SEE_THROUGH,IS_GRAYSCALE", "", "IS_GRAYSCALE",
          "OIT,OIT_ACCUMULATE", "OIT,OIT_ALPHA_ONLY,OIT_DEPTH_BOUNDS", "OIT,OIT_ALPHA_ONLY,OIT_TRANSMITTANCE");
      for (var program : List.of("text", "position_tex_color")) for (String variant : variants) for (String ext : List.of("vsh", "fsh")) {
        String path = "assets/minecraft/shaders/core/" + program + "." + ext;
        var source = check.expand(path, new HashSet<>());
        String defines = (variant.isBlank() ? "" : String.join("\n", Arrays.stream(variant.split(",")).map(s -> "#define " + s).toList()) + "\n")
            + (variant.contains("OIT") ? "#define OIT_COEFF_COUNT 8\n#define OIT_WAVELET_RANK 2\n#define OIT_COEFF_ATTACHMENT_COUNT 2\n" : "");
        source = source.replaceFirst("(#version[^\\n]*\\n)", "$1" + defines);
        long result = shaderc_compile_into_spv(compiler, source, ext.equals("vsh") ? shaderc_glsl_vertex_shader : shaderc_glsl_fragment_shader, path, "main", options);
        try {
          if (shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success)
            throw new IllegalStateException(program + "." + ext + " [" + variant + "]:\n" + shaderc_result_get_error_message(result));
          count++;
        } finally { shaderc_result_release(result); }
      }
    } finally { shaderc_compile_options_release(options); shaderc_compiler_release(compiler); }
    System.out.println("PASS " + count + " shader stages against Minecraft 26.3");
  }
}
