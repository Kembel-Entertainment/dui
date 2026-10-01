package gg.kembel.dui.core;

import java.util.*;

/** Stable draw-order batching, without charging untracked effects for motion payloads. */
public final class EffectBatches {
  private EffectBatches() {}

  public static List<List<ShaderEffect>> of(Canvas canvas) {
    var batches = new ArrayList<List<ShaderEffect>>();
    int at = 0;
    while (at < canvas.effects.size()) {
      boolean tracked = canvas.effectMotions.containsKey(canvas.effects.get(at).id());
      int limit = tracked ? 2 : RendererProtocol.CARRIER_EFFECTS;
      var batch = new ArrayList<ShaderEffect>();
      do {
        batch.add(canvas.effects.get(at++));
      } while (at < canvas.effects.size()
          && batch.size() < limit
          && canvas.effectMotions.containsKey(canvas.effects.get(at).id()) == tracked);
      batches.add(List.copyOf(batch));
    }
    return List.copyOf(batches);
  }
}
