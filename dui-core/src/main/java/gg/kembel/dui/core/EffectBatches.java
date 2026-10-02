package gg.kembel.dui.core;

import java.util.*;

/** Order-preserving packing by actual parameter and motion cost. */
public final class EffectBatches {
  private EffectBatches() {}

  public static int bits(Canvas canvas, ShaderInvocation effect) {
    return RendererProtocol.SHADER_DEFINITION_BITS
        + 4 * 9
        + effect.shader().bits()
        + (canvas.effectMotions.containsKey(effect.id()) ? RendererProtocol.SHADER_MOTION_BITS : 0);
  }

  public static List<List<ShaderInvocation>> of(Canvas canvas) {
    var out = new ArrayList<List<ShaderInvocation>>();
    int at = 0;
    while (at < canvas.effects.size()) {
      boolean tracked = canvas.effectMotions.containsKey(canvas.effects.get(at).id());
      var batch = new ArrayList<ShaderInvocation>();
      int used = 0;
      while (at < canvas.effects.size() && batch.size() < RendererProtocol.CARRIER_EFFECTS) {
        var effect = canvas.effects.get(at);
        int cost = bits(canvas, effect);
        if (canvas.effectMotions.containsKey(effect.id()) != tracked
            || used + cost > RendererProtocol.SHADER_CARRIER_BITS) break;
        batch.add(effect);
        used += cost;
        at++;
      }
      if (batch.isEmpty())
        throw new IllegalArgumentException("Shader instance exceeds carrier budget");
      out.add(List.copyOf(batch));
    }
    return List.copyOf(out);
  }
}
