package gg.kembel.dui.core.world;

import java.util.UUID;

/** Rejects clicks for previous views and duplicate main/offhand actions in the same tick. */
public final class WorldMapClickGate {
  private UUID entity;
  private long acceptedTick = Long.MIN_VALUE;

  public void replace(UUID next) {
    entity = next;
  }

  public boolean accept(UUID clicked, long tick) {
    if (!clicked.equals(entity) || tick == acceptedTick) return false;
    acceptedTick = tick;
    return true;
  }
}
