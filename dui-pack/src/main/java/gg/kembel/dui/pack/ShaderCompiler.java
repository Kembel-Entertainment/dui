package gg.kembel.dui.pack;

import gg.kembel.dui.core.*;
import java.util.*;

/** Generates typed GLSL ABI wrappers from the same specifications used by Java. */
final class ShaderCompiler {
  private ShaderCompiler() {}

  static String compile(
      List<PackContribution.Shader> shaders,
      Map<String, ShaderBinding> bindings,
      Map<String, String> modules) {
    var out = new StringBuilder();
    var cases = new StringBuilder();
    var sizes = new StringBuilder();
    new TreeMap<>(modules)
        .forEach(
            (id, source) ->
                out.append("// module ").append(id).append("\n").append(source).append("\n"));
    for (var shader :
        shaders.stream().sorted(Comparator.comparing(s -> s.specification().id())).toList()) {
      var spec = shader.specification();
      int code = ShaderRegistry.require(bindings, spec).code();
      String type = "DuiParams_" + code;
      out.append("struct ").append(type).append("{\n");
      if (spec.parameters().isEmpty()) out.append("int unused;\n");
      for (var p : spec.parameters())
        out.append(
                switch (p.kind()) {
                  case BOOLEAN -> "bool";
                  case RGB -> "vec3";
                  case DECIMAL -> "float";
                  default -> "int";
                })
            .append(' ')
            .append(p.name())
            .append(";\n");
      out.append("};\n");
      out.append(shader.glsl().replace("DUI_PARAMETERS", type)).append('\n');
      cases.append("if(code==").append(code).append("){").append(type).append(" p;\n");
      if (spec.parameters().isEmpty()) cases.append("p.unused=0;\n");
      int at = 0;
      for (var p : spec.parameters()) {
        String read = "duiShaderRead(base+" + at + "," + p.bits() + ")";
        cases.append("p.").append(p.name()).append('=');
        switch (p.kind()) {
          case BOOLEAN -> cases.append(read).append("!=0u");
          case RGB ->
              cases
                  .append("vec3(float((")
                  .append(read)
                  .append(">>16u)&255u),float((")
                  .append(read)
                  .append(">>8u)&255u),float(")
                  .append(read)
                  .append("&255u))/255.0");
          case DECIMAL ->
              cases.append(
                  Double.toString(p.minimum())
                      + "+float("
                      + read
                      + ")*"
                      + Double.toString(p.step()));
          default -> cases.append((int) p.minimum()).append("+int(").append(read).append(')');
        }
        cases.append(";\n");
        at += p.bits();
      }
      cases.append("return ").append(shader.function()).append("(q,size,p,t,live);}\n");
      sizes.append("if(code==").append(code).append(")return ").append(spec.bits()).append(";\n");
    }
    return out
        + "int duiShaderSize(int code){"
        + sizes
        + "return -1;}\nvec4 duiShaderPixel(vec2 q,vec2 size,int code,int base,float t,bool live){"
        + cases
        + "return vec4(0);}\n";
  }
}
