package gg.kembel.dui.pack;

import gg.kembel.dui.core.*;
import java.util.*;

/** GLSL lookup tables generated from the exact registered consumer model specifications. */
final class PlayerRendererPack {
  private PlayerRendererPack() {}

  static String glsl(Map<String, PlayerRenderBinding> bindings) {
    var out =
        new StringBuilder("bool duiPlayerViewport(int renderer,int viewport,out vec2 size){\n");
    for (var binding : new TreeMap<>(bindings).values())
      for (int i = 0; i < binding.specification().viewports().size(); i++) {
        var v = binding.specification().viewports().get(i);
        out.append("if(renderer==")
            .append(binding.code())
            .append("&&viewport==")
            .append(i)
            .append("){size=vec2(")
            .append(v.width())
            .append(',')
            .append(v.height())
            .append(");return true;}\n");
      }
    out.append(
        "size=vec2(0);return false;}\n"
            + "bool duiPlayerPose(int renderer,int pose,out vec4 camera,out vec2 idle,out float"
            + " limbs[6]){\n");
    for (var binding : new TreeMap<>(bindings).values())
      for (int i = 0; i < binding.specification().poses().size(); i++) {
        var p = binding.specification().poses().get(i);
        out.append(
            String.format(
                Locale.ROOT,
                "if(renderer==%d&&pose==%d){camera=vec4(%.9f,%.9f,%.9f,0.0);idle=vec2(%.9f,%.9f);",
                binding.code(),
                i,
                Math.toRadians(p.yawDegrees()),
                Math.toRadians(p.pitchDegrees()),
                p.worldHeight(),
                Math.toRadians(p.idleDegrees()),
                p.idleSpeed()));
        for (int limb = 0; limb < 6; limb++)
          out.append(
              String.format(
                  Locale.ROOT, "limbs[%d]=%.9f;", limb, Math.toRadians(p.limbPitch().get(limb))));
        out.append("return true;}\n");
      }
    return out.append(
            "camera=vec4(0);idle=vec2(0);for(int i=0;i<6;i++)limbs[i]=0.0;return false;}\n")
        .toString();
  }
}
